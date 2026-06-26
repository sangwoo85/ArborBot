package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.StrategyPerformanceRepository;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import com.tossai.trading.infrastructure.persistence.entity.StrategyPerformanceEntity;
import com.tossai.trading.infrastructure.persistence.jpa.StrategyPerformanceJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StrategyPerformanceRepositoryAdapter implements StrategyPerformanceRepository {

    private final StrategyPerformanceJpaRepository jpa;

    public StrategyPerformanceRepositoryAdapter(StrategyPerformanceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<StrategyPerformance> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StrategyPerformance> findByStrategyId(String strategyId) {
        return jpa.findById(strategyId).map(this::toDomain);
    }

    @Override
    public StrategyPerformance save(StrategyPerformance p) {
        StrategyPerformanceEntity e = jpa.findById(p.strategyId()).orElseGet(StrategyPerformanceEntity::new);
        e.setStrategyId(p.strategyId());
        e.setCumulativeReturnPercent(p.cumulativeReturnPercent());
        e.setMaxDrawdownPercent(p.maxDrawdownPercent());
        e.setWinRatePercent(p.winRatePercent());
        e.setProfitFactor(p.profitFactor());
        e.setSharpeRatio(p.sharpeRatio());
        e.setTradeCount(p.tradeCount());
        e.setNetReturnAfterCostPercent(p.netReturnAfterCostPercent());
        e.setConsecutiveLosses(p.consecutiveLosses());
        jpa.save(e);
        return p;
    }

    private StrategyPerformance toDomain(StrategyPerformanceEntity e) {
        return new StrategyPerformance(e.getStrategyId(), e.getCumulativeReturnPercent(),
                e.getMaxDrawdownPercent(), e.getWinRatePercent(), e.getProfitFactor(),
                e.getSharpeRatio(), e.getTradeCount(), e.getNetReturnAfterCostPercent(),
                e.getConsecutiveLosses());
    }
}
