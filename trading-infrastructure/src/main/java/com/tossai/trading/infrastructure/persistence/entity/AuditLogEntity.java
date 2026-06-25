package com.tossai.trading.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @Column(name = "audit_id", length = 64)
    private String auditId;
    private String correlationId;
    private String category;
    private String action;
    @Column(length = 1000)
    private String detail;
    private String actor;
    private Instant occurredAt;

    public String getAuditId() { return auditId; }
    public void setAuditId(String v) { this.auditId = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }
    public String getActor() { return actor; }
    public void setActor(String v) { this.actor = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { this.occurredAt = v; }
}
