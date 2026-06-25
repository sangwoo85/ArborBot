package com.tossai.trading.application.service.aitrader;

import com.tossai.trading.domain.signal.SignalType;

import java.math.BigDecimal;

/** 신호 생성 요청 커맨드. */
public record GenerateSignalCommand(
        String symbol,
        String strategyId,
        String strategyVersion,
        SignalType requestedType,
        BigDecimal referencePrice,
        String holdingPeriod
) {
}
