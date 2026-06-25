package com.tossai.trading.application.port.out;

import java.util.Optional;

/**
 * 분산 락 포트. 다중 인스턴스에서 단일 작업(예: Outbox 디스패치)만 수행되도록 보장.
 * 기본 구현은 인메모리(단일 인스턴스 가정), Redis 구현은 SET NX PX 기반.
 */
public interface DistributedLockPort {

    /** 락 획득 시 해제 토큰 반환, 실패 시 빈 값. ttl 후 자동 만료(데드락 방지). */
    Optional<String> tryLock(String key, int ttlSeconds);

    /** 토큰이 일치할 때만 해제(소유자만 해제). */
    void unlock(String key, String token);
}
