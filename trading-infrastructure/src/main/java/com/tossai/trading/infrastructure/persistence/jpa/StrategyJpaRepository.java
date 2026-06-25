package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.StrategyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyJpaRepository extends JpaRepository<StrategyEntity, String> {
}
