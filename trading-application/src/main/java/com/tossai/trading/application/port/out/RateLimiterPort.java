package com.tossai.trading.application.port.out;

/**
 * 레이트리미트 포트. 주문 빈도 제한·중복 판정에 사용.
 * 기본 구현은 인메모리(단일 인스턴스), Redis 구현은 다중 인스턴스에서 원자적으로 동작.
 */
public interface RateLimiterPort {

    /**
     * 윈도 내 허용 한도를 넘지 않으면 1 소비하고 true, 넘으면 false.
     *
     * @param key           제한 키(예: "orders:rate")
     * @param limit         윈도당 최대 허용 횟수
     * @param windowSeconds 윈도 길이(초)
     */
    boolean tryAcquire(String key, int limit, int windowSeconds);

    /**
     * 멱등 등록: 키가 처음이면 true(등록), 이미 있으면 false(중복).
     *
     * @param key        멱등 키
     * @param ttlSeconds 보존 기간(초)
     */
    boolean registerOnce(String key, int ttlSeconds);
}
