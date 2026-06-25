package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.SettlementLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SettlementLotJpaRepository extends JpaRepository<SettlementLotEntity, String> {
    List<SettlementLotEntity> findBySettledFalseAndSettleDateLessThanEqual(LocalDate asOf);
}
