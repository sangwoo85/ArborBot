package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.portfolio.Portfolio;

import java.math.BigDecimal;

public interface PortfolioRepository {
    /** 현재 포트폴리오 스냅샷(현금/주문가능금액/포지션). */
    Portfolio getCurrent();

    /** 매수 체결 반영: 현금 차감 + 보유수량 증가(평균단가 갱신). 매도가능수량은 T+2 결제 후 증가. */
    void applyBuy(String symbol, String sector, long quantity, BigDecimal price);

    /** 매도 체결 반영: 현금 증가 + 포지션 감소. */
    void applySell(String symbol, long quantity, BigDecimal price);

    /** T+2 결제 완료분을 매도가능수량으로 전환(증가). */
    void increaseSellable(String symbol, long quantity);
}
