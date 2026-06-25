package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.OrderExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderExecutionJpaRepository extends JpaRepository<OrderExecutionEntity, String> {
    List<OrderExecutionEntity> findByOrderId(String orderId);
}
