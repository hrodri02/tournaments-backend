package com.example.tournaments_backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the real Lua sliding-window behavior against a Redis container,
 * plus the fail-closed path when Redis is unreachable.
 */
@Testcontainers
@SuppressWarnings({ "rawtypes", "resource" })
class SlidingWindowRateLimiterIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static final RedisScript<List> SCRIPT = buildScript();

    private LettuceConnectionFactory connectionFactory;
    private SlidingWindowRateLimiter limiter;

    private static RedisScript<List> buildScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("sliding_window.lua"));
        script.setResultType(List.class);
        return script;
    }

    private static RateLimitProperties props(int requests, int windowSeconds) {
        RateLimitProperties p = new RateLimitProperties();
        p.setEnabled(true);
        p.setRequests(requests);
        p.setWindowSeconds(windowSeconds);
        return p;
    }

    private SlidingWindowRateLimiter limiterFor(LettuceConnectionFactory factory, RateLimitProperties props) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return new SlidingWindowRateLimiter(template, SCRIPT, Clock.systemUTC(), props);
    }

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        // Large window so all requests in a test fall inside one window (no rollover).
        limiter = limiterFor(connectionFactory, props(5, 100));
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void allowsUpToLimit_thenThrottles() {
        String ip = "203.0.113.9";

        for (int i = 1; i <= 5; i++) {
            Decision decision = limiter.check(ip);
            assertThat(decision.allowed())
                    .as("request #%d should be allowed", i)
                    .isTrue();
            assertThat(decision.remaining()).isEqualTo(5 - i);
        }

        Decision overLimit = limiter.check(ip);
        assertThat(overLimit.allowed()).isFalse();
        assertThat(overLimit.remaining()).isZero();
        assertThat(overLimit.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void separateIps_haveIndependentBuckets() {
        for (int i = 0; i < 5; i++) {
            limiter.check("198.51.100.1");
        }
        // A different IP starts fresh and is allowed.
        assertThat(limiter.check("198.51.100.2").allowed()).isTrue();
    }

    @Test
    void failsClosed_whenRedisUnreachable() {
        LettuceConnectionFactory deadFactory = new LettuceConnectionFactory("localhost", 63999);
        deadFactory.afterPropertiesSet();
        try {
            SlidingWindowRateLimiter deadLimiter = limiterFor(deadFactory, props(5, 100));
            assertThatThrownBy(() -> deadLimiter.check("203.0.113.9"))
                    .isInstanceOf(RateLimitUnavailableException.class);
        } finally {
            deadFactory.destroy();
        }
    }
}
