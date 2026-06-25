package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trading_strategy")
public class StrategyEntity {

    @Id
    @Column(name = "strategy_id", length = 64)
    private String strategyId;
    private String version;
    @Column(length = 500)
    private String description;
    private String targetRegime;
    private boolean active;
    private boolean autoTradingEligible;

    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String v) { this.strategyId = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getTargetRegime() { return targetRegime; }
    public void setTargetRegime(String v) { this.targetRegime = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public boolean isAutoTradingEligible() { return autoTradingEligible; }
    public void setAutoTradingEligible(boolean v) { this.autoTradingEligible = v; }
}
