package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.KillSwitchRepository;
import com.tossai.trading.domain.risk.KillSwitch;
import com.tossai.trading.infrastructure.persistence.entity.KillSwitchEntity;
import com.tossai.trading.infrastructure.persistence.jpa.KillSwitchJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class KillSwitchRepositoryAdapter implements KillSwitchRepository {

    private final KillSwitchJpaRepository jpa;

    public KillSwitchRepositoryAdapter(KillSwitchJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public KillSwitch getGlobal() {
        return jpa.findById(key("GLOBAL", null)).map(this::toDomain).orElseGet(KillSwitch::globalOff);
    }

    @Override
    public Optional<KillSwitch> find(String scope, String target) {
        return jpa.findById(key(scope, target)).map(this::toDomain);
    }

    @Override
    public void save(KillSwitch ks) {
        KillSwitchEntity e = jpa.findById(key(ks.scope(), ks.target())).orElseGet(KillSwitchEntity::new);
        e.setSwitchKey(key(ks.scope(), ks.target()));
        e.setScope(ks.scope());
        e.setTarget(ks.target());
        e.setEnabled(ks.enabled());
        e.setReason(ks.reason());
        e.setActor(ks.actor());
        e.setUpdatedAt(ks.updatedAt());
        jpa.save(e);
    }

    @Override
    public List<KillSwitch> findEnabled() {
        return jpa.findByEnabledTrue().stream().map(this::toDomain).toList();
    }

    private String key(String scope, String target) {
        return scope + "|" + (target == null ? "" : target);
    }

    private KillSwitch toDomain(KillSwitchEntity e) {
        return new KillSwitch(e.getScope(), e.getTarget(), e.isEnabled(),
                e.getReason(), e.getActor(), e.getUpdatedAt());
    }
}
