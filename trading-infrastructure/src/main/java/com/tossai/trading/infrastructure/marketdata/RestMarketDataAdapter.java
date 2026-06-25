package com.tossai.trading.infrastructure.marketdata;

import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.domain.market.Instrument;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * 실 시세 수집 어댑터. <b>trading.marketdata.provider=rest</b> 일 때만 활성화(기본은 Mock).
 * 엔드포인트/인증/필드명을 {@link MarketDataProperties} 설정으로 주입받는다(스펙 하드코딩 금지).
 */
@Component
@ConditionalOnProperty(prefix = "trading.marketdata", name = "provider", havingValue = "rest")
public class RestMarketDataAdapter implements MarketDataPort {

    private static final Logger log = LoggerFactory.getLogger(RestMarketDataAdapter.class);

    private final RestClient client;
    private final MarketDataProperties props;

    public RestMarketDataAdapter(RestClient.Builder builder, MarketDataProperties props) {
        this.client = builder.build();
        this.props = props;
    }

    @Override
    @CircuitBreaker(name = "marketdata")
    @Retry(name = "marketdata")
    public Optional<Instrument> getInstrument(String symbol) {
        try {
            RestClient.RequestHeadersSpec<?> req = client.get()
                    .uri(props.getBaseUrl() + props.getInstrumentPath() + "?symbol=" + symbol);
            if (!props.getApiKeyHeader().isBlank()) {
                req = req.header(props.getApiKeyHeader(), props.getApiKey());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = req.retrieve().body(Map.class);
            if (resp == null || resp.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parse(symbol, resp));
        } catch (RestClientResponseException e) {
            log.warn("[REST-MD] getInstrument {} 실패 status={}", symbol, e.getStatusCode().value());
            return Optional.empty();
        }
    }

    @Override
    public Optional<BigDecimal> lastPrice(String symbol) {
        return getInstrument(symbol).map(Instrument::lastPrice);
    }

    private Instrument parse(String symbol, Map<String, Object> r) {
        BigDecimal price = parseBigDecimal(r.get(props.getFields().getPrice()));
        String sector = str(r.get(props.getFields().getSector()), "UNKNOWN");
        boolean tradable = parseBool(r.get(props.getFields().getTradable()), true);
        boolean halted = parseBool(r.get(props.getFields().getHalted()), false);
        boolean illiquid = parseBool(r.get(props.getFields().getIlliquid()), false);
        return new Instrument(symbol, sector, price, tradable, halted, illiquid);
    }

    private String str(Object o, String def) {
        return o == null ? def : o.toString();
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

    private boolean parseBool(Object o, boolean def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = o.toString().trim();
        return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("Y");
    }
}
