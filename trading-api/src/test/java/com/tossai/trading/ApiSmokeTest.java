package com.tossai.trading;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 웹 계층 스모크 테스트: 컨텍스트 기동, actuator, 전략 목록 조회. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:trading_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class ApiSmokeTest {

    @Autowired TestRestTemplate rest;

    @Test
    void healthIsUp() {
        ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().contains("UP"));
    }

    @Test
    void strategiesAreSeeded() {
        ResponseEntity<String> res = rest.getForEntity("/api/v1/strategies", String.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().contains("momentum-v1"));
    }
}
