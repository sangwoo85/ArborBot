package com.tossai.trading.application.service.strategyengine;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.NotificationPort;
import com.tossai.trading.application.port.out.StrategyPerformanceRepository;
import com.tossai.trading.application.port.out.StrategyRepository;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import com.tossai.trading.domain.strategy.StrategyThresholds;
import com.tossai.trading.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 전략 엔진. 전략 목록/성과 제공, 활성 전략 선택, 자동주문 적격성 판정.
 * 성과가 기준 이하인 전략은 자동 주문 대상에서 제외한다(구현 규칙 14).
 */
@Service
public class StrategyEngineService {

    private final StrategyRepository strategyRepository;
    private final StrategyPerformanceRepository performanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationPort notifications;
    private final StrategyThresholds thresholds = StrategyThresholds.defaults();

    public StrategyEngineService(StrategyRepository strategyRepository,
                                 StrategyPerformanceRepository performanceRepository,
                                 AuditLogRepository auditLogRepository,
                                 NotificationPort notifications) {
        this.strategyRepository = strategyRepository;
        this.performanceRepository = performanceRepository;
        this.auditLogRepository = auditLogRepository;
        this.notifications = notifications;
    }

    public List<TradingStrategy> listStrategies() {
        return strategyRepository.findAll();
    }

    public List<StrategyPerformance> listPerformance() {
        return performanceRepository.findAll();
    }

    /** 활성 + 자동주문 적격 전략만 반환. */
    public List<TradingStrategy> selectAutoEligibleStrategies() {
        return strategyRepository.findAll().stream()
                .filter(TradingStrategy::active)
                .filter(s -> isAutoEligible(s.strategyId()))
                .toList();
    }

    /**
     * 전략 성과 재평가. 성과가 임계 이하인 활성 전략을 비활성화한다(비활성화 기준 충족 시).
     * batch-service 가 주기적으로 호출한다. 비활성화는 감사 로그 + 알림으로 남긴다.
     *
     * @return 이번에 비활성화된 전략 ID 목록
     */
    public List<String> reevaluateAndDeactivate() {
        List<String> deactivated = new ArrayList<>();
        for (TradingStrategy s : strategyRepository.findAll()) {
            if (!s.active()) {
                continue;
            }
            boolean healthy = performanceRepository.findByStrategyId(s.strategyId())
                    .map(p -> p.meetsAutoTradingThreshold(thresholds))
                    .orElse(false);
            if (!healthy) {
                strategyRepository.setActive(s.strategyId(), false);
                auditLogRepository.save(new AuditLog(Ids.newId("audit"), null,
                        "STRATEGY", "DEACTIVATED",
                        "strategyId=" + s.strategyId() + " 성과 기준 미달", "batch", Instant.now()));
                notifications.notify("HIGH", "전략 비활성화",
                        "strategyId=" + s.strategyId() + " 성과 악화로 자동 비활성화", null);
                deactivated.add(s.strategyId());
            }
        }
        return deactivated;
    }

    /**
     * 전략 등록(거버넌스 DRAFT). 기본 active=true(신호 생성/페이퍼 가능), autoTradingEligible=false
     * (백테스트 승격 전까지 자동 주문 불가).
     */
    public TradingStrategy registerStrategy(String strategyId, String version,
                                            String description, MarketRegime targetRegime) {
        TradingStrategy s = new TradingStrategy(strategyId, version, description, targetRegime, true, false);
        TradingStrategy saved = strategyRepository.save(s);
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "STRATEGY", "REGISTERED",
                "strategyId=" + strategyId + " regime=" + targetRegime, "governance", Instant.now()));
        return saved;
    }

    /**
     * 백테스트 성과 주입 → 임계 충족 시 자동주문 적격으로 승격(미충족 시 강등).
     * research/ 백테스트 결과를 strategy-engine 거버넌스에 연결하는 진입점.
     */
    public PromotionResult submitBacktest(String strategyId, StrategyPerformance performance) {
        performanceRepository.save(performance);
        List<String> failed = failedCriteria(performance);
        boolean qualifies = failed.isEmpty();
        strategyRepository.setAutoEligible(strategyId, qualifies);
        if (qualifies) {
            strategyRepository.setActive(strategyId, true);
        }
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "STRATEGY",
                qualifies ? "BACKTEST_PROMOTED" : "BACKTEST_REJECTED",
                "strategyId=" + strategyId + " failed=" + failed, "governance", Instant.now()));
        notifications.notify(qualifies ? "INFO" : "HIGH",
                qualifies ? "전략 백테스트 승격" : "전략 백테스트 미달",
                "strategyId=" + strategyId + " autoEligible=" + qualifies, null);
        return new PromotionResult(strategyId, qualifies, failed);
    }

    /** 임계 미달 항목 목록(빈 목록이면 승격 적격). */
    private List<String> failedCriteria(StrategyPerformance p) {
        List<String> failed = new ArrayList<>();
        if (p.maxDrawdownPercent() != null
                && p.maxDrawdownPercent().compareTo(thresholds.maxDrawdownLimitPercent()) > 0) {
            failed.add("MAX_DRAWDOWN");
        }
        if (p.sharpeRatio() != null && p.sharpeRatio().compareTo(thresholds.minSharpe()) < 0) {
            failed.add("MIN_SHARPE");
        }
        if (p.tradeCount() < thresholds.minTradeCount()) {
            failed.add("MIN_TRADE_COUNT");
        }
        if (p.consecutiveLosses() >= thresholds.maxConsecutiveLosses()) {
            failed.add("MAX_CONSECUTIVE_LOSSES");
        }
        return failed;
    }

    /** 전략이 자동 주문 대상으로 적격인지(활성 + 성과 임계 충족). */
    public boolean isAutoEligible(String strategyId) {
        Optional<TradingStrategy> strategy = strategyRepository.findById(strategyId);
        if (strategy.isEmpty() || !strategy.get().active() || !strategy.get().autoTradingEligible()) {
            return false;
        }
        return performanceRepository.findByStrategyId(strategyId)
                .map(p -> p.meetsAutoTradingThreshold(thresholds))
                .orElse(false);
    }
}
