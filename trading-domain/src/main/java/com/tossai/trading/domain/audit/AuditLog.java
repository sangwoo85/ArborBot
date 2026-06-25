package com.tossai.trading.domain.audit;

import java.time.Instant;

/** 감사 로그(append-only). 주문/한도/KillSwitch/모드 등 중요 이벤트를 기록한다. */
public record AuditLog(
        String auditId,
        String correlationId,
        String category,    // ORDER / RISK / KILL_SWITCH / SIGNAL / MODE
        String action,
        String detail,
        String actor,
        Instant occurredAt
) {
}
