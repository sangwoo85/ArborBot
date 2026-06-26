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

    @Override
    public TradingStrategy save(TradingStrategy s) {
        StrategyEntity e = jpa.findById(s.strategyId()).orElseGet(StrategyEntity::new);
        e.setStrategyId(s.strategyId());
        e.setVersion(s.version());
        e.setDescription(s.description());
        e.setTargetRegime(s.targetRegime() == null ? null : s.targetRegime().name());
        e.setActive(s.active());
        e.setAutoTradingEligible(s.autoTradingEligible());
        jpa.save(e);
        return s;
    }

    @Override
    public void setAutoEligible(String strategyId, boolean eligible) {
        jpa.findById(strategyId).ifPresent(e -> {
            e.setAutoTradingEligible(eligible);
            jpa.save(e);
        });
    }

    private TradingStrategy toDomain(StrategyEntity e) {
        return new TradingStrategy(e.getStrategyId(), e.getVersion(), e.getDescription(),
                e.getTargetRegime() == null ? null : MarketRegime.valueOf(e.getTargetRegime()),
                e.isActive(), e.isAutoTradingEligible());
    }
}
