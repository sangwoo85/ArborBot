package com.tossai.trading.domain.order;

import com.tossai.trading.common.error.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {

    @Test
    void allowsValidTransitions() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.CREATED, OrderStatus.VALIDATING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.VALIDATING, OrderStatus.APPROVED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.APPROVED, OrderStatus.SUBMITTED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.SUBMITTED, OrderStatus.PARTIALLY_FILLED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED));
    }

    @Test
    void rejectsInvalidTransitions() {
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CREATED, OrderStatus.FILLED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.FILLED, OrderStatus.SUBMITTED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.REJECTED, OrderStatus.APPROVED));
        assertTrue(OrderStatus.FILLED.isTerminal());
    }

    @Test
    void orderRejectsIllegalTransition() {
        Order order = sampleOrder();
        // CREATED -> FILLED 직접 전이는 불가
        assertThrows(DomainException.class, () -> order.transitionTo(OrderStatus.FILLED));
    }

    @Test
    void partialThenFullFill() {
        Order order = sampleOrder();
        order.transitionTo(OrderStatus.VALIDATING);
        order.transitionTo(OrderStatus.APPROVED);
        order.transitionTo(OrderStatus.SUBMITTED);
        order.applyFill(4);
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        order.applyFill(6);
        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(10, order.getFilledQuantity());
    }

    private Order sampleOrder() {
        return new Order("order-1", "corr-1", "sig-1", "pf-1", "005930",
                OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("70000"),
                "idem-1", "AUTO", true, Instant.now());
    }
}
