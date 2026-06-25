package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.signal.TradingSignal;

import java.util.List;
import java.util.Optional;

public interface TradingSignalRepository {
    TradingSignal save(TradingSignal signal);

    Optional<TradingSignal> findById(String signalId);

    List<TradingSignal> findRecent(int limit);
}
