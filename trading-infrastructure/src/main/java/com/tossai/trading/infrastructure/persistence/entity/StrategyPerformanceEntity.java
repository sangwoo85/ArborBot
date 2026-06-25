package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "strategy_performance")
public class StrategyPerformanceEntity {

    @Id
    @Column(name = "strategy_id", length = 64)
    private String strategyId;
    @Column(precision = 9, scale = 4)
    private BigDecimal cumulativeReturnPercent;
    @Column(precision = 9, scale = 4)
    private BigDecimal maxDrawdownPercent;
    @Column(precision = 9, scale = 4)
    private BigDecimal winRatePercent;
    @Column(precision = 9, scale = 4)
    private BigDecimal profitFactor;
    @Column(precision = 9, scale = 4)
    private BigDecimal sharpeRatio;
    private int tradeCount;
    @Column(precision = 9, scale = 4)
    private BigDecimal netReturnAfterCostPercent;
    private int consecutiveLosses;

    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String v) { this.strategyId = v; }
    public BigDecimal getCumulativeReturnPercent() { return cumulativeReturnPercent; }
    public void setCumulativeReturnPercent(BigDecimal v) { this.cumulativeReturnPercent = v; }
    public BigDecimal getMaxDrawdownPercent() { return maxDrawdownPercent; }
    public void setMaxDrawdownPercent(BigDecimal v) { this.maxDrawdownPercent = v; }
    public BigDecimal getWinRatePercent() { return winRatePercent; }
    public void setWinRatePercent(BigDecimal v) { this.winRatePercent = v; }
    public BigDecimal getProfitFactor() { return profitFactor; }
    public void setProfitFactor(BigDecimal v) { this.profitFactor = v; }
    public BigDecimal getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(BigDecimal v) { this.sharpeRatio = v; }
    public int getTradeCount() { return tradeCount; }
    public void setTradeCount(int v) { this.tradeCount = v; }
    public BigDecimal getNetReturnAfterCostPercent() { return netReturnAfterCostPercent; }
    public void setNetReturnAfterCostPercent(BigDecimal v) { this.netReturnAfterCostPercent = v; }
    public int getConsecutiveLosses() { return consecutiveLosses; }
    public void setConsecutiveLosses(int v) { this.consecutiveLosses = v; }
}
