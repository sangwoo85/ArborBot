package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "positions")
public class PositionEntity {

    @Id
    @Column(name = "symbol", length = 32)
    private String symbol;
    private String sector;
    private long quantity;
    private long sellableQuantity;
    @Column(precision = 19, scale = 4)
    private BigDecimal avgPrice;
    @Column(precision = 19, scale = 4)
    private BigDecimal lastPrice;
    @Column(precision = 19, scale = 4)
    private BigDecimal evaluationAmount;
    @Column(precision = 19, scale = 4)
    private BigDecimal unrealizedPnl;

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getSector() { return sector; }
    public void setSector(String v) { this.sector = v; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long v) { this.quantity = v; }
    public long getSellableQuantity() { return sellableQuantity; }
    public void setSellableQuantity(long v) { this.sellableQuantity = v; }
    public BigDecimal getAvgPrice() { return avgPrice; }
    public void setAvgPrice(BigDecimal v) { this.avgPrice = v; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal v) { this.lastPrice = v; }
    public BigDecimal getEvaluationAmount() { return evaluationAmount; }
    public void setEvaluationAmount(BigDecimal v) { this.evaluationAmount = v; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal v) { this.unrealizedPnl = v; }
}
