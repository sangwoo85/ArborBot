package com.tossai.trading.infrastructure.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시장 데이터 연동 설정.
 *
 * <p>브로커와 동일 원칙: 엔드포인트·인증·필드명을 모두 설정으로 주입하며 특정 시세 API 스펙을
 * 코드에 추측해 하드코딩하지 않는다(절대 규칙 6). 공식 스펙 확인 후 설정값만 채워 사용한다.
 */
@ConfigurationProperties(prefix = "trading.marketdata")
public class MarketDataProperties {

    /** mock(기본, instrument 시드) | rest(실 HTTP 수집). */
    private String provider = "mock";

    private String baseUrl = "";
    private String instrumentPath = "";   // 예: /v1/quote
    private String apiKeyHeader = "";      // 인증 헤더명(비우면 미사용)
    private String apiKey = "";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;

    private final Fields fields = new Fields();

    /** 응답 JSON 키 이름(스펙에 맞춰 매핑). */
    public static class Fields {
        private String price = "lastPrice";
        private String sector = "sector";
        private String tradable = "tradable";
        private String halted = "halted";
        private String illiquid = "illiquid";
        public String getPrice() { return price; }
        public void setPrice(String v) { this.price = v; }
        public String getSector() { return sector; }
        public void setSector(String v) { this.sector = v; }
        public String getTradable() { return tradable; }
        public void setTradable(String v) { this.tradable = v; }
        public String getHalted() { return halted; }
        public void setHalted(String v) { this.halted = v; }
        public String getIlliquid() { return illiquid; }
        public void setIlliquid(String v) { this.illiquid = v; }
    }

    public String getProvider() { return provider; }
    public void setProvider(String v) { this.provider = v; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { this.baseUrl = v; }
    public String getInstrumentPath() { return instrumentPath; }
    public void setInstrumentPath(String v) { this.instrumentPath = v; }
    public String getApiKeyHeader() { return apiKeyHeader; }
    public void setApiKeyHeader(String v) { this.apiKeyHeader = v; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { this.apiKey = v; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int v) { this.connectTimeoutMs = v; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int v) { this.readTimeoutMs = v; }
    public Fields getFields() { return fields; }
}
