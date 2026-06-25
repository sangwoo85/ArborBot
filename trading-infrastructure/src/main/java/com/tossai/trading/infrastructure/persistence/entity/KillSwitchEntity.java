package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "kill_switch")
public class KillSwitchEntity {

    /** scope|target 조합 키. GLOBAL 은 "GLOBAL|". */
    @Id
    @Column(name = "switch_key", length = 128)
    private String switchKey;
    private String scope;
    private String target;
    private boolean enabled;
    @Column(length = 500)
    private String reason;
    private String actor;
    private Instant updatedAt;

    public String getSwitchKey() { return switchKey; }
    public void setSwitchKey(String v) { this.switchKey = v; }
    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }
    public String getTarget() { return target; }
    public void setTarget(String v) { this.target = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getActor() { return actor; }
    public void setActor(String v) { this.actor = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
