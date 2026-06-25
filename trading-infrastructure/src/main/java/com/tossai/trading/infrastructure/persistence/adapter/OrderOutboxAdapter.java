package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.OrderOutboxPort;
import com.tossai.trading.infrastructure.persistence.entity.OrderOutboxEntity;
import com.tossai.trading.infrastructure.persistence.jpa.OrderOutboxJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class OrderOutboxAdapter implements OrderOutboxPort {

    private final OrderOutboxJpaRepository jpa;

    public OrderOutboxAdapter(OrderOutboxJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void enqueue(String orderId, String idempotencyKey) {
        OrderOutboxEntity e = jpa.findById(orderId).orElseGet(OrderOutboxEntity::new);
        e.setOrderId(orderId);
        e.setIdempotencyKey(idempotencyKey);
        e.setStatus("PENDING");
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(Instant.now());
        }
        e.setUpdatedAt(Instant.now());
        jpa.save(e);
    }

    @Override
    public void markProcessed(String orderId) {
        jpa.findById(orderId).ifPresent(e -> {
            e.setStatus("PROCESSED");
            e.setUpdatedAt(Instant.now());
            jpa.save(e);
        });
    }

    @Override
    public void markFailed(String orderId, String reason) {
        jpa.findById(orderId).ifPresent(e -> {
            e.setStatus("FAILED");
            e.setAttempts(e.getAttempts() + 1);
            e.setLastError(reason);
            e.setUpdatedAt(Instant.now());
            jpa.save(e);
        });
    }

    @Override
    public List<String> fetchPendingOrderIds(int limit) {
        return jpa.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, limit))
                .stream().map(OrderOutboxEntity::getOrderId).toList();
    }
}
