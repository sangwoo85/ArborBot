package com.tossai.trading.infrastructure.marketdata;

import com.tossai.trading.domain.market.Instrument;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 실 시세 수집 어댑터의 HTTP 배관 검증(실제 호출 없이 MockRestServiceServer).
 * 인증 헤더 → 응답 필드 매핑(가격/거래정지/유동성) 확인.
 */
class RestMarketDataAdapterTest {

    private MarketDataProperties props() {
        MarketDataProperties p = new MarketDataProperties();
        p.setProvider("rest");
        p.setBaseUrl("http://md.test");
        p.setInstrumentPath("/v1/quote");
        p.setApiKeyHeader("X-API-KEY");
        p.setApiKey("mdkey");
        return p;
    }

    @Test
    void getInstrument_mapsPriceAndStatus() {
        MarketDataProperties props = props();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://md.test/v1/quote?symbol=005930"))
                .andExpect(header("X-API-KEY", "mdkey"))
                .andRespond(withSuccess(
                        "{\"lastPrice\":71500,\"sector\":\"SEMICONDUCTOR\",\"tradable\":true,\"halted\":false,\"illiquid\":false}",
                        MediaType.APPLICATION_JSON));

        RestMarketDataAdapter adapter = new RestMarketDataAdapter(builder, props);
        Optional<Instrument> inst = adapter.getInstrument("005930");

        assertTrue(inst.isPresent());
        assertEquals(0, new java.math.BigDecimal("71500").compareTo(inst.get().lastPrice()));
        assertEquals("SEMICONDUCTOR", inst.get().sector());
        assertFalse(inst.get().blocksNewEntry());
        server.verify();
    }

    @Test
    void getInstrument_haltedFlagParsed() {
        MarketDataProperties props = props();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://md.test/v1/quote?symbol=900110"))
                .andRespond(withSuccess(
                        "{\"lastPrice\":1500,\"sector\":\"MISC\",\"tradable\":false,\"halted\":true,\"illiquid\":false}",
                        MediaType.APPLICATION_JSON));

        RestMarketDataAdapter adapter = new RestMarketDataAdapter(builder, props);
        Instrument inst = adapter.getInstrument("900110").orElseThrow();

        assertTrue(inst.halted());
        assertTrue(inst.blocksNewEntry());   // 거래정지 → 신규 진입 차단
        server.verify();
    }
}
