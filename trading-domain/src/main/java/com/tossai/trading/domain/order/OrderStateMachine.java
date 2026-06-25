package com.tossai.trading.domain.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 주문 상태 전이 규칙. 허용된 전이만 가능하다(구현 규칙 8).
 * ORDER_LIFECYCLE.md 의 상태 전이 다이어그램과 일치한다.
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.VALIDATING));
        ALLOWED.put(OrderStatus.VALIDATING, EnumSet.of(
                OrderStatus.REJECTED, OrderStatus.APPROVED, OrderStatus.PENDING_APPROVAL));
        ALLOWED.put(OrderStatus.PENDING_APPROVAL, EnumSet.of(
                OrderStatus.APPROVED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.APPROVED, EnumSet.of(OrderStatus.SUBMITTED, OrderStatus.FAILED));
        ALLOWED.put(OrderStatus.SUBMITTED, EnumSet.of(
                OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED,
                OrderStatus.FAILED, OrderStatus.CANCEL_REQUESTED));
        ALLOWED.put(OrderStatus.PARTIALLY_FILLED, EnumSet.of(
                OrderStatus.FILLED, OrderStatus.CANCEL_REQUESTED));
        ALLOWED.put(OrderStatus.CANCEL_REQUESTED, EnumSet.of(
                OrderStatus.CANCELLED, OrderStatus.FILLED));
        // 종료 상태에서의 전이는 없음
        ALLOWED.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.FILLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }

    public static Set<OrderStatus> allowedNext(OrderStatus from) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
    }
}
