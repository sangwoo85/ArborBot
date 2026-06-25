package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, String> {
}
