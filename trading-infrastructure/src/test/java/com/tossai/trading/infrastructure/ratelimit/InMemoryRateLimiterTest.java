package com.tossai.trading.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 인메모리 레이트리미터/분산락 단위 검증(Docker 불필요). */
class InMemoryRateLimiterTest {

    @Test
    void rateLimit_allowsUpToLimitThenDenies() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        assertTrue(limiter.tryAcquire("k", 3, 60));
        assertTrue(limiter.tryAcquire("k", 3, 60));
        assertTrue(limiter.tryAcquire("k", 3, 60));
        assertFalse(limiter.tryAcquire("k", 3, 60), "한도 초과 시 거부");
    }

    @Test
    void registerOnce_detectsDuplicate() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        assertTrue(limiter.registerOnce("idem-1", 60));
        assertFalse(limiter.registerOnce("idem-1", 60), "동일 키는 중복");
        assertTrue(limiter.registerOnce("idem-2", 60));
    }

    @Test
    void lock_exclusiveUntilUnlocked() {
        InMemoryDistributedLock lock = new InMemoryDistributedLock();
        Optional<String> t1 = lock.tryLock("job", 30);
        assertTrue(t1.isPresent());
        assertTrue(lock.tryLock("job", 30).isEmpty(), "보유 중에는 재획득 불가");
        lock.unlock("job", t1.get());
        assertTrue(lock.tryLock("job", 30).isPresent(), "해제 후 재획득 가능");
    }
}
