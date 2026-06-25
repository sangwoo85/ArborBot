package com.tossai.trading.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

/** 체결 결과. 주문(Order)과 분리 저장된다. */
public record OrderExecution(
        String executionId,
        String orderId,
        String correlationId,
        long filledQuantity,
        BigDecimal avgFillPrice,
        BigDecimal fee,
        BigDecimal tax,
        String brokerOrderRef,
        Instant executedAt
) {
}
