package com.tossai.trading.infrastructure.broker;

import com.tossai.trading.application.port.out.BrokerPort;
import com.tossai.trading.common.util.SensitiveDataMasker;
import com.tossai.trading.domain.broker.BrokerOrderRequest;
import com.tossai.trading.domain.broker.BrokerOrderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 토스증권 Adapter 의 Mock 구현(구현 규칙 11).
 * 실제 API 스펙 확정 전까지 결정적 체결을 시뮬레이션한다.
 * 외부 호출 지점에 retry/circuit breaker 를 적용한다(구현 규칙 7).
 * timeout 은 실제 HTTP 클라이언트(RestClient/WebClient) 요청 타임아웃으로 적용한다
 * (Mock 은 동기 호출이라 @TimeLimiter 비적용 — 실제 어댑터에서 보완).
 *
 * <p>⚠️ 실제 스펙 확정 시 보완 필요:
 * 인증(OAuth/토큰 발급·회전), 주문 엔드포인트/필드, 주문번호 체계, 부분체결 폴링,
 * 오류 코드 매핑, 멱등성 헤더, 계좌번호 전달 방식(마스킹/암호화).
 */
@Component
public class MockTossBrokerAdapter implements BrokerPort {

    private static final Logger log = LoggerFactory.getLogger(MockTossBrokerAdapter.class);

    /** 증권사 측에 접수된 주문(멱등키 기준). queryOrder 재동기화에 사용. */
    private final Map<String, BrokerOrderResponse> received = new ConcurrentHashMap<>();

    /**
     * 테스트/시연용: true 면 placeOrder 가 "주문은 접수되었으나 응답이 유실"되는 타임아웃을 모사한다.
     * (주문은 received 에 저장 후 예외 발생 → 이후 queryOrder 로 복구 가능)
     */
    private volatile boolean simulatePlaceTimeout = false;

    public void setSimulatePlaceTimeout(boolean v) {
        this.simulatePlaceTimeout = v;
    }

    @Override
    @CircuitBreaker(name = "broker")
    @Retry(name = "broker")
    public BrokerOrderResponse placeOrder(BrokerOrderRequest request) {
        // 로그에는 비밀정보(계좌/토큰)를 남기지 않는다. idempotencyKey 도 마스킹.
        log.info("[MOCK-TOSS] placeOrder symbol={} side={} qty={} idem={}",
                request.symbol(), request.side(), request.quantity(),
                SensitiveDataMasker.maskToken(request.idempotencyKey()));

        // Mock: 지정가가 있으면 전량 체결, 없으면 시장가로 가정해 전량 체결.
        BigDecimal fillPrice = request.limitPrice() != null ? request.limitPrice() : BigDecimal.ZERO;
        String brokerRef = "MOCK-" + Math.abs(request.idempotencyKey().hashCode());
        BrokerOrderResponse resp = new BrokerOrderResponse(
                true, brokerRef, request.quantity(), fillPrice, "0000", "OK(mock)");

        // 멱등: 증권사 측에 먼저 기록한 뒤 응답을 반환한다.
        received.putIfAbsent(request.idempotencyKey(), resp);

        if (simulatePlaceTimeout) {
            // 주문은 접수되었으나 응답이 유실된 상황(결과 불명).
            throw new RuntimeException("simulated broker timeout (response lost)");
        }
        return resp;
    }

    @Override
    public Optional<BrokerOrderResponse> queryOrder(String idempotencyKey) {
        log.info("[MOCK-TOSS] queryOrder idem={}", SensitiveDataMasker.maskToken(idempotencyKey));
        return Optional.ofNullable(received.get(idempotencyKey));
    }

    @Override
    @CircuitBreaker(name = "broker")
    @Retry(name = "broker")
    public BrokerOrderResponse cancelOrder(String brokerOrderRef, String idempotencyKey) {
        log.info("[MOCK-TOSS] cancelOrder ref={} idem={}", brokerOrderRef,
                SensitiveDataMasker.maskToken(idempotencyKey));
        return new BrokerOrderResponse(true, brokerOrderRef, 0, null, "0000", "CANCEL-OK(mock)");
    }
}
