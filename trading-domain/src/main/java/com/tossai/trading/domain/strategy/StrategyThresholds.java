package com.tossai.trading.domain.strategy;

import java.math.BigDecimal;

/** 전략 비활성화/자동주문 제외 임계값. */
public record StrategyThresholds(
        BigDecimal maxDrawdownLimitPercent,
        int maxConsecutiveLosses,
        BigDecimal minSharpe,
        int minTradeCount
) {
    public static StrategyThresholds defaults() {
        return new StrategyThresholds(new BigDecimal("20"), 5, new BigDecimal("0.5"), 20);
    }
}
