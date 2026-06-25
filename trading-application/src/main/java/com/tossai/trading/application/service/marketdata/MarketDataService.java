package com.tossai.trading.application.service.marketdata;

import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.domain.market.Instrument;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * market-data-service 진입점(다음 단계 골격).
 * 현재는 MarketDataPort(Mock 어댑터)를 통해 종목 상태/현재가를 제공한다.
 * 향후 실시간 수집/정규화 파이프라인으로 대체한다.
 */
@Service
public class MarketDataService {

    private final MarketDataPort marketDataPort;

    public MarketDataService(MarketDataPort marketDataPort) {
        this.marketDataPort = marketDataPort;
    }

    public Optional<Instrument> getInstrument(String symbol) {
        return marketDataPort.getInstrument(symbol);
    }

    /** 참조가: 시장 현재가 우선, 없으면 fallback 사용. */
    public BigDecimal referencePrice(String symbol, BigDecimal fallback) {
        return marketDataPort.lastPrice(symbol).orElse(fallback);
    }

    public String sectorOf(String symbol) {
        return marketDataPort.getInstrument(symbol).map(Instrument::sector).orElse("UNKNOWN");
    }
}
