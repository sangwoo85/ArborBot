package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trading_signal")
public class TradingSignalEntity {

    @Id
    @Column(name = "signal_id", length = 64)
    private String signalId;
    private String strategyId;
    private String strategyVersion;
    private String modelVersion;
    private String symbol;
    private String signalType;
    private int confidenceScore;
    @Column(precision = 9, scale = 4)
    private BigDecimal recommendedPositionSizePercent;
    @Column(precision = 19, scale = 4)
    private BigDecimal entryPriceMin;
    @Column(precision = 19, scale = 4)
    private BigDecimal entryPriceMax;
    @Column(precision = 19, scale = 4)
    private BigDecimal stopLossPrice;
    @Column(precision = 19, scale = 4)
    private BigDecimal takeProfitPrice;
    private String holdingPeriod;
    private Instant validUntil;
    private String marketRegime;
    @Column(length = 2000)
    private String rationale;
    @Column(length = 1000)
    private String riskFlags;
    private Instant createdAt;

    public String getSignalId() { return signalId; }
    public void setSignalId(String v) { this.signalId = v; }
    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String v) { this.strategyId = v; }
    public String getStrategyVersion() { return strategyVersion; }
    public void setStrategyVersion(String v) { this.strategyVersion = v; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String v) { this.modelVersion = v; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getSignalType() { return signalType; }
    public void setSignalType(String v) { this.signalType = v; }
    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int v) { this.confidenceScore = v; }
    public BigDecimal getRecommendedPositionSizePercent() { return recommendedPositionSizePercent; }
    public void setRecommendedPositionSizePercent(BigDecimal v) { this.recommendedPositionSizePercent = v; }
    public BigDecimal getEntryPriceMin() { return entryPriceMin; }
    public void setEntryPriceMin(BigDecimal v) { this.entryPriceMin = v; }
    public BigDecimal getEntryPriceMax() { return entryPriceMax; }
    public void setEntryPriceMax(BigDecimal v) { this.entryPriceMax = v; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal v) { this.stopLossPrice = v; }
    public BigDecimal getTakeProfitPrice() { return takeProfitPrice; }
    public void setTakeProfitPrice(BigDecimal v) { this.takeProfitPrice = v; }
    public String getHoldingPeriod() { return holdingPeriod; }
    public void setHoldingPeriod(String v) { this.holdingPeriod = v; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant v) { this.validUntil = v; }
    public String getMarketRegime() { return marketRegime; }
    public void setMarketRegime(String v) { this.marketRegime = v; }
    public String getRationale() { return rationale; }
    public void setRationale(String v) { this.rationale = v; }
    public String getRiskFlags() { return riskFlags; }
    public void setRiskFlags(String v) { this.riskFlags = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
