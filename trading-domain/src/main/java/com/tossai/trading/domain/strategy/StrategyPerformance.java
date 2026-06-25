package com.tossai.trading.domain.strategy;

import java.math.BigDecimal;

/**
 * 전략 성과 지표. 거래비용 반영 수익률을 포함한다.
 * 성과가 기준 이하이면 자동 주문 대상에서 제외된다(구현 규칙 14).
 */
public record StrategyPerformance(
        String strategyId,
        BigDecimal cumulativeReturnPercent,
        BigDecimal maxDrawdownPercent,
        BigDecimal winRatePercent,
        BigDecimal profitFactor,
        BigDecimal sharpeRatio,
        int tradeCount,
        BigDecimal netReturnAfterCostPercent,
        int consecutiveLosses
) {

    /** 자동 주문 적격 여부의 성과 기준(기본 임계). */
    public boolean meetsAutoTradingThreshold(StrategyThresholds t) {
        if (maxDrawdownPercent != null && maxDrawdownPercent.compareTo(t.maxDrawdownLimitPercent()) > 0) {
            return false;
        }
        if (consecutiveLosses >= t.maxConsecutiveLosses()) {
            return false;
        }
        if (sharpeRatio != null && sharpeRatio.compareTo(t.minSharpe()) < 0) {
            return false;
        }
        return tradeCount >= t.minTradeCount();
    }
}
