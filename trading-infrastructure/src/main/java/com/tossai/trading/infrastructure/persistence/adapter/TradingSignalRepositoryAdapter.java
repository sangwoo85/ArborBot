package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.TradingSignalRepository;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import com.tossai.trading.infrastructure.persistence.entity.TradingSignalEntity;
import com.tossai.trading.infrastructure.persistence.jpa.TradingSignalJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class TradingSignalRepositoryAdapter implements TradingSignalRepository {

    private final TradingSignalJpaRepository jpa;

    public TradingSignalRepositoryAdapter(TradingSignalJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TradingSignal save(TradingSignal s) {
        jpa.save(toEntity(s));
        return s;
    }

    @Override
    public Optional<TradingSignal> findById(String signalId) {
        return jpa.findById(signalId).map(this::toDomain);
    }

    @Override
    public List<TradingSignal> findRecent(int limit) {
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    private TradingSignalEntity toEntity(TradingSignal s) {
        TradingSignalEntity e = new TradingSignalEntity();
        e.setSignalId(s.signalId());
        e.setStrategyId(s.strategyId());
        e.setStrategyVersion(s.strategyVersion());
        e.setModelVersion(s.modelVersion());
        e.setSymbol(s.symbol());
        e.setSignalType(s.signalType().name());
        e.setConfidenceScore(s.confidenceScore());
        e.setRecommendedPositionSizePercent(s.recommendedPositionSizePercent());
        e.setEntryPriceMin(s.entryPriceMin());
        e.setEntryPriceMax(s.entryPriceMax());
        e.setStopLossPrice(s.stopLossPrice());
        e.setTakeProfitPrice(s.takeProfitPrice());
        e.setHoldingPeriod(s.holdingPeriod());
        e.setValidUntil(s.validUntil());
        e.setMarketRegime(s.marketRegime() == null ? null : s.marketRegime().name());
        e.setRationale(s.rationale());
        e.setRiskFlags(s.riskFlags() == null ? "" : String.join(",", s.riskFlags()));
        e.setCreatedAt(s.createdAt());
        return e;
    }

    private TradingSignal toDomain(TradingSignalEntity e) {
        List<String> flags = (e.getRiskFlags() == null || e.getRiskFlags().isBlank())
                ? List.of() : Arrays.asList(e.getRiskFlags().split(","));
        return new TradingSignal(
                e.getSignalId(), e.getStrategyId(), e.getStrategyVersion(), e.getModelVersion(),
                e.getSymbol(), SignalType.valueOf(e.getSignalType()), e.getConfidenceScore(),
                e.getRecommendedPositionSizePercent(), e.getEntryPriceMin(), e.getEntryPriceMax(),
                e.getStopLossPrice(), e.getTakeProfitPrice(), e.getHoldingPeriod(), e.getValidUntil(),
                e.getMarketRegime() == null ? null : MarketRegime.valueOf(e.getMarketRegime()),
                e.getRationale(), flags, e.getCreatedAt());
    }
}
