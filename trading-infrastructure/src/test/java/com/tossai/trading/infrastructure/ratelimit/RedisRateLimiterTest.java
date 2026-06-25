package com.tossai.trading.infrastructure.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 레이트리미터/분산락 통합 검증. Docker 가 있을 때만 실행(disabledWithoutDocker).
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimiterTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;

    @BeforeAll
    static void start() {
        REDIS.start();
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void stop() {
        if (factory != null) {
            factory.destroy();
        }
        REDIS.stop();
    }

    @Test
    void redisRateLimit_allowsUpToLimitThenDenies() {
        RedisRateLimiter limiter = new RedisRateLimiter(redis);
        assertTrue(limiter.tryAcquire("orders", 2, 60));
        assertTrue(limiter.tryAcquire("orders", 2, 60));
        assertFalse(limiter.tryAcquire("orders", 2, 60), "한도 초과 시 거부");
    }

    @Test
    void redisLock_exclusiveAndReleasable() {
        RedisDistributedLock lock = new RedisDistributedLock(redis);
        Optional<String> t1 = lock.tryLock("dispatch", 30);
        assertTrue(t1.isPresent());
        assertTrue(lock.tryLock("dispatch", 30).isEmpty(), "보유 중 재획득 불가");
        lock.unlock("dispatch", t1.get());
        assertTrue(lock.tryLock("dispatch", 30).isPresent(), "해제 후 재획득 가능");
    }
}
