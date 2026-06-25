package com.tossai.trading.infrastructure.ratelimit;

import com.tossai.trading.application.port.out.RateLimiterPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 레이트리미터(단일 인스턴스 기본). 고정 윈도 카운터.
 * 다중 인스턴스에서는 Redis 구현(trading.redis.enabled=true)으로 교체한다.
 */
@Component
@ConditionalOnProperty(prefix = "trading.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiterPort {

    private static final class Window {
        long startEpochSec;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<String, Long> onceKeys = new ConcurrentHashMap<>();   // key -> expiryEpochSec

    @Override
    public synchronized boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        if (now - w.startEpochSec >= windowSeconds) {
            w.startEpochSec = now;
            w.count = 0;
        }
        if (w.count >= limit) {
            return false;
        }
        w.count++;
        return true;
    }

    @Override
    public synchronized boolean registerOnce(String key, int ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        Long expiry = onceKeys.get(key);
        if (expiry != null && expiry > now) {
            return false;   // 아직 유효 → 중복
        }
        onceKeys.put(key, now + ttlSeconds);
        return true;
    }
}
