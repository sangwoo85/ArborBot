package com.tossai.trading.infrastructure.ratelimit;

import com.tossai.trading.application.port.out.RateLimiterPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 레이트리미터(다중 인스턴스 원자적). 고정 윈도 INCR+EXPIRE.
 * trading.redis.enabled=true 일 때만 활성화.
 */
@Component
@ConditionalOnProperty(prefix = "trading.redis", name = "enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiterPort {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String k = "rl:" + key;
        Long count = redis.opsForValue().increment(k);
        if (count != null && count == 1L) {
            redis.expire(k, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= limit;
    }

    @Override
    public boolean registerOnce(String key, int ttlSeconds) {
        Boolean set = redis.opsForValue()
                .setIfAbsent("once:" + key, "1", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(set);
    }
}
