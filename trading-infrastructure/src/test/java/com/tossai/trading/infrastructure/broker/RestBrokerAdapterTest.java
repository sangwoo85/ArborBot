package com.tossai.trading.infrastructure.broker;

import com.tossai.trading.domain.broker.BrokerOrderRequest;
import com.tossai.trading.domain.broker.BrokerOrderResponse;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.order.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 실 REST 브로커 어댑터의 HTTP 배관 검증(실제 증권사 호출 없이 MockRestServiceServer 사용).
 * 토큰 발급 → Authorization/Idempotency 헤더 → 요청 본문 → 응답 매핑 → 4xx 거부 처리를 확인한다.
 */
class RestBrokerAdapterTest {

    private BrokerProperties props() {
        BrokerProperties p = new BrokerProperties();
        p.setProvider("rest");
        p.setBaseUrl("http://broker.test");
        p.setTokenUrl("http://broker.test/oauth/token");
        p.setAppKey("key"); p.setAppSecret("secret"); p.setAccountNo("ACC-1");
        p.getPaths().setOrder("/v1/orders");
        return p;
    }

    @Test
    void placeOrder_success_mapsResponse() {
        BrokerProperties props = props();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://broker.test/oauth/token")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok123\",\"expires_in\":600}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://broker.test/v1/orders")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok123"))
                .andExpect(header("Idempotency-Key", "idem-1"))
                .andRespond(withSuccess(
                        "{\"resultCode\":\"0\",\"orderId\":\"B123\",\"filledQuantity\":10,\"avgPrice\":71000}",
                        MediaType.APPLICATION_JSON));

        RestBrokerAdapter adapter = new RestBrokerAdapter(builder, props);
        BrokerOrderResponse resp = adapter.placeOrder(new BrokerOrderRequest(
                "idem-1", "005930", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("71000")));

        assertTrue(resp.accepted());
        assertEquals("B123", resp.brokerOrderRef());
        assertEquals(10, resp.filledQuantity());
        assertEquals(0, new BigDecimal("71000").compareTo(resp.avgFillPrice()));
        server.verify();
    }

    @Test
    void placeOrder_4xx_returnsRejected() {
        BrokerProperties props = props();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://broker.test/oauth/token")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok123\",\"expires_in\":600}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://broker.test/v1/orders")).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        RestBrokerAdapter adapter = new RestBrokerAdapter(builder, props);
        BrokerOrderResponse resp = adapter.placeOrder(new BrokerOrderRequest(
                "idem-2", "005930", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("71000")));

        assertFalse(resp.accepted());   // 4xx = 확정 거부
        server.verify();
    }
}
