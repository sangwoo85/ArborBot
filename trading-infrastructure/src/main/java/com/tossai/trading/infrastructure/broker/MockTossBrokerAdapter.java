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
    private final Map<String, Filling> received = new ConcurrentHashMap<>();

    /**
     * 테스트/시연용: true 면 placeOrder 가 "주문은 접수되었으나 응답이 유실"되는 타임아웃을 모사한다.
     * (주문은 received 에 저장 후 예외 발생 → 이후 queryOrder 로 복구 가능)
     */
    private volatile boolean simulatePlaceTimeout = false;

    /**
     * 테스트/시연용: 0 보다 크면 한 번에 이 수량씩만 체결되는 부분 체결을 모사한다.
     * placeOrder 가 첫 청크를, 이후 queryOrder 호출마다 다음 청크를 누적 체결한다.
     */
    private volatile long partialFillChunk = 0;

    public void setSimulatePlaceTimeout(boolean v) {
        this.simulatePlaceTimeout = v;
    }

    public void setPartialFillChunk(long chunk) {
        this.partialFillChunk = chunk;
    }

    /** 증권사 측 체결 진행 상태(요청수량/누적체결/체결가). */
    private static final class Filling {
        final long requested;
        long filled;
        final BigDecimal price;
        final String brokerRef;

        Filling(long requested, long filled, BigDecimal price, String brokerRef) {
            this.requested = requested;
            this.filled = filled;
            this.price = price;
            this.brokerRef = brokerRef;
        }

        BrokerOrderResponse toResponse() {
            return new BrokerOrderResponse(true, brokerRef, filled, price, "0000", "OK(mock)");
        }
    }

    @Override
    @CircuitBreaker(name = "broker")
    @Retry(name = "broker")
    public BrokerOrderResponse placeOrder(BrokerOrderRequest request) {
        // 로그에는 비밀정보(계좌/토큰)를 남기지 않는다. idempotencyKey 도 마스킹.
        log.info("[MOCK-TOSS] placeOrder symbol={} side={} qty={} idem={}",
                request.symbol(), request.side(), request.quantity(),
                SensitiveDataMasker.maskToken(request.idempotencyKey()));

        BigDecimal fillPrice = request.limitPrice() != null ? request.limitPrice() : BigDecimal.ZERO;
        String brokerRef = "MOCK-" + Math.abs(request.idempotencyKey().hashCode());

        // 멱등: 증권사 측에 먼저 기록한다(첫 청크 또는 전량 체결).
        long firstFill = partialFillChunk > 0
                ? Math.min(partialFillChunk, request.quantity())
                : request.quantity();
        Filling f = received.computeIfAbsent(request.idempotencyKey(),
                k -> new Filling(request.quantity(), firstFill, fillPrice, brokerRef));

        if (simulatePlaceTimeout) {
            // 주문은 접수되었으나 응답이 유실된 상황(결과 불명).
            throw new RuntimeException("simulated broker timeout (response lost)");
        }
        return f.toResponse();
    }

    @Override
    public Optional<BrokerOrderResponse> queryOrder(String idempotencyKey) {
        log.info("[MOCK-TOSS] queryOrder idem={}", SensitiveDataMasker.maskToken(idempotencyKey));
        Filling f = received.get(idempotencyKey);
        if (f == null) {
            return Optional.empty();
        }
        // 조회할 때마다 다음 청크를 누적 체결한다(부분 체결 진행 모사).
        if (partialFillChunk > 0 && f.filled < f.requested) {
            f.filled = Math.min(f.requested, f.filled + partialFillChunk);
        }
        return Optional.of(f.toResponse());
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
