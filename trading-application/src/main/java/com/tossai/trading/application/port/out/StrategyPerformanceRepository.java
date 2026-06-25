package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.strategy.StrategyPerformance;

import java.util.List;
import java.util.Optional;

public interface StrategyPerformanceRepository {
    List<StrategyPerformance> findAll();

    Optional<StrategyPerformance> findByStrategyId(String strategyId);
}
