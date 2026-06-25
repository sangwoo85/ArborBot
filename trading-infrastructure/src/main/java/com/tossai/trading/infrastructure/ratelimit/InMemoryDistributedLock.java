package com.tossai.trading.infrastructure.ratelimit;

import com.tossai.trading.application.port.out.DistributedLockPort;
import com.tossai.trading.common.util.Ids;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 분산 락(단일 인스턴스 기본). 다중 인스턴스에서는 Redis 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(prefix = "trading.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryDistributedLock implements DistributedLockPort {

    private record Held(String token, long expiryEpochMs) {
    }

    private final Map<String, Held> locks = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<String> tryLock(String key, int ttlSeconds) {
        long now = System.currentTimeMillis();
        Held held = locks.get(key);
        if (held != null && held.expiryEpochMs() > now) {
            return Optional.empty();
        }
        String token = Ids.newId();
        locks.put(key, new Held(token, now + ttlSeconds * 1000L));
        return Optional.of(token);
    }

    @Override
    public synchronized void unlock(String key, String token) {
        Held held = locks.get(key);
        if (held != null && held.token().equals(token)) {
            locks.remove(key);
        }
    }
}
