package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_execution", indexes = @Index(name = "idx_exec_order", columnList = "orderId"))
public class OrderExecutionEntity {

    @Id
    @Column(name = "execution_id", length = 64)
    private String executionId;
    private String orderId;
    private String correlationId;
    private long filledQuantity;
    @Column(precision = 19, scale = 4)
    private BigDecimal avgFillPrice;
    @Column(precision = 19, scale = 4)
    private BigDecimal fee;
    @Column(precision = 19, scale = 4)
    private BigDecimal tax;
    @Column(length = 80)
    private String brokerOrderRef;
    private Instant executedAt;

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String v) { this.executionId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public long getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(long v) { this.filledQuantity = v; }
    public BigDecimal getAvgFillPrice() { return avgFillPrice; }
    public void setAvgFillPrice(BigDecimal v) { this.avgFillPrice = v; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal v) { this.fee = v; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal v) { this.tax = v; }
    public String getBrokerOrderRef() { return brokerOrderRef; }
    public void setBrokerOrderRef(String v) { this.brokerOrderRef = v; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant v) { this.executedAt = v; }
}
