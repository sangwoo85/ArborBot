package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.order.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String orderId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    int countCreatedAfter(java.time.Instant since);

    /** 특정 상태의 주문 목록(상태 동기화 배치용). */
    List<Order> findByStatus(String status);
}
