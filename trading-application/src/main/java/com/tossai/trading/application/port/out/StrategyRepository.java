package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.strategy.TradingStrategy;

import java.util.List;
import java.util.Optional;

public interface StrategyRepository {
    List<TradingStrategy> findAll();

    Optional<TradingStrategy> findById(String strategyId);

    /** 전략 활성/비활성 상태 변경(거버넌스). */
    void setActive(String strategyId, boolean active);

    /** 전략 등록/갱신(upsert). */
    TradingStrategy save(TradingStrategy strategy);

    /** 자동 주문 적격 여부 변경(백테스트 승격/강등). */
    void setAutoEligible(String strategyId, boolean eligible);
}
