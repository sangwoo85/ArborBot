package com.tossai.trading.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 거래/위험 설정. 기본값은 안전한 쪽(approvalRequired=true, dryRun=true)으로 둔다(구현 규칙 6).
 */
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    /** 자동매매 전체 토글. false 면 자동 주문 자체 비활성. */
    private boolean autoTradingEnabled = false;
    /** 사람 승인 필요 여부. 기본 true. */
    private boolean approvalRequired = true;
    /** 모의 실행. 기본 true(실제 증권사 호출 시뮬레이션). */
    private boolean dryRun = true;
    /** 거래 모드: PAPER / SEMI_AUTO / AUTO */
    private String mode = "PAPER";

    private final Limits limits = new Limits();

    public static class Limits {
        private BigDecimal maxOrderAmount = new BigDecimal("1000000");
        private BigDecimal maxDailyOrderAmount = new BigDecimal("5000000");
        private BigDecimal maxDailyLoss = new BigDecimal("300000");
        private BigDecimal maxPositionPercent = new BigDecimal("10");
        private BigDecimal maxSectorPercent = new BigDecimal("30");
        private BigDecimal minCashReservePercent = new BigDecimal("10");
        private BigDecimal autoOrderAmountCap = new BigDecimal("300000");
        private int minConfidenceScore = 60;
        private int autoConfidenceScore = 70;
        private int maxOrdersPerMinute = 5;

        public BigDecimal getMaxOrderAmount() { return maxOrderAmount; }
        public void setMaxOrderAmount(BigDecimal v) { this.maxOrderAmount = v; }
        public BigDecimal getMaxDailyOrderAmount() { return maxDailyOrderAmount; }
        public void setMaxDailyOrderAmount(BigDecimal v) { this.maxDailyOrderAmount = v; }
        public BigDecimal getMaxDailyLoss() { return maxDailyLoss; }
        public void setMaxDailyLoss(BigDecimal v) { this.maxDailyLoss = v; }
        public BigDecimal getMaxPositionPercent() { return maxPositionPercent; }
        public void setMaxPositionPercent(BigDecimal v) { this.maxPositionPercent = v; }
        public BigDecimal getMaxSectorPercent() { return maxSectorPercent; }
        public void setMaxSectorPercent(BigDecimal v) { this.maxSectorPercent = v; }
        public BigDecimal getMinCashReservePercent() { return minCashReservePercent; }
        public void setMinCashReservePercent(BigDecimal v) { this.minCashReservePercent = v; }
        public BigDecimal getAutoOrderAmountCap() { return autoOrderAmountCap; }
        public void setAutoOrderAmountCap(BigDecimal v) { this.autoOrderAmountCap = v; }
        public int getMinConfidenceScore() { return minConfidenceScore; }
        public void setMinConfidenceScore(int v) { this.minConfidenceScore = v; }
        public int getAutoConfidenceScore() { return autoConfidenceScore; }
        public void setAutoConfidenceScore(int v) { this.autoConfidenceScore = v; }
        public int getMaxOrdersPerMinute() { return maxOrdersPerMinute; }
        public void setMaxOrdersPerMinute(int v) { this.maxOrdersPerMinute = v; }
    }

    public boolean isAutoTradingEnabled() { return autoTradingEnabled; }
    public void setAutoTradingEnabled(boolean v) { this.autoTradingEnabled = v; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean v) { this.approvalRequired = v; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean v) { this.dryRun = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { this.mode = v; }
    public Limits getLimits() { return limits; }
}
