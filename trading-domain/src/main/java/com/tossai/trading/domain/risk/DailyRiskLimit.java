package com.tossai.trading.domain.risk;

import java.math.BigDecimal;

/** 일일/단건 위험 한도 정의(RISK_POLICY 와 일치). */
public record DailyRiskLimit(
        BigDecimal maxOrderAmount,
        BigDecimal maxDailyOrderAmount,
        BigDecimal maxDailyLoss,
        BigDecimal maxPositionPercent,
        BigDecimal maxSectorPercent,
        BigDecimal minCashReservePercent,
        BigDecimal autoOrderAmountCap,
        int minConfidenceScore,
        int autoConfidenceScore,
        int maxOrdersPerMinute
) {
}
