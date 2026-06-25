package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.market.Instrument;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 시장 데이터 포트. 종목 상태(거래가능/정지/유동성)와 현재가를 공급한다.
 * 실제로는 market-data-service 가 수집/정규화한 결과를 제공한다.
 */
public interface MarketDataPort {
    Optional<Instrument> getInstrument(String symbol);

    /** 현재가(참조가). 없으면 빈 값. */
    Optional<BigDecimal> lastPrice(String symbol);
}
