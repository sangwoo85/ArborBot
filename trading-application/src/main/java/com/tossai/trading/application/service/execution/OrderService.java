package com.tossai.trading.application.service.execution;

import com.tossai.trading.application.config.TradingProperties;
import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.NotificationPort;
import com.tossai.trading.application.port.out.OrderOutboxPort;
import com.tossai.trading.application.port.out.OrderRepository;
import com.tossai.trading.application.port.out.TradingSignalRepository;
import com.tossai.trading.application.service.portfolio.PortfolioService;
import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.common.error.DomainException;
import com.tossai.trading.common.error.ErrorCode;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.order.OrderType;
import com.tossai.trading.domain.portfolio.PortfolioDecision;
import com.tossai.trading.domain.risk.RiskCheckResult;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * 주문 유스케이스: 신호로부터 주문 생성 → 검증 → 승인 정책 → (자동) 제출.
 * <p>BUY 신호여도 Risk Engine 이 거절하면 주문은 REJECTED 로 끝나고 제출되지 않는다(구현 규칙 3).
 * SELL 도 보유/매도가능 수량을 Risk Engine 에서 확인한다(구현 규칙 4).
 */
@Service
public class OrderService {

    private final TradingProperties props;
    private final TradingSignalRepository signalRepository;
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final RiskEngine riskEngine;
    private final StrategyEngineService strategyEngine;
    private final OrderOutboxPort outbox;
    private final ExecutionSubmitter submitter;
    private final NotificationPort notifications;
    private final AuditLogRepository auditLogRepository;

    public OrderService(TradingProperties props, TradingSignalRepository signalRepository,
                        OrderRepository orderRepository, PortfolioService portfolioService,
                        RiskEngine riskEngine, StrategyEngineService strategyEngine,
                        OrderOutboxPort outbox, ExecutionSubmitter submitter,
                        NotificationPort notifications, AuditLogRepository auditLogRepository) {
        this.props = props;
        this.signalRepository = signalRepository;
        this.orderRepository = orderRepository;
        this.portfolioService = portfolioService;
        this.riskEngine = riskEngine;
        this.strategyEngine = strategyEngine;
        this.outbox = outbox;
        this.submitter = submitter;
        this.notifications = notifications;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Order createOrderFromSignal(String signalId) {
        TradingSignal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "신호 없음: " + signalId));

        Instant now = Instant.now();
        // 신호 품질 게이트
        if (!signal.isActionable()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "실행 불가 신호(HOLD)");
        }
        if (signal.isExpired(now)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "신호 유효기간 경과");
        }
        if (!signal.isStructurallyValid()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "신호 정합성 위반");
        }

        OrderSide side = signal.signalType() == SignalType.SELL ? OrderSide.SELL : OrderSide.BUY;
        PortfolioDecision decision = portfolioService.analyzeImpact(signal);

        if (decision.targetQuantity() <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "주문 수량 0 (비중/보유 부족)");
        }

        String idemKey = "idem-" + signalId;
        Optional<Order> existing = orderRepository.findByIdempotencyKey(idemKey);
        if (existing.isPresent()) {
            return existing.get(); // 멱등: 동일 신호로 중복 주문 생성 방지
        }

        BigDecimal price = decision.targetPrice();
        Order order = new Order(
                Ids.newId("order"),
                signal.signalId(),
                signal.signalId(),
                decision.portfolioDecisionId(),
                signal.symbol(),
                side,
                OrderType.LIMIT,
                decision.targetQuantity(),
                price,
                idemKey,
                props.getMode(),
                props.isDryRun(),
                now
        );
        orderRepository.save(order);

        // 검증
        order.transitionTo(OrderStatus.VALIDATING);
        RiskCheckResult risk = riskEngine.evaluate(order, signal, decision);
        if (risk.rejected()) {
            order.reject(summarize(risk));
            orderRepository.save(order);
            notifications.notify("HIGH", "주문 거절",
                    "order=" + order.getOrderId() + " " + summarize(risk), order.getCorrelationId());
            return order;
        }

        // 자동 주문 허용 조건(구현 규칙 5) 모두 충족 시에만 자동 제출
        boolean autoAllowed = props.isAutoTradingEnabled()
                && !props.isApprovalRequired()
                && "AUTO".equalsIgnoreCase(props.getMode())
                && !riskEngine.status().globalKillSwitchEnabled()
                && strategyEngine.isAutoEligible(signal.strategyId())
                && signal.confidenceScore() >= props.getLimits().getAutoConfidenceScore()
                && riskEngine.orderAmount(order).compareTo(props.getLimits().getAutoOrderAmountCap()) <= 0
                && !signal.isExpired(Instant.now());

        if (autoAllowed) {
            order.transitionTo(OrderStatus.APPROVED);
            orderRepository.save(order);
            audit(order, "APPROVED_AUTO");
            outbox.enqueue(order.getOrderId(), order.getIdempotencyKey());
            submitter.submit(order.getOrderId());   // 동기 시도, 실패 시 Outbox 가 재시도 보장
            return orderRepository.findById(order.getOrderId()).orElse(order);
        }

        order.transitionTo(OrderStatus.PENDING_APPROVAL);
        orderRepository.save(order);
        audit(order, "PENDING_APPROVAL");
        notifications.notify("INFO", "승인 대기 주문",
                "order=" + order.getOrderId() + " 사람 승인 필요", order.getCorrelationId());
        return order;
    }

    /** 승인 대기 주문을 사람이 승인 → 제출. */
    @Transactional
    public Order approveOrder(String orderId) {
        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                    "승인 가능 상태 아님: " + order.getStatus());
        }
        order.transitionTo(OrderStatus.APPROVED);
        orderRepository.save(order);
        audit(order, "APPROVED_MANUAL");
        outbox.enqueue(order.getOrderId(), order.getIdempotencyKey());
        submitter.submit(order.getOrderId());
        return orderRepository.findById(orderId).orElse(order);
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = load(orderId);
        OrderStatus s = order.getStatus();
        if (s == OrderStatus.PENDING_APPROVAL) {
            order.transitionTo(OrderStatus.CANCELLED);
            orderRepository.save(order);
            audit(order, "CANCELLED_BEFORE_SUBMIT");
            return order;
        }
        if (s == OrderStatus.SUBMITTED || s == OrderStatus.PARTIALLY_FILLED) {
            order.transitionTo(OrderStatus.CANCEL_REQUESTED);
            orderRepository.save(order);
            submitter.requestCancel(orderId);
            return orderRepository.findById(orderId).orElse(order);
        }
        throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION, "취소 불가 상태: " + s);
    }

    public Order getOrder(String orderId) {
        return load(orderId);
    }

    private Order load(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "주문 없음: " + orderId));
    }

    private String summarize(RiskCheckResult risk) {
        StringBuilder sb = new StringBuilder("REJECT: ");
        risk.violations().forEach(v -> sb.append(v.rule()).append(" "));
        return sb.toString().trim();
    }

    private void audit(Order order, String action) {
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), order.getCorrelationId(),
                "ORDER", action, "order=" + order.getOrderId() + ", status=" + order.getStatus(),
                "order-service", Instant.now()));
    }
}
