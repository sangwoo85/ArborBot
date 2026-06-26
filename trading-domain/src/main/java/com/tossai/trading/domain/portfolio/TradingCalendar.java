package com.tossai.trading.domain.portfolio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 거래 영업일 캘린더. 주말 + 공휴일을 제외해 영업일을 계산한다.
 *
 * <p>공휴일은 <b>외부에서 주입</b>한다(거래소 공식 휴장일 캘린더). 코드에 특정 연도의 휴일을
 * 추측해 하드코딩하지 않는다(절대 규칙 6). 휴일 집합이 비면 주말만 제외한다.
 */
public final class TradingCalendar {

    private final Set<LocalDate> holidays;

    public TradingCalendar(Set<LocalDate> holidays) {
        this.holidays = holidays == null ? Set.of() : Set.copyOf(holidays);
    }

    public boolean isBusinessDay(LocalDate d) {
        return d.getDayOfWeek() != DayOfWeek.SATURDAY
                && d.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(d);
    }

    /** start 다음 영업일부터 n 영업일 뒤 날짜(T+n 결제일 계산). */
    public LocalDate addBusinessDays(LocalDate start, int n) {
        LocalDate d = start;
        int added = 0;
        while (added < n) {
            d = d.plusDays(1);
            if (isBusinessDay(d)) {
                added++;
            }
        }
        return d;
    }
}
