package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "instrument")
public class InstrumentEntity {

    @Id
    @Column(name = "symbol", length = 32)
    private String symbol;
    private String sector;
    @Column(precision = 19, scale = 4)
    private BigDecimal lastPrice;
    private boolean tradable;
    private boolean halted;
    private boolean illiquid;

    public String getSymbol() { return symbol; }
    public void setSymbol(String v) { this.symbol = v; }
    public String getSector() { return sector; }
    public void setSector(String v) { this.sector = v; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal v) { this.lastPrice = v; }
    public boolean isTradable() { return tradable; }
    public void setTradable(boolean v) { this.tradable = v; }
    public boolean isHalted() { return halted; }
    public void setHalted(boolean v) { this.halted = v; }
    public boolean isIlliquid() { return illiquid; }
    public void setIlliquid(boolean v) { this.illiquid = v; }
}
