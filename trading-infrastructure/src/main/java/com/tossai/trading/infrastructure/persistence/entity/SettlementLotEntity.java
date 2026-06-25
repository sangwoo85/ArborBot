package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "settlement_lot", indexes = @Index(name = "idx_settle_due", columnList = "settled,settleDate"))
public class SettlementLotEntity {

    @Id
    @Column(name = "lot_id", length = 64)
    private String lotId;
    private String symbol;
    private long quantity;
    private LocalDate settleDate;
    private boolean settled;

    public String getLotId() { return lotId; }
    public void setLotId(String v) { this.lotId = v; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long v) { this.quantity = v; }
    public LocalDate getSettleDate() { return settleDate; }
    public void setSettleDate(LocalDate v) { this.settleDate = v; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean v) { this.settled = v; }
}
