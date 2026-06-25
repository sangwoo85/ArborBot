package com.tossai.trading.domain.portfolio;

import java.math.BigDecimal;

/** 보유 종목 포지션. */
public record Position(
        String symbol,
        String sector,
        long quantity,
        long sellableQuantity,
        BigDecimal avgPrice,
        BigDecimal lastPrice,
        BigDecimal evaluationAmount,
        BigDecimal unrealizedPnl
) {
}
