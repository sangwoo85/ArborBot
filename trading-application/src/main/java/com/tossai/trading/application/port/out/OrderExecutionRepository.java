package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.order.OrderExecution;

import java.util.List;

public interface OrderExecutionRepository {
    OrderExecution save(OrderExecution execution);

    List<OrderExecution> findByOrderId(String orderId);
}
