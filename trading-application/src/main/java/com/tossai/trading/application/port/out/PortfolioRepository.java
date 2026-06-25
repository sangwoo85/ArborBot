package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.portfolio.Portfolio;

import java.math.BigDecimal;

public interface PortfolioRepository {
    /** 현재 포트폴리오 스냅샷(현금/주문가능금액/포지션). */
    Portfolio getCurrent();

    /** 매수 체결 반영: 현금 차감 + 포지션 증가(평균단가 갱신). */
    void applyBuy(String symbol, String sector, long quantity, BigDecimal price);

    /** 매도 체결 반영: 현금 증가 + 포지션 감소. */
    void applySell(String symbol, long quantity, BigDecimal price);
}
