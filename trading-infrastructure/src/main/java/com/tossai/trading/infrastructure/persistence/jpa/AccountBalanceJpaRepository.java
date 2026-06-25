package com.tossai.trading.infrastructure.persistence.jpa;

import com.tossai.trading.infrastructure.persistence.entity.AccountBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceJpaRepository extends JpaRepository<AccountBalanceEntity, String> {
}
