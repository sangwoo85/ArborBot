package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.OrderExecutionRepository;
import com.tossai.trading.domain.order.OrderExecution;
import com.tossai.trading.infrastructure.persistence.entity.OrderExecutionEntity;
import com.tossai.trading.infrastructure.persistence.jpa.OrderExecutionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderExecutionRepositoryAdapter implements OrderExecutionRepository {

    private final OrderExecutionJpaRepository jpa;

    public OrderExecutionRepositoryAdapter(OrderExecutionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OrderExecution save(OrderExecution x) {
        OrderExecutionEntity e = new OrderExecutionEntity();
        e.setExecutionId(x.executionId());
        e.setOrderId(x.orderId());
        e.setCorrelationId(x.correlationId());
        e.setFilledQuantity(x.filledQuantity());
        e.setAvgFillPrice(x.avgFillPrice());
        e.setFee(x.fee());
        e.setTax(x.tax());
        e.setBrokerOrderRef(x.brokerOrderRef());
        e.setExecutedAt(x.executedAt());
        jpa.save(e);
        return x;
    }

    @Override
    public List<OrderExecution> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).stream().map(e -> new OrderExecution(
                e.getExecutionId(), e.getOrderId(), e.getCorrelationId(), e.getFilledQuantity(),
                e.getAvgFillPrice(), e.getFee(), e.getTax(), e.getBrokerOrderRef(), e.getExecutedAt()
        )).toList();
    }
}
