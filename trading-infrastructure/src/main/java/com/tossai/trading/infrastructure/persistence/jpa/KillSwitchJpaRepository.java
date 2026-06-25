package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.KillSwitchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KillSwitchJpaRepository extends JpaRepository<KillSwitchEntity, String> {
    List<KillSwitchEntity> findByEnabledTrue();
}
