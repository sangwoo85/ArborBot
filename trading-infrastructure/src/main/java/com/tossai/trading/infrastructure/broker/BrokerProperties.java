package com.tossai.trading.infrastructure.broker;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 브로커(증권사) 연동 설정.
 *
 * <p><b>중요</b>: 엔드포인트 경로·인증·요청/응답 필드명은 <b>모두 설정값으로 주입</b>한다.
 * 코드에 특정 증권사의 실제 스펙을 추측해 하드코딩하지 않는다(절대 규칙 6).
 * 토스증권/한국투자증권 등 <b>공식 문서로 스펙이 확인되면 이 설정값만 채워</b> 사용한다.
 *
 * <p>비밀정보(appKey/appSecret/accountNo)는 환경변수로만 주입하며 로그·응답에 노출하지 않는다.
 */
@ConfigurationProperties(prefix = "trading.broker")
public class BrokerProperties {

    /** mock(기본, 시뮬레이션) | rest(실 HTTP 연동). */
    private String provider = "mock";

    private String baseUrl = "";
    private String tokenUrl = "";
    private String appKey = "";
    private String appSecret = "";
    private String accountNo = "";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private String idempotencyHeader = "Idempotency-Key";

    private final Paths paths = new Paths();
    private final RequestFields request = new RequestFields();
    private final ResponseFields response = new ResponseFields();
    private final TokenFields token = new TokenFields();

    /** 엔드포인트 경로(공식 문서 확인 후 채움). */
    public static class Paths {
        private String order = "";
        private String cancel = "";
        private String query = "";
        public String getOrder() { return order; }
        public void setOrder(String v) { this.order = v; }
        public String getCancel() { return cancel; }
        public void setCancel(String v) { this.cancel = v; }
        public String getQuery() { return query; }
        public void setQuery(String v) { this.query = v; }
    }

    /** 주문 요청 JSON 키 이름(스펙에 맞춰 매핑). */
    public static class RequestFields {
        private String symbol = "symbol";
        private String side = "side";
        private String quantity = "quantity";
        private String price = "price";
        private String orderType = "orderType";
        private String account = "accountNo";
        public String getSymbol() { return symbol; }
        public void setSymbol(String v) { this.symbol = v; }
        public String getSide() { return side; }
        public void setSide(String v) { this.side = v; }
        public String getQuantity() { return quantity; }
        public void setQuantity(String v) { this.quantity = v; }
        public String getPrice() { return price; }
        public void setPrice(String v) { this.price = v; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String v) { this.orderType = v; }
        public String getAccount() { return account; }
        public void setAccount(String v) { this.account = v; }
    }

    /** 주문 응답 JSON 키 이름 + 성공 판정값. */
    public static class ResponseFields {
        private String orderRef = "orderId";
        private String filledQuantity = "filledQuantity";
        private String avgPrice = "avgPrice";
        private String resultCode = "resultCode";
        private String successCode = "0";
        public String getOrderRef() { return orderRef; }
        public void setOrderRef(String v) { this.orderRef = v; }
        public String getFilledQuantity() { return filledQuantity; }
        public void setFilledQuantity(String v) { this.filledQuantity = v; }
        public String getAvgPrice() { return avgPrice; }
        public void setAvgPrice(String v) { this.avgPrice = v; }
        public String getResultCode() { return resultCode; }
        public void setResultCode(String v) { this.resultCode = v; }
        public String getSuccessCode() { return successCode; }
        public void setSuccessCode(String v) { this.successCode = v; }
    }

    /** 토큰 발급 요청/응답 키. */
    public static class TokenFields {
        private String grantTypeKey = "grant_type";
        private String grantTypeValue = "client_credentials";
        private String appKeyKey = "appkey";
        private String appSecretKey = "appsecret";
        private String accessTokenKey = "access_token";
        private String expiresInKey = "expires_in";
        public String getGrantTypeKey() { return grantTypeKey; }
        public void setGrantTypeKey(String v) { this.grantTypeKey = v; }
        public String getGrantTypeValue() { return grantTypeValue; }
        public void setGrantTypeValue(String v) { this.grantTypeValue = v; }
        public String getAppKeyKey() { return appKeyKey; }
        public void setAppKeyKey(String v) { this.appKeyKey = v; }
        public String getAppSecretKey() { return appSecretKey; }
        public void setAppSecretKey(String v) { this.appSecretKey = v; }
        public String getAccessTokenKey() { return accessTokenKey; }
        public void setAccessTokenKey(String v) { this.accessTokenKey = v; }
        public String getExpiresInKey() { return expiresInKey; }
        public void setExpiresInKey(String v) { this.expiresInKey = v; }
    }

    public String getProvider() { return provider; }
    public void setProvider(String v) { this.provider = v; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { this.baseUrl = v; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String v) { this.tokenUrl = v; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String v) { this.appKey = v; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String v) { this.appSecret = v; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String v) { this.accountNo = v; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int v) { this.connectTimeoutMs = v; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int v) { this.readTimeoutMs = v; }
    public String getIdempotencyHeader() { return idempotencyHeader; }
    public void setIdempotencyHeader(String v) { this.idempotencyHeader = v; }
    public Paths getPaths() { return paths; }
    public RequestFields getRequest() { return request; }
    public ResponseFields getResponse() { return response; }
    public TokenFields getToken() { return token; }
}
