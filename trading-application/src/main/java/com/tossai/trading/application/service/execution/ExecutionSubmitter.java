package com.tossai.trading.application.service.execution;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.BrokerPort;
import com.tossai.trading.application.port.out.DailyRiskUsageRepository;
import com.tossai.trading.application.port.out.NotificationPort;
import com.tossai.trading.application.port.out.OrderExecutionRepository;
import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.application.port.out.OrderOutboxPort;
import com.tossai.trading.application.port.out.OrderRepository;
import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.application.service.portfolio.SettlementService;
import com.tossai.trading.common.error.DomainException;
import com.tossai.trading.common.error.ErrorCode;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.broker.BrokerOrderRequest;
import com.tossai.trading.domain.broker.BrokerOrderResponse;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderExecution;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.portfolio.Position;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 승인된 주문을 증권사로 제출하고 체결을 반영한다.
 *
 * <p>안전 원칙:
 * <ul>
 *   <li>타임아웃 등 결과 불명 시 <b>맹목 재전송 금지</b>. 주문 조회(queryOrder)로 접수 여부를 확인한다.</li>
 *   <li>제출 후 상태는 SUBMITTED 로 유지하고, 미확인 시 Outbox Dispatcher 가 조회 기반 재동기화한다.</li>
 *   <li>매도 체결 손실은 일일 실현 손실에 누적되어 손실 한도 차단에 반영된다.</li>
 * </ul>
 */
@Service
public class ExecutionSubmitter {

    private final BrokerPort brokerPort;
    private final OrderRepository orderRepository;
    private final OrderExecutionRepository executionRepository;
    private final DailyRiskUsageRepository usageRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataPort marketDataPort;
    private final SettlementService settlementService;
    private final OrderOutboxPort outbox;
    private final NotificationPort notifications;
    private final AuditLogRepository auditLogRepository;

    public ExecutionSubmitter(BrokerPort brokerPort, OrderRepository orderRepository,
                              OrderExecutionRepository executionRepository,
                              DailyRiskUsageRepository usageRepository,
                              PortfolioRepository portfolioRepository, MarketDataPort marketDataPort,
                              SettlementService settlementService, OrderOutboxPort outbox,
                              NotificationPort notifications, AuditLogRepository auditLogRepository) {
        this.brokerPort = brokerPort;
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.usageRepository = usageRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataPort = marketDataPort;
        this.settlementService = settlementService;
        this.outbox = outbox;
        this.notifications = notifications;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 주문 처리. APPROVED 는 제출(place), SUBMITTED(결과 불명)는 조회 기반 재동기화(reconcile)한다.
     * 멱등: 그 외 상태에서는 아무 것도 하지 않는다.
     */
    public void submit(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "주문 없음: " + orderId));
        if (order.getStatus() == OrderStatus.APPROVED) {
            place(order);
        } else if (order.getStatus() == OrderStatus.SUBMITTED
                || order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            reconcile(order);   // 결과 불명 또는 잔량 — 조회로 추가 체결 반영(재전송 없음)
        }
    }

    private void place(Order order) {
        BrokerOrderRequest req = new BrokerOrderRequest(
                order.getIdempotencyKey(), order.getSymbol(), order.getSide(),
                order.getOrderType(), order.getQuantity(), order.getLimitPrice());

        order.transitionTo(OrderStatus.SUBMITTED);
        orderRepository.save(order);

        BrokerOrderResponse resp;
        try {
            resp = brokerPort.placeOrder(req);
        } catch (Exception e) {
            // 결과 불명: 맹목 재전송 금지. 조회로 접수 여부 확인.
            Optional<BrokerOrderResponse> q = safeQuery(order.getIdempotencyKey());
            if (q.isPresent() && q.get().accepted()) {
                notifications.notify("INFO", "주문 재동기화",
                        "order=" + order.getOrderId() + " 타임아웃이었으나 접수 확인됨", order.getCorrelationId());
                applyAccepted(order, q.get());
                return;
            }
            // 여전히 미확인: SUBMITTED 유지(주문이 살아있을 수 있음). Outbox 가 조회 재시도.
            outbox.markFailed(order.getOrderId(), "결과 불명: " + e.getClass().getSimpleName());
            notifications.notify("HIGH", "주문 결과 불명",
                    "order=" + order.getOrderId() + " 재동기화 대기(재전송 금지)", order.getCorrelationId());
            audit(order, "SUBMIT_UNKNOWN");
            return;
        }

        if (!resp.accepted()) {
            order.transitionTo(OrderStatus.FAILED);
            orderRepository.save(order);
            outbox.markFailed(order.getOrderId(), "broker 거부: " + resp.resultCode());
            notifications.notify("HIGH", "주문 거부",
                    "order=" + order.getOrderId() + " code=" + resp.resultCode(), order.getCorrelationId());
            audit(order, "BROKER_REJECTED");
            return;
        }
        applyAccepted(order, resp);
    }

    /** SUBMITTED(결과 불명) 주문을 조회만으로 재동기화. 재전송하지 않는다. */
    private void reconcile(Order order) {
        Optional<BrokerOrderResponse> q = safeQuery(order.getIdempotencyKey());
        if (q.isPresent() && q.get().accepted()) {
            applyAccepted(order, q.get());
        }
        // 미확인이면 SUBMITTED 유지. 다음 주기에 재시도(운영자 개입 가능).
    }

    /**
     * 증권사 응답의 <b>누적</b> 체결수량과 이미 반영한 수량의 차이(delta)만 처리한다.
     * 부분 체결이 여러 번 들어와도 중복 반영 없이 잔량만큼만 누적된다.
     */
    private void applyAccepted(Order order, BrokerOrderResponse resp) {
        long cumulativeFilled = resp.filledQuantity();
        long delta = cumulativeFilled - order.getFilledQuantity();
        if (delta <= 0) {
            return;   // 추가 체결 없음 — 상태 유지(잔량 대기)
        }

        order.applyFill(delta);   // PARTIALLY_FILLED 또는 FILLED 로 전이
        orderRepository.save(order);

        BigDecimal fillPrice = nz(resp.avgFillPrice());
        executionRepository.save(new OrderExecution(
                Ids.newId("exec"), order.getOrderId(), order.getCorrelationId(),
                delta, fillPrice, BigDecimal.ZERO, BigDecimal.ZERO, resp.brokerOrderRef(), Instant.now()));
        usageRepository.addOrderAmount(LocalDate.now(), fillPrice.multiply(BigDecimal.valueOf(delta)));

        // 포지션/현금 갱신(이번 delta 만). 매도는 손익 계산을 먼저 한 뒤 포지션을 줄인다.
        if (order.getSide() == OrderSide.SELL) {
            BigDecimal realizedLoss = computeRealizedLoss(order, fillPrice, delta);  // 매도 전 평균가 기준
            usageRepository.addRealizedLoss(LocalDate.now(), realizedLoss);
            if (realizedLoss.signum() > 0) {
                notifications.notify("INFO", "실현 손실",
                        "order=" + order.getOrderId() + " loss=" + realizedLoss, order.getCorrelationId());
            }
            portfolioRepository.applySell(order.getSymbol(), delta, fillPrice);
        } else {
            String sector = marketDataPort.getInstrument(order.getSymbol())
                    .map(i -> i.sector()).orElse("UNKNOWN");
            portfolioRepository.applyBuy(order.getSymbol(), sector, delta, fillPrice);
            // 매수 체결분은 T+2 결제 후에 매도가능해진다(결제 로트 기록).
            settlementService.recordBuy(order.getSymbol(), delta, LocalDate.now());
        }

        if (order.getStatus() == OrderStatus.FILLED) {
            outbox.markProcessed(order.getOrderId());
            notifications.notify("INFO", "체결 완료",
                    "order=" + order.getOrderId() + " filled=" + order.getFilledQuantity(),
                    order.getCorrelationId());
            audit(order, "FILLED");
        } else {
            // 부분 체결: Outbox 는 PENDING 유지 → 디스패처/배치가 잔량을 계속 폴링
            notifications.notify("INFO", "부분 체결",
                    "order=" + order.getOrderId() + " filled=" + order.getFilledQuantity()
                            + "/" + order.getQuantity(), order.getCorrelationId());
            audit(order, "PARTIALLY_FILLED");
        }
    }

    /** 실현 손실(양수=손실). 보유 평균가 대비 체결가가 낮으면 손실. delta 수량 기준. */
    private BigDecimal computeRealizedLoss(Order order, BigDecimal fillPrice, long delta) {
        Position pos = portfolioRepository.getCurrent().findPosition(order.getSymbol());
        if (pos == null || pos.avgPrice() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal pnl = fillPrice.subtract(pos.avgPrice()).multiply(BigDecimal.valueOf(delta));
        return pnl.signum() < 0 ? pnl.negate() : BigDecimal.ZERO;
    }

    public void requestCancel(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            return;
        }
        BrokerOrderResponse resp = brokerPort.cancelOrder("ref-" + orderId, order.getIdempotencyKey());
        if (resp.accepted()) {
            order.transitionTo(OrderStatus.CANCELLED);
            orderRepository.save(order);
            audit(order, "CANCELLED");
        }
    }

    private Optional<BrokerOrderResponse> safeQuery(String idempotencyKey) {
        try {
            return brokerPort.queryOrder(idempotencyKey);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void audit(Order order, String action) {
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), order.getCorrelationId(),
                "ORDER", action, "order=" + order.getOrderId() + ", status=" + order.getStatus(),
                "execution", Instant.now()));
    }
}
