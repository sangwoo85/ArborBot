package com.tossai.trading.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingCalendarTest {

    private LocalDate next(DayOfWeek dow) {
        LocalDate d = LocalDate.of(2026, 6, 1);
        while (d.getDayOfWeek() != dow) {
            d = d.plusDays(1);
        }
        return d;
    }

    @Test
    void weekendsAreNotBusinessDays() {
        TradingCalendar cal = new TradingCalendar(Set.of());
        assertFalse(cal.isBusinessDay(next(DayOfWeek.SATURDAY)));
        assertFalse(cal.isBusinessDay(next(DayOfWeek.SUNDAY)));
        assertTrue(cal.isBusinessDay(next(DayOfWeek.WEDNESDAY)));
    }

    @Test
    void addBusinessDays_skipsWeekend() {
        TradingCalendar cal = new TradingCalendar(Set.of());
        LocalDate friday = next(DayOfWeek.FRIDAY);
        // 금요일 +1 영업일 → 토/일 건너뛰어 월요일(+3일)
        assertEquals(friday.plusDays(3), cal.addBusinessDays(friday, 1));
        // T+2 → 화요일(+4일)
        assertEquals(friday.plusDays(4), cal.addBusinessDays(friday, 2));
    }

    @Test
    void addBusinessDays_skipsHoliday() {
        LocalDate friday = next(DayOfWeek.FRIDAY);
        LocalDate monday = friday.plusDays(3);
        TradingCalendar cal = new TradingCalendar(Set.of(monday));   // 월요일이 공휴일
        // 금요일 +1 영업일 → 토/일/월(휴일) 건너뛰어 화요일(+4일)
        assertEquals(friday.plusDays(4), cal.addBusinessDays(friday, 1));
    }

    @Test
    void addBusinessDays_midweekT2() {
        TradingCalendar cal = new TradingCalendar(Set.of());
        LocalDate wed = next(DayOfWeek.WEDNESDAY);
        assertEquals(wed.plusDays(2), cal.addBusinessDays(wed, 2));   // 수 → 금
    }
}
