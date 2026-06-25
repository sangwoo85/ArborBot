package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_idem", columnList = "idempotencyKey"),
        @Index(name = "idx_orders_created", columnList = "createdAt")
})
public class OrderEntity {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;
    private String correlationId;
    private String strategySignalId;
    private String portfolioDecisionId;
    private String symbol;
    private String side;
    private String orderType;
    private long quantity;
    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;
    @Column(length = 80)
    private String idempotencyKey;
    @Column(name = "order_mode")
    private String mode;
    private boolean dryRun;
    private String status;
    private long filledQuantity;
    @Column(length = 500)
    private String rejectReason;
    private Instant createdAt;
    private Instant updatedAt;

    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public String getStrategySignalId() { return strategySignalId; }
    public void setStrategySignalId(String v) { this.strategySignalId = v; }
    public String getPortfolioDecisionId() { return portfolioDecisionId; }
    public void setPortfolioDecisionId(String v) { this.portfolioDecisionId = v; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getSide() { return side; }
    public void setSide(String v) { this.side = v; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long v) { this.quantity = v; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(BigDecimal v) { this.limitPrice = v; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { this.mode = v; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean v) { this.dryRun = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public long getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(long v) { this.filledQuantity = v; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String v) { this.rejectReason = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
