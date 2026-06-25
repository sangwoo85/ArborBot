package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.audit.AuditLog;

import java.util.List;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    List<AuditLog> findRecent(int limit);
}
