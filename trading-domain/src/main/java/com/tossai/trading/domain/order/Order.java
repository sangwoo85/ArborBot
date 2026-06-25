package com.tossai.trading.domain.order;

import com.tossai.trading.common.error.DomainException;
import com.tossai.trading.common.error.ErrorCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 실행 가능한 주문 요청. TradingSignal(AI 판단)과 구분된다(구현 규칙 2).
 * 상태 변경은 transitionTo 를 통해서만 가능하며 허용된 전이만 수행된다(구현 규칙 8).
 */
public class Order {

    private final String orderId;
    private final String correlationId;
    private final String strategySignalId;
    private final String portfolioDecisionId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType orderType;
    private final long quantity;
    private final BigDecimal limitPrice;
    private final String idempotencyKey;
    private final String mode;            // PAPER / SEMI_AUTO / AUTO
    private final boolean dryRun;
    private final Instant createdAt;

    private OrderStatus status;
    private long filledQuantity;
    private String rejectReason;
    private Instant updatedAt;

    public Order(String orderId, String correlationId, String strategySignalId,
                 String portfolioDecisionId, String symbol, OrderSide side, OrderType orderType,
                 long quantity, BigDecimal limitPrice, String idempotencyKey, String mode,
                 boolean dryRun, Instant createdAt) {
        this.orderId = orderId;
        this.correlationId = correlationId;
        this.strategySignalId = strategySignalId;
        this.portfolioDecisionId = portfolioDecisionId;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.idempotencyKey = idempotencyKey;
        this.mode = mode;
        this.dryRun = dryRun;
        this.createdAt = createdAt;
        this.status = OrderStatus.CREATED;
        this.filledQuantity = 0;
        this.updatedAt = createdAt;
    }

    /** 허용된 전이만 수행. 위반 시 예외. */
    public void transitionTo(OrderStatus next) {
        if (!OrderStateMachine.canTransition(this.status, next)) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                    "허용되지 않은 주문 상태 전이: " + status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status != OrderStatus.VALIDATING) {
            // 검증 단계에서만 거절 가능. CREATED 인 경우 우선 VALIDATING 으로 이동.
            transitionTo(OrderStatus.VALIDATING);
        }
        this.rejectReason = reason;
        transitionTo(OrderStatus.REJECTED);
    }

    public void applyFill(long additionalFilled) {
        this.filledQuantity = Math.min(this.quantity, this.filledQuantity + additionalFilled);
        if (this.filledQuantity >= this.quantity) {
            transitionTo(OrderStatus.FILLED);
        } else {
            transitionTo(OrderStatus.PARTIALLY_FILLED);
        }
    }

    public String getOrderId() { return orderId; }
    public String getCorrelationId() { return correlationId; }
    public String getStrategySignalId() { return strategySignalId; }
    public String getPortfolioDecisionId() { return portfolioDecisionId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public long getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getMode() { return mode; }
    public boolean isDryRun() { return dryRun; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public long getFilledQuantity() { return filledQuantity; }
    public String getRejectReason() { return rejectReason; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** 영속 계층에서 상태를 복원할 때 사용(검증 우회 — 재구성 전용). */
    public void restoreState(OrderStatus status, long filledQuantity, String rejectReason, Instant updatedAt) {
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.rejectReason = rejectReason;
        this.updatedAt = updatedAt;
    }
}
