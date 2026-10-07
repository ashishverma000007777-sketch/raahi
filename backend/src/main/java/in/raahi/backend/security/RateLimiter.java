package in.raahi.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    // Fallback in-memory rate limiter when Redis is down or unconfigured
    private static class WindowCounter {
        final long windowStartEpochMs;
        final AtomicInteger count;

        WindowCounter(long windowStartEpochMs) {
            this.windowStartEpochMs = windowStartEpochMs;
            this.count = new AtomicInteger(1);
        }
    }

    private final ConcurrentHashMap<String, WindowCounter> fallbackStore = new ConcurrentHashMap<>();

    private static final String LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current";

    @Autowired
    public RateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    /**
     * Standard security-sensitive rate limiter.
     * Uses atomic Redis Lua script.
     * On Redis outage or missing Redis, degrades safely to in-memory window limiter
     * instead of turning into unlimited access.
     */
    public boolean allow(String key, int maxRequests, Duration window) {
        if (redisTemplate != null) {
            try {
                long windowSeconds = Math.max(1, window.toSeconds());
                List<String> keys = Collections.singletonList(key);
                Long count = redisTemplate.execute(rateLimitScript, keys, String.valueOf(windowSeconds));

                if (count != null) {
                    return count <= maxRequests;
                }
            } catch (Exception e) {
                log.warn("Redis rate limiter failed for key '{}': {}. Falling back to degraded in-memory limiter.",
                        key, e.getMessage());
            }
        }
        return allowDegraded(key, maxRequests, window);
    }

    /**
     * Special rate limiter for life-safety emergency SOS triggers.
     * On Redis outage, it logs a warning and fails open so an emergency alert is NEVER dropped
     * because of a Redis connectivity glitch.
     */
    public boolean allowEmergency(String key, int maxRequests, Duration window) {
        if (redisTemplate != null) {
            try {
                long windowSeconds = Math.max(1, window.toSeconds());
                List<String> keys = Collections.singletonList(key);
                Long count = redisTemplate.execute(rateLimitScript, keys, String.valueOf(windowSeconds));

                if (count != null) {
                    return count <= maxRequests;
                }
            } catch (Exception e) {
                log.warn("Redis rate limiter failed for emergency key '{}': {}. Failing open for SOS.",
                        key, e.getMessage());
                return true;
            }
        }
        return true;
    }

    private boolean allowDegraded(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1000, window.toMillis());

        // Simple cleanup if store grows excessively
        if (fallbackStore.size() > 10000) {
            fallbackStore.entrySet().removeIf(e -> now - e.getValue().windowStartEpochMs > windowMs * 2);
        }

        WindowCounter counter = fallbackStore.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStartEpochMs) > windowMs) {
                return new WindowCounter(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return counter.count.get() <= maxRequests;
    }
}
