package com.example.tournaments_backend.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Sliding-window-counter rate limiter backed by Redis.
 *
 * <p>Window math is done here against an injectable {@link Clock} (deterministic
 * in tests); the increment-and-decide step runs as one atomic Lua script so there
 * is no read-decide-write race. A Redis failure is translated to
 * {@link RateLimitUnavailableException} to signal the fail-closed stance.
 */
@Component
@SuppressWarnings("rawtypes")
public class SlidingWindowRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> script;
    private final Clock clock;
    private final RateLimitProperties properties;

    public SlidingWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            RedisScript<List> script,
            Clock clock,
            RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.script = script;
        this.clock = clock;
        this.properties = properties;
    }

    public Decision check(String clientIp) {
        long windowMillis = properties.getWindowSeconds() * 1000L;
        long now = clock.millis();
        long windowNumber = now / windowMillis;
        long elapsedInWindow = now % windowMillis;
        double weight = 1.0 - ((double) elapsedInWindow / windowMillis);

        String currentKey = KEY_PREFIX + clientIp + ":" + windowNumber;
        String previousKey = KEY_PREFIX + clientIp + ":" + (windowNumber - 1);
        long limit = properties.getRequests();
        long ttlSeconds = 2L * properties.getWindowSeconds();

        List<?> result;
        try {
            result = redisTemplate.execute(
                    script,
                    List.of(currentKey, previousKey),
                    String.valueOf(limit),
                    String.valueOf(weight),
                    String.valueOf(ttlSeconds));
        } catch (DataAccessException | io.lettuce.core.RedisException ex) {
            throw new RateLimitUnavailableException("Rate limiter Redis store is unavailable", ex);
        }

        if (result == null || result.size() < 2) {
            throw new RateLimitUnavailableException(
                    "Rate limiter script returned no result", null);
        }

        boolean allowed = ((Number) result.get(0)).longValue() == 1L;
        long estimate = ((Number) result.get(1)).longValue();
        long remaining = Math.max(0L, limit - estimate);
        long resetSeconds = (long) Math.ceil((windowMillis - elapsedInWindow) / 1000.0);
        long retryAfterSeconds = Math.max(1L, resetSeconds);

        return new Decision(allowed, limit, remaining, retryAfterSeconds, resetSeconds);
    }
}
