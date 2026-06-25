package com.tossai.trading.domain.portfolio;

import com.tossai.trading.domain.order.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

/** 신호의 포트폴리오 영향 분석 결과. 주문 후보의 기반이 된다. */
public record PortfolioDecision(
        String portfolioDecisionId,
        String correlationId,
        String strategySignalId,
        String symbol,
        OrderSide side,
        long targetQuantity,
        BigDecimal targetPrice,
        BigDecimal expectedPositionPercent,
        BigDecimal expectedSectorPercent,
        BigDecimal expectedCashReservePercent,
        Instant decidedAt
) {
}
