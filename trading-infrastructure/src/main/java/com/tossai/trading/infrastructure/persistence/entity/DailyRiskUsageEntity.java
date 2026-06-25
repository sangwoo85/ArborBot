package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_risk_usage")
public class DailyRiskUsageEntity {

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;
    @Column(precision = 19, scale = 4)
    private BigDecimal orderAmount;
    @Column(precision = 19, scale = 4)
    private BigDecimal realizedLoss;

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate v) { this.usageDate = v; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal v) { this.orderAmount = v; }
    public BigDecimal getRealizedLoss() { return realizedLoss; }
    public void setRealizedLoss(BigDecimal v) { this.realizedLoss = v; }
}
