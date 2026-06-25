package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "account_balance")
public class AccountBalanceEntity {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;
    @Column(precision = 19, scale = 4)
    private BigDecimal cash;
    @Column(precision = 19, scale = 4)
    private BigDecimal orderableAmount;

    public String getAccountId() { return accountId; }
    public void setAccountId(String v) { this.accountId = v; }
    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal v) { this.cash = v; }
    public BigDecimal getOrderableAmount() { return orderableAmount; }
    public void setOrderableAmount(BigDecimal v) { this.orderableAmount = v; }
}
