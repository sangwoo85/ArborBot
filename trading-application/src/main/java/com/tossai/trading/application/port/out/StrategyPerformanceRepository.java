package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.strategy.StrategyPerformance;

import java.util.List;
import java.util.Optional;

public interface StrategyPerformanceRepository {
    List<StrategyPerformance> findAll();

    Optional<StrategyPerformance> findByStrategyId(String strategyId);

    /** 성과 upsert(백테스트 결과 주입 등). */
    StrategyPerformance save(StrategyPerformance performance);
}
