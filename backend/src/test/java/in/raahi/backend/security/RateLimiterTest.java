package in.raahi.backend.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RateLimiterTest {

    @Test
    void testDegradedRateLimitingWhenRedisTemplateIsNull() {
        RateLimiter limiter = new RateLimiter(null);
        // Under limit should be allowed
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allow("degraded:test:1", 3, Duration.ofMinutes(1)));
        }
        // Exceeding limit must be blocked even when Redis is absent
        assertFalse(limiter.allow("degraded:test:1", 3, Duration.ofMinutes(1)));
    }

    @Test
    void testDegradedRateLimitingWhenRedisThrowsException() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        RateLimiter limiter = new RateLimiter(redis);
        // Under limit allowed
        for (int i = 0; i < 2; i++) {
            assertTrue(limiter.allow("outage:test:2", 2, Duration.ofMinutes(1)));
        }
        // Over limit blocked by degraded limiter
        assertFalse(limiter.allow("outage:test:2", 2, Duration.ofMinutes(1)));
    }

    @Test
    void testEmergencySosFailsOpenWhenRedisThrowsException() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        RateLimiter limiter = new RateLimiter(redis);
        // Life-safety SOS triggers must always fail open when Redis fails
        assertTrue(limiter.allowEmergency("sos:emergency:user1", 3, Duration.ofMinutes(1)));
    }

    @Test
    void testAllowsWhenCountUnderLimit() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any()))
                .thenReturn(3L);

        RateLimiter limiter = new RateLimiter(redis);
        assertTrue(limiter.allow("key:3", 5, Duration.ofMinutes(1)));
    }

    @Test
    void testRejectsWhenCountExceedsLimit() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any()))
                .thenReturn(6L);

        RateLimiter limiter = new RateLimiter(redis);
        assertFalse(limiter.allow("key:4", 5, Duration.ofMinutes(1)));
    }
}
