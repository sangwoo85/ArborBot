package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.OrderRepository;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.order.OrderType;
import com.tossai.trading.infrastructure.persistence.entity.OrderEntity;
import com.tossai.trading.infrastructure.persistence.jpa.OrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    public OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        jpa.save(toEntity(order));
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jpa.findById(orderId).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public int countCreatedAfter(Instant since) {
        return jpa.countByCreatedAtAfter(since);
    }

    @Override
    public java.util.List<Order> findByStatus(String status) {
        return jpa.findByStatus(status).stream().map(this::toDomain).toList();
    }

    private OrderEntity toEntity(Order o) {
        OrderEntity e = new OrderEntity();
        e.setOrderId(o.getOrderId());
        e.setCorrelationId(o.getCorrelationId());
        e.setStrategySignalId(o.getStrategySignalId());
        e.setPortfolioDecisionId(o.getPortfolioDecisionId());
        e.setSymbol(o.getSymbol());
        e.setSide(o.getSide().name());
        e.setOrderType(o.getOrderType().name());
        e.setQuantity(o.getQuantity());
        e.setLimitPrice(o.getLimitPrice());
        e.setIdempotencyKey(o.getIdempotencyKey());
        e.setMode(o.getMode());
        e.setDryRun(o.isDryRun());
        e.setStatus(o.getStatus().name());
        e.setFilledQuantity(o.getFilledQuantity());
        e.setRejectReason(o.getRejectReason());
        e.setCreatedAt(o.getCreatedAt());
        e.setUpdatedAt(o.getUpdatedAt());
        return e;
    }

    private Order toDomain(OrderEntity e) {
        Order o = new Order(e.getOrderId(), e.getCorrelationId(), e.getStrategySignalId(),
                e.getPortfolioDecisionId(), e.getSymbol(), OrderSide.valueOf(e.getSide()),
                OrderType.valueOf(e.getOrderType()), e.getQuantity(), e.getLimitPrice(),
                e.getIdempotencyKey(), e.getMode(), e.isDryRun(), e.getCreatedAt());
        o.restoreState(OrderStatus.valueOf(e.getStatus()), e.getFilledQuantity(),
                e.getRejectReason(), e.getUpdatedAt());
        return o;
    }
}
