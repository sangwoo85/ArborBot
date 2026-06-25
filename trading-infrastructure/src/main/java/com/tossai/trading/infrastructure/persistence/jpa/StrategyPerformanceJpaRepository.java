package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.StrategyPerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyPerformanceJpaRepository extends JpaRepository<StrategyPerformanceEntity, String> {
}
