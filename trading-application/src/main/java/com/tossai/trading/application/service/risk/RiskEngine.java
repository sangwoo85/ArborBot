package com.tossai.trading.application.service.risk;

import com.tossai.trading.application.config.TradingProperties;
import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.DailyRiskUsageRepository;
import com.tossai.trading.application.port.out.KillSwitchRepository;
import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.application.port.out.OrderRepository;
import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.application.port.out.RateLimiterPort;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.market.Instrument;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.portfolio.Portfolio;
import com.tossai.trading.domain.portfolio.PortfolioDecision;
import com.tossai.trading.domain.portfolio.Position;
import com.tossai.trading.domain.risk.KillSwitch;
import com.tossai.trading.domain.risk.RiskCheckResult;
import com.tossai.trading.domain.risk.RiskViolation;
import com.tossai.trading.domain.signal.TradingSignal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Risk Engine. 주문 가능 여부를 검증하고 ALLOW/REJECT 를 판정한다.
 * 하나라도 위반하면 REJECT. 수익률이 높아도 한도 초과 주문은 막는다.
 */
@Service
public class RiskEngine {

    private final TradingProperties props;
    private final KillSwitchRepository killSwitchRepository;
    private final DailyRiskUsageRepository usageRepository;
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataPort marketDataPort;
    private final RateLimiterPort rateLimiter;
    private final AuditLogRepository auditLogRepository;

    public RiskEngine(TradingProperties props,
                      KillSwitchRepository killSwitchRepository,
                      DailyRiskUsageRepository usageRepository,
                      OrderRepository orderRepository,
                      PortfolioRepository portfolioRepository,
                      MarketDataPort marketDataPort,
                      RateLimiterPort rateLimiter,
                      AuditLogRepository auditLogRepository) {
        this.props = props;
        this.killSwitchRepository = killSwitchRepository;
        this.usageRepository = usageRepository;
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataPort = marketDataPort;
        this.rateLimiter = rateLimiter;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 주문 후보 검증. 위반 목록을 모아 REJECT 여부를 결정한다.
     */
    public RiskCheckResult evaluate(Order order, TradingSignal signal, PortfolioDecision decision) {
        List<RiskViolation> violations = new ArrayList<>();
        TradingProperties.Limits limits = props.getLimits();
        Portfolio portfolio = portfolioRepository.getCurrent();
        Instant now = Instant.now();

        // 0. Kill Switch
        KillSwitch global = killSwitchRepository.getGlobal();
        String killScope = "OFF";
        if (global.enabled()) {
            violations.add(RiskViolation.of("KILL_SWITCH", "전역 Kill Switch 활성"));
            killScope = "GLOBAL";
        }
        killSwitchRepository.find("SYMBOL", order.getSymbol())
                .filter(KillSwitch::enabled)
                .ifPresent(ks -> violations.add(RiskViolation.of("KILL_SWITCH", "종목 Kill Switch 활성")));
        if (signal != null) {
            killSwitchRepository.find("STRATEGY", signal.strategyId())
                    .filter(KillSwitch::enabled)
                    .ifPresent(ks -> violations.add(RiskViolation.of("KILL_SWITCH", "전략 Kill Switch 활성")));
        }

        // 1. 신호 유효성 (만료/신뢰도)
        if (signal != null) {
            if (signal.isExpired(now)) {
                violations.add(RiskViolation.of("SIGNAL_EXPIRED", "신호 유효기간 경과"));
            }
            if (signal.confidenceScore() < limits.getMinConfidenceScore()) {
                violations.add(new RiskViolation("LOW_CONFIDENCE",
                        BigDecimal.valueOf(limits.getMinConfidenceScore()),
                        BigDecimal.valueOf(signal.confidenceScore()), "신뢰도 미달"));
            }
        }

        // 2. 수량 유효성
        if (order.getQuantity() <= 0) {
            violations.add(RiskViolation.of("VALIDATION", "주문 수량이 0 이하"));
        }

        // 2-1. 종목 상태(거래정지/관리/유동성/거래시간) — 신규 진입(매수) 차단
        if (order.getSide() == OrderSide.BUY) {
            Instrument inst = marketDataPort.getInstrument(order.getSymbol()).orElse(null);
            if (inst != null) {
                if (inst.halted()) {
                    violations.add(RiskViolation.of("HALTED_SYMBOL", "거래정지/관리종목"));
                }
                if (inst.illiquid()) {
                    violations.add(RiskViolation.of("ILLIQUID_OR_VOLATILE", "유동성 부족/급등락"));
                }
                if (!inst.tradable() && !inst.halted()) {
                    violations.add(RiskViolation.of("MARKET_CLOSED", "거래 가능 시간/상태 아님"));
                }
            }
        }

        BigDecimal orderAmount = orderAmount(order);

        // 3. 1회 최대 주문 금액
        if (orderAmount.compareTo(limits.getMaxOrderAmount()) > 0) {
            violations.add(new RiskViolation("MAX_ORDER_AMOUNT",
                    limits.getMaxOrderAmount(), orderAmount, "1회 최대 주문 금액 초과"));
        }

        // 4. 일일 최대 주문 금액
        LocalDate today = LocalDate.now();
        BigDecimal todayAmount = usageRepository.todayOrderAmount(today);
        if (todayAmount.add(orderAmount).compareTo(limits.getMaxDailyOrderAmount()) > 0) {
            violations.add(new RiskViolation("MAX_DAILY_ORDER_AMOUNT",
                    limits.getMaxDailyOrderAmount(), todayAmount.add(orderAmount), "일일 최대 주문 금액 초과"));
        }

        // 5. 일일 손실 한도 (도달 시 신규 매수 차단)
        BigDecimal todayLoss = usageRepository.todayRealizedLoss(today);
        if (order.getSide() == OrderSide.BUY
                && todayLoss.compareTo(limits.getMaxDailyLoss()) >= 0) {
            violations.add(new RiskViolation("MAX_DAILY_LOSS",
                    limits.getMaxDailyLoss(), todayLoss, "일일 손실 한도 도달 — 신규 매수 차단"));
        }

        // 6. 잔고/주문가능금액 (매수)
        if (order.getSide() == OrderSide.BUY
                && orderAmount.compareTo(portfolio.orderableAmount()) > 0) {
            violations.add(new RiskViolation("INSUFFICIENT_BALANCE",
                    portfolio.orderableAmount(), orderAmount, "주문가능금액 부족"));
        }

        // 7. 보유/매도가능 수량 (매도)
        if (order.getSide() == OrderSide.SELL) {
            Position pos = portfolio.findPosition(order.getSymbol());
            long sellable = pos == null ? 0 : pos.sellableQuantity();
            if (order.getQuantity() > sellable) {
                violations.add(new RiskViolation("INSUFFICIENT_POSITION",
                        BigDecimal.valueOf(sellable), BigDecimal.valueOf(order.getQuantity()),
                        "매도가능 수량 부족"));
            }
        }

        // 8. 종목별 최대 비중 (사후 예상, 매수)
        if (order.getSide() == OrderSide.BUY && decision != null
                && decision.expectedPositionPercent().compareTo(limits.getMaxPositionPercent()) > 0) {
            violations.add(new RiskViolation("MAX_POSITION_PCT",
                    limits.getMaxPositionPercent(), decision.expectedPositionPercent(), "종목 비중 한도 초과"));
        }

        // 9. 섹터별 최대 비중
        if (order.getSide() == OrderSide.BUY && decision != null
                && decision.expectedSectorPercent().compareTo(limits.getMaxSectorPercent()) > 0) {
            violations.add(new RiskViolation("MAX_SECTOR_PCT",
                    limits.getMaxSectorPercent(), decision.expectedSectorPercent(), "섹터 비중 한도 초과"));
        }

        // 10. 현금 최소 보유 비율
        if (order.getSide() == OrderSide.BUY && decision != null
                && decision.expectedCashReservePercent().compareTo(limits.getMinCashReservePercent()) < 0) {
            violations.add(new RiskViolation("MIN_CASH_RESERVE",
                    limits.getMinCashReservePercent(), decision.expectedCashReservePercent(),
                    "현금 최소 보유 비율 위반"));
        }

        // 11. 중복 주문
        orderRepository.findByIdempotencyKey(order.getIdempotencyKey())
                .filter(o -> !o.getOrderId().equals(order.getOrderId()))
                .ifPresent(o -> violations.add(RiskViolation.of("DUPLICATE_ORDER", "중복 주문")));

        // 12. 주문 빈도 제한 (RateLimiterPort: 기본 인메모리, Redis 시 다중 인스턴스 원자적)
        if (!rateLimiter.tryAcquire("orders:rate", limits.getMaxOrdersPerMinute(), 60)) {
            violations.add(new RiskViolation("RATE_LIMIT",
                    BigDecimal.valueOf(limits.getMaxOrdersPerMinute()), null, "주문 빈도 제한 초과"));
        }

        RiskCheckResult result = violations.isEmpty()
                ? new RiskCheckResult(com.tossai.trading.domain.risk.RiskDecision.ALLOW, List.of(), killScope, now)
                : new RiskCheckResult(com.tossai.trading.domain.risk.RiskDecision.REJECT, violations, killScope, now);

        auditLogRepository.save(new AuditLog(Ids.newId("audit"), order.getCorrelationId(),
                "RISK", result.decision().name(),
                "order=" + order.getOrderId() + ", violations=" + violations.size(),
                "risk-engine", now));
        return result;
    }

    public BigDecimal orderAmount(Order order) {
        BigDecimal price = order.getLimitPrice() == null ? BigDecimal.ZERO : order.getLimitPrice();
        return price.multiply(BigDecimal.valueOf(order.getQuantity()));
    }

    // ---- Kill Switch 운영 ----

    public void enableGlobalKillSwitch(String reason, String actor) {
        killSwitchRepository.save(new KillSwitch("GLOBAL", null, true, reason, actor, Instant.now()));
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "KILL_SWITCH", "ENABLE",
                "reason=" + reason, actor, Instant.now()));
    }

    public void disableGlobalKillSwitch(String reason, String actor) {
        killSwitchRepository.save(new KillSwitch("GLOBAL", null, false, reason, actor, Instant.now()));
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "KILL_SWITCH", "DISABLE",
                "reason=" + reason, actor, Instant.now()));
    }

    public RiskStatus status() {
        KillSwitch global = killSwitchRepository.getGlobal();
        LocalDate today = LocalDate.now();
        return new RiskStatus(
                global.enabled(),
                killSwitchRepository.findEnabled().size(),
                usageRepository.todayOrderAmount(today),
                usageRepository.todayRealizedLoss(today),
                props.getLimits().getMaxDailyOrderAmount(),
                props.getLimits().getMaxDailyLoss(),
                props.isAutoTradingEnabled(),
                props.isApprovalRequired(),
                props.isDryRun(),
                props.getMode()
        );
    }
}
