package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.infrastructure.persistence.entity.AuditLogEntity;
import com.tossai.trading.infrastructure.persistence.jpa.AuditLogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditLog save(AuditLog a) {
        AuditLogEntity e = new AuditLogEntity();
        e.setAuditId(a.auditId());
        e.setCorrelationId(a.correlationId());
        e.setCategory(a.category());
        e.setAction(a.action());
        e.setDetail(a.detail());
        e.setActor(a.actor());
        e.setOccurredAt(a.occurredAt());
        jpa.save(e);
        return a;
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        return jpa.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit)).stream()
                .map(e -> new AuditLog(e.getAuditId(), e.getCorrelationId(), e.getCategory(),
                        e.getAction(), e.getDetail(), e.getActor(), e.getOccurredAt()))
                .toList();
    }
}
