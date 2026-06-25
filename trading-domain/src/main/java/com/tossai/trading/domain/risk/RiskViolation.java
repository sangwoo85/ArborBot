package com.tossai.trading.domain.risk;

import java.math.BigDecimal;

/** 위험 위반 항목. */
public record RiskViolation(
        String rule,
        BigDecimal limit,
        BigDecimal actual,
        String message
) {
    public static RiskViolation of(String rule, String message) {
        return new RiskViolation(rule, null, null, message);
    }
}
