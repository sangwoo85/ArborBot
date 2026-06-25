package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.StrategyRepository;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.strategy.TradingStrategy;
import com.tossai.trading.infrastructure.persistence.entity.StrategyEntity;
import com.tossai.trading.infrastructure.persistence.jpa.StrategyJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StrategyRepositoryAdapter implements StrategyRepository {

    private final StrategyJpaRepository jpa;

    public StrategyRepositoryAdapter(StrategyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TradingStrategy> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<TradingStrategy> findById(String strategyId) {
        return jpa.findById(strategyId).map(this::toDomain);
    }

    @Override
    public void setActive(String strategyId, boolean active) {
        jpa.findById(strategyId).ifPresent(e -> {
            e.setActive(active);
            jpa.save(e);
        });
    }

    private TradingStrategy toDomain(StrategyEntity e) {
        return new TradingStrategy(e.getStrategyId(), e.getVersion(), e.getDescription(),
                e.getTargetRegime() == null ? null : MarketRegime.valueOf(e.getTargetRegime()),
                e.isActive(), e.isAutoTradingEligible());
    }
}
