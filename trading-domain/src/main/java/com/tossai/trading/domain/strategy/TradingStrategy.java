package com.tossai.trading.domain.strategy;

import com.tossai.trading.domain.market.MarketRegime;

/** 전략 메타데이터. 승인된 전략 풀의 구성원. */
public record TradingStrategy(
        String strategyId,
        String version,
        String description,
        MarketRegime targetRegime,
        boolean active,
        boolean autoTradingEligible
) {
}
