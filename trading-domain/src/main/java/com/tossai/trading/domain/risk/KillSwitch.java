package com.tossai.trading.domain.risk;

import java.time.Instant;

/** Kill Switch 상태. 활성 시 신규 진입 주문은 차단된다. */
public record KillSwitch(
        String scope,        // GLOBAL / STRATEGY / SYMBOL
        String target,       // STRATEGY/SYMBOL 일 때 대상, GLOBAL 이면 null
        boolean enabled,
        String reason,
        String actor,
        Instant updatedAt
) {
    public static KillSwitch globalOff() {
        return new KillSwitch("GLOBAL", null, false, null, "system", Instant.now());
    }
}
