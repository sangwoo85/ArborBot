package com.tossai.trading.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 일일 위험 사용량(누적 주문금액, 일일 손익) 추적. */
public interface DailyRiskUsageRepository {
    BigDecimal todayOrderAmount(LocalDate date);

    BigDecimal todayRealizedLoss(LocalDate date);

    void addOrderAmount(LocalDate date, BigDecimal amount);

    /** 실현 손실 누적(양수 = 손실 금액). 매도 체결 시 손실분을 더한다. */
    void addRealizedLoss(LocalDate date, BigDecimal lossAmount);
}
