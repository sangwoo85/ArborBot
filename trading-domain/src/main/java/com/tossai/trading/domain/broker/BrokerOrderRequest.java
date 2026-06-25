package com.tossai.trading.domain.broker;

import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.order.OrderType;

import java.math.BigDecimal;

/** 증권사 주문 요청(내부 추상화). 실제 토스 API 필드는 Adapter 에서 매핑한다. */
public record BrokerOrderRequest(
        String idempotencyKey,
        String symbol,
        OrderSide side,
        OrderType orderType,
        long quantity,
        BigDecimal limitPrice
) {
}
