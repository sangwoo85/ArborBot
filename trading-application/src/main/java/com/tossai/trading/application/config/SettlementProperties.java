package com.tossai.trading.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 결제 설정. 결제 주기(영업일)와 거래소 공휴일을 주입받는다.
 * 공휴일은 거래소 공식 휴장일을 설정(yml/env)으로 채운다(추측 하드코딩 금지).
 */
@ConfigurationProperties(prefix = "trading.settlement")
public class SettlementProperties {

    /** 결제 주기. 한국 주식 현물 기준 T+2. */
    private int businessDays = 2;

    /** 거래소 휴장일(ISO-8601: yyyy-MM-dd) 목록. 기본 비어 있음(주말만 제외). */
    private List<String> holidays = new ArrayList<>();

    public int getBusinessDays() {
        return businessDays;
    }

    public void setBusinessDays(int businessDays) {
        this.businessDays = businessDays;
    }

    public List<String> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<String> holidays) {
        this.holidays = holidays;
    }

    /** 휴일 문자열 → LocalDate 집합. */
    public Set<LocalDate> holidaySet() {
        return holidays.stream().map(LocalDate::parse).collect(Collectors.toSet());
    }
}
