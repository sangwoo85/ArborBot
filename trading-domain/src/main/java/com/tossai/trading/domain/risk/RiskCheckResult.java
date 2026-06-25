package com.tossai.trading.domain.risk;

import java.time.Instant;
import java.util.List;

/** Risk Engine 검증 결과. 위반이 있으면 REJECT. */
public record RiskCheckResult(
        RiskDecision decision,
        List<RiskViolation> violations,
        String killSwitchScope,   // OFF / GLOBAL / STRATEGY / SYMBOL
        Instant evaluatedAt
) {

    public boolean allowed() {
        return decision == RiskDecision.ALLOW;
    }

    public boolean rejected() {
        return decision == RiskDecision.REJECT;
    }

    public static RiskCheckResult allow() {
        return new RiskCheckResult(RiskDecision.ALLOW, List.of(), "OFF", Instant.now());
    }

    public static RiskCheckResult reject(List<RiskViolation> violations) {
        return new RiskCheckResult(RiskDecision.REJECT, violations, "OFF", Instant.now());
    }

    public static RiskCheckResult pending(List<RiskViolation> notes) {
        return new RiskCheckResult(RiskDecision.PENDING_APPROVAL, notes, "OFF", Instant.now());
    }
}
