package com.tossai.trading.application.service.risk;

import java.math.BigDecimal;

/** Risk Engine 상태 요약(운영 조회용). */
public record RiskStatus(
        boolean globalKillSwitchEnabled,
        int enabledKillSwitchCount,
        BigDecimal todayOrderAmount,
        BigDecimal todayRealizedLoss,
        BigDecimal maxDailyOrderAmount,
        BigDecimal maxDailyLoss,
        boolean autoTradingEnabled,
        boolean approvalRequired,
        boolean dryRun,
        String mode
) {
}
