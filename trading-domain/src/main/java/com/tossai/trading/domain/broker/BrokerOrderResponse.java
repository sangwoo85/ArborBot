package com.tossai.trading.domain.broker;

import java.math.BigDecimal;

/** 증권사 주문 응답(내부 추상화). */
public record BrokerOrderResponse(
        boolean accepted,
        String brokerOrderRef,
        long filledQuantity,
        BigDecimal avgFillPrice,
        String resultCode,
        String resultMessage
) {
    public static BrokerOrderResponse rejected(String code, String message) {
        return new BrokerOrderResponse(false, null, 0, null, code, message);
    }
}
