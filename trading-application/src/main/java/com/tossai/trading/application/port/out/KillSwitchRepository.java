package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.risk.KillSwitch;

import java.util.List;
import java.util.Optional;

public interface KillSwitchRepository {
    KillSwitch getGlobal();

    Optional<KillSwitch> find(String scope, String target);

    void save(KillSwitch killSwitch);

    /** 활성화된 모든 Kill Switch. */
    List<KillSwitch> findEnabled();
}
