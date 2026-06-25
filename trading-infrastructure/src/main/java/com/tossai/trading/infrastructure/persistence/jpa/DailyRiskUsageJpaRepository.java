package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.DailyRiskUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyRiskUsageJpaRepository extends JpaRepository<DailyRiskUsageEntity, LocalDate> {
}
