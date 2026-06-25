package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.DailyRiskUsageRepository;
import com.tossai.trading.infrastructure.persistence.entity.DailyRiskUsageEntity;
import com.tossai.trading.infrastructure.persistence.jpa.DailyRiskUsageJpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public class DailyRiskUsageRepositoryAdapter implements DailyRiskUsageRepository {

    private final DailyRiskUsageJpaRepository jpa;

    public DailyRiskUsageRepositoryAdapter(DailyRiskUsageJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BigDecimal todayOrderAmount(LocalDate date) {
        return jpa.findById(date).map(DailyRiskUsageEntity::getOrderAmount).orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal todayRealizedLoss(LocalDate date) {
        return jpa.findById(date).map(DailyRiskUsageEntity::getRealizedLoss).orElse(BigDecimal.ZERO);
    }

    @Override
    public void addOrderAmount(LocalDate date, BigDecimal amount) {
        DailyRiskUsageEntity e = jpa.findById(date).orElseGet(() -> {
            DailyRiskUsageEntity n = new DailyRiskUsageEntity();
            n.setUsageDate(date);
            n.setOrderAmount(BigDecimal.ZERO);
            n.setRealizedLoss(BigDecimal.ZERO);
            return n;
        });
        e.setOrderAmount(e.getOrderAmount().add(amount == null ? BigDecimal.ZERO : amount));
        jpa.save(e);
    }

    @Override
    public void addRealizedLoss(LocalDate date, BigDecimal lossAmount) {
        if (lossAmount == null || lossAmount.signum() <= 0) {
            return; // 손실(양수)만 누적
        }
        DailyRiskUsageEntity e = jpa.findById(date).orElseGet(() -> {
            DailyRiskUsageEntity n = new DailyRiskUsageEntity();
            n.setUsageDate(date);
            n.setOrderAmount(BigDecimal.ZERO);
            n.setRealizedLoss(BigDecimal.ZERO);
            return n;
        });
        e.setRealizedLoss(e.getRealizedLoss().add(lossAmount));
        jpa.save(e);
    }
}
