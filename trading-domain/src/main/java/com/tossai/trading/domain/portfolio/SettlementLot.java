package com.tossai.trading.domain.portfolio;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 매수 결제 로트. 매수 체결 즉시 보유수량은 늘지만 매도가능수량은 결제일(T+2) 이후에 늘어난다.
 */
public record SettlementLot(
        String lotId,
        String symbol,
        long quantity,
        LocalDate settleDate,
        boolean settled
) {

    /**
     * T+2 결제일 계산(영업일 기준, 주말 제외).
     * 참고: 공휴일은 고려하지 않음 — 실제 캘린더 연동 시 보정 필요.
     */
    public static LocalDate settlementDate(LocalDate tradeDate, int businessDays) {
        LocalDate d = tradeDate;
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }
}
