package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.OrderOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, String> {
    List<OrderOutboxEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
