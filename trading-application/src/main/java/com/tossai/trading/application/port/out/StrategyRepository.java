package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.strategy.TradingStrategy;

import java.util.List;
import java.util.Optional;

public interface StrategyRepository {
    List<TradingStrategy> findAll();

    Optional<TradingStrategy> findById(String strategyId);

    /** 전략 활성/비활성 상태 변경(거버넌스). */
    void setActive(String strategyId, boolean active);
}
