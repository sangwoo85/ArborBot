package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.TradingSignalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradingSignalJpaRepository extends JpaRepository<TradingSignalEntity, String> {
    List<TradingSignalEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
