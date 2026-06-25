package com.tossai.trading.domain.order;

/** 주문 상태. 허용된 전이만 가능(OrderStatus#canTransitionTo). */
public enum OrderStatus {
    CREATED,
    VALIDATING,
    REJECTED,
    APPROVED,
    PENDING_APPROVAL,
    SUBMITTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCEL_REQUESTED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == REJECTED || this == FILLED || this == CANCELLED || this == FAILED;
    }
}
