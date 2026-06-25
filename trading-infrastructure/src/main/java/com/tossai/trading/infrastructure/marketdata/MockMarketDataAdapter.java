package com.tossai.trading.infrastructure.marketdata;

import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.domain.market.Instrument;
import com.tossai.trading.infrastructure.persistence.jpa.InstrumentJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * market-data 의 Mock 구현. instrument 테이블(시드)에서 종목 상태/현재가를 읽는다.
 * 미등록 종목은 기본적으로 거래 가능(tradable=true)으로 간주한다.
 * 실제 구현에서는 시세/호가/거래정지 피드를 수집해 갱신한다.
 */
@Component
@ConditionalOnProperty(prefix = "trading.marketdata", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockMarketDataAdapter implements MarketDataPort {

    private final InstrumentJpaRepository jpa;

    public MockMarketDataAdapter(InstrumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Instrument> getInstrument(String symbol) {
        return jpa.findById(symbol).map(e -> new Instrument(
                e.getSymbol(), e.getSector(), e.getLastPrice(),
                e.isTradable(), e.isHalted(), e.isIlliquid()));
    }

    @Override
    public Optional<BigDecimal> lastPrice(String symbol) {
        return jpa.findById(symbol)
                .map(com.tossai.trading.infrastructure.persistence.entity.InstrumentEntity::getLastPrice);
    }
}
