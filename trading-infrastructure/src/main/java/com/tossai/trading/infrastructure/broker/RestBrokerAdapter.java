package com.tossai.trading.infrastructure.broker;

import com.tossai.trading.application.port.out.BrokerPort;
import com.tossai.trading.common.util.SensitiveDataMasker;
import com.tossai.trading.domain.broker.BrokerOrderRequest;
import com.tossai.trading.domain.broker.BrokerOrderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 실 증권사 REST 연동 어댑터. <b>provider=rest</b> 일 때만 활성화된다(기본은 Mock).
 *
 * <p>엔드포인트/인증/필드명을 모두 {@link BrokerProperties} 설정으로 주입받으므로,
 * 특정 증권사의 실제 스펙을 코드에 하드코딩하지 않는다(절대 규칙 6).
 * 토스증권/한국투자증권 등 <b>공식 문서로 스펙이 확인되면 설정값(.env/yml)만 채워</b> 사용한다.
 *
 * <p>외부 호출에 timeout(RestClient) + retry/circuit breaker(Resilience4j) + 멱등키 헤더를 적용하고,
 * 토큰/계좌 등 비밀정보는 로그에 마스킹한다.
 */
@Component
@ConditionalOnProperty(prefix = "trading.broker", name = "provider", havingValue = "rest")
public class RestBrokerAdapter implements BrokerPort {

    private static final Logger log = LoggerFactory.getLogger(RestBrokerAdapter.class);

    private final RestClient client;
    private final BrokerProperties props;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public RestBrokerAdapter(RestClient.Builder builder, BrokerProperties props) {
        this.client = builder.build();
        this.props = props;
    }

    @Override
    @CircuitBreaker(name = "broker")
    @Retry(name = "broker")
    public BrokerOrderResponse placeOrder(BrokerOrderRequest request) {
        log.info("[REST-BROKER] placeOrder symbol={} side={} qty={} idem={}",
                request.symbol(), request.side(), request.quantity(),
                SensitiveDataMasker.maskToken(request.idempotencyKey()));
        try {
            Map<String, Object> body = buildOrderBody(request);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = client.post()
                    .uri(props.getBaseUrl() + props.getPaths().getOrder())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .header(props.getIdempotencyHeader(), request.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return parseOrderResponse(resp);
        } catch (RestClientResponseException e) {
            // 4xx = 비즈니스 거부(확정) / 그 외(5xx·IO)는 결과 불명 → 상위에서 queryOrder 재동기화
            if (e.getStatusCode().is4xxClientError()) {
                return BrokerOrderResponse.rejected(String.valueOf(e.getStatusCode().value()),
                        "broker 4xx");
            }
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "broker")
    @Retry(name = "broker")
    public BrokerOrderResponse cancelOrder(String brokerOrderRef, String idempotencyKey) {
        log.info("[REST-BROKER] cancelOrder ref={} idem={}", brokerOrderRef,
                SensitiveDataMasker.maskToken(idempotencyKey));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(props.getResponse().getOrderRef(), brokerOrderRef);
        body.put(props.getRequest().getAccount(), props.getAccountNo());
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = client.post()
                .uri(props.getBaseUrl() + props.getPaths().getCancel())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .header(props.getIdempotencyHeader(), idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        return parseOrderResponse(resp);
    }

    @Override
    public Optional<BrokerOrderResponse> queryOrder(String idempotencyKey) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = client.get()
                    .uri(props.getBaseUrl() + props.getPaths().getQuery() + "?idempotencyKey=" + idempotencyKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .retrieve()
                    .body(Map.class);
            if (resp == null || resp.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parseOrderResponse(resp));
        } catch (RestClientResponseException e) {
            return Optional.empty();   // 미존재/오류 → 미확인 처리(상위에서 보수적 복구)
        }
    }

    // ---- 내부: 토큰/요청·응답 매핑 (필드명은 모두 설정 주입) ----

    /** 토큰 발급(클라이언트 자격증명). 만료 전까지 캐시. */
    private synchronized String token() {
        if (props.getTokenUrl() == null || props.getTokenUrl().isBlank()) {
            return "";   // 토큰 미사용 환경
        }
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put(props.getToken().getGrantTypeKey(), props.getToken().getGrantTypeValue());
        req.put(props.getToken().getAppKeyKey(), props.getAppKey());
        req.put(props.getToken().getAppSecretKey(), props.getAppSecret());
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = client.post()
                .uri(props.getTokenUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(Map.class);
        Object t = resp == null ? null : resp.get(props.getToken().getAccessTokenKey());
        long expiresIn = parseLong(resp == null ? null : resp.get(props.getToken().getExpiresInKey()), 600);
        cachedToken = t == null ? "" : t.toString();
        tokenExpiry = Instant.now().plusSeconds(Math.max(60, expiresIn - 30));   // 만료 30s 전 회전
        return cachedToken;
    }

    private Map<String, Object> buildOrderBody(BrokerOrderRequest r) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(props.getRequest().getSymbol(), r.symbol());
        body.put(props.getRequest().getSide(), r.side().name());
        body.put(props.getRequest().getQuantity(), r.quantity());
        body.put(props.getRequest().getPrice(), r.limitPrice());
        body.put(props.getRequest().getOrderType(), r.orderType().name());
        body.put(props.getRequest().getAccount(), props.getAccountNo());
        return body;
    }

    private BrokerOrderResponse parseOrderResponse(Map<String, Object> resp) {
        if (resp == null) {
            return BrokerOrderResponse.rejected("EMPTY", "빈 응답");
        }
        Object rc = resp.get(props.getResponse().getResultCode());
        boolean accepted = (rc == null) || String.valueOf(rc).equals(props.getResponse().getSuccessCode());
        String orderRef = str(resp.get(props.getResponse().getOrderRef()));
        long filled = parseLong(resp.get(props.getResponse().getFilledQuantity()), 0);
        BigDecimal avg = parseBigDecimal(resp.get(props.getResponse().getAvgPrice()));
        return new BrokerOrderResponse(accepted, orderRef, filled, avg,
                rc == null ? "0" : String.valueOf(rc), accepted ? "OK" : "REJECTED");
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private long parseLong(Object o, long def) {
        if (o == null) {
            return def;
        }
        try {
            return (o instanceof Number n) ? n.longValue() : Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private BigDecimal parseBigDecimal(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
