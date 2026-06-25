package com.tossai.trading.infrastructure.ratelimit;

import com.tossai.trading.application.port.out.DistributedLockPort;
import com.tossai.trading.common.util.Ids;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis 기반 분산 락(SET NX PX + Lua compare-del). trading.redis.enabled=true 일 때만 활성화.
 */
@Component
@ConditionalOnProperty(prefix = "trading.redis", name = "enabled", havingValue = "true")
public class RedisDistributedLock implements DistributedLockPort {

    /** 토큰이 일치할 때만 삭제(소유자만 해제) — 원자적. */
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisDistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> tryLock(String key, int ttlSeconds) {
        String token = Ids.newId();
        Boolean ok = redis.opsForValue()
                .setIfAbsent("lock:" + key, token, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(ok) ? Optional.of(token) : Optional.empty();
    }

    @Override
    public void unlock(String key, String token) {
        redis.execute(UNLOCK, List.of("lock:" + key), token);
    }
}
