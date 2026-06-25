package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.broker.BrokerOrderRequest;
import com.tossai.trading.domain.broker.BrokerOrderResponse;

import java.util.Optional;

/**
 * 증권사 주문 포트. 유일한 증권사 호출 추상화 지점.
 * 실제 토스증권 API 스펙 확정 전까지 Mock 구현을 사용한다(구현 규칙 11).
 */
public interface BrokerPort {
    BrokerOrderResponse placeOrder(BrokerOrderRequest request);

    BrokerOrderResponse cancelOrder(String brokerOrderRef, String idempotencyKey);

    /**
     * 멱등키로 주문 접수/체결 여부 조회. 타임아웃 등 결과 불명 시 재동기화에 사용한다.
     * 증권사에 해당 주문이 없으면 빈 값.
     */
    Optional<BrokerOrderResponse> queryOrder(String idempotencyKey);
}
