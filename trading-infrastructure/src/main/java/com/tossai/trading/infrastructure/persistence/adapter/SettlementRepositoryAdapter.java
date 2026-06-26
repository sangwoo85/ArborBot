package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.SettlementRepository;
import com.tossai.trading.domain.portfolio.SettlementLot;
import com.tossai.trading.infrastructure.persistence.entity.SettlementLotEntity;
import com.tossai.trading.infrastructure.persistence.jpa.SettlementLotJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SettlementLotJpaRepository jpa;

    public SettlementRepositoryAdapter(SettlementLotJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(SettlementLot lot) {
        SettlementLotEntity e = new SettlementLotEntity();
        e.setLotId(lot.lotId());
        e.setSymbol(lot.symbol());
        e.setQuantity(lot.quantity());
        e.setSettleDate(lot.settleDate());
        e.setSettled(lot.settled());
        jpa.save(e);
    }

    @Override
    public List<SettlementLot> findDue(LocalDate asOf) {
        return jpa.findBySettledFalseAndSettleDateLessThanEqual(asOf).stream()
                .map(e -> new SettlementLot(e.getLotId(), e.getSymbol(), e.getQuantity(),
                        e.getSettleDate(), e.isSettled()))
                .toList();
    }

    @Override
    public void markSettled(String lotId) {
        jpa.findById(lotId).ifPresent(e -> {
            e.setSettled(true);
            jpa.save(e);
        });
    }

    @Override
    public List<SettlementLot> findUnsettledBySymbol(String symbol) {
        return jpa.findBySymbolAndSettledFalseOrderBySettleDateAsc(symbol).stream()
                .map(e -> new SettlementLot(e.getLotId(), e.getSymbol(), e.getQuantity(),
                        e.getSettleDate(), e.isSettled()))
                .toList();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteUnsettledBySymbol(String symbol) {
        jpa.deleteBySymbolAndSettledFalse(symbol);
    }
}
