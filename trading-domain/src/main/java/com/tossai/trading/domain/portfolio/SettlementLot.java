package com.tossai.trading.domain.portfolio;

import java.time.LocalDate;

/**
 * 매수 결제 로트. 매수 체결 즉시 보유수량은 늘지만 매도가능수량은 결제일(T+2) 이후에 늘어난다.
 * 결제일은 {@link TradingCalendar} 로 영업일(주말+공휴일 제외) 기준 계산한다.
 */
public record SettlementLot(
        String lotId,
        String symbol,
        long quantity,
        LocalDate settleDate,
        boolean settled
) {
}
