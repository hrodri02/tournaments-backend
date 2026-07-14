package com.example.tournaments_backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({ "unchecked", "rawtypes" })
class SlidingWindowRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private RedisScript<List> script;
    private SlidingWindowRateLimiter limiter;

    // Fixed at 10_500ms → windowSize 1000ms → window number 10, 500ms elapsed, weight 0.5.
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.ofEpochMilli(10_500L), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        script = org.mockito.Mockito.mock(RedisScript.class);
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setRequests(10);
        props.setWindowSeconds(1);
        limiter = new SlidingWindowRateLimiter(redisTemplate, script, FIXED_CLOCK, props);
    }

    @Test
    void buildsCorrectKeysAndArgs_andReturnsAllowedDecision() {
        when(redisTemplate.execute(same(script), anyList(), any(), any(), any()))
                .thenReturn(List.of(1L, 3L));

        Decision decision = limiter.check("1.2.3.4");

        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(redisTemplate).execute(
                same(script), keysCaptor.capture(),
                argsCaptor.capture(), argsCaptor.capture(), argsCaptor.capture());

        // window number = 10_500 / 1000 = 10; previous = 9
        assertThat(keysCaptor.getValue())
                .containsExactly("ratelimit:1.2.3.4:10", "ratelimit:1.2.3.4:9");
        // args: limit, weight (1 - 500/1000 = 0.5), ttl (2 * windowSeconds = 2)
        assertThat(argsCaptor.getAllValues())
                .containsExactly("10", "0.5", "2");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(10);
        assertThat(decision.remaining()).isEqualTo(7); // max(0, 10 - estimate(3))
        assertThat(decision.resetSeconds()).isEqualTo(1); // ceil((1000 - 500)/1000)
        assertThat(decision.retryAfterSeconds()).isEqualTo(1); // min 1
    }

    @Test
    void deniesAndClampsRemainingToZero_whenEstimateExceedsLimit() {
        when(redisTemplate.execute(same(script), anyList(), any(), any(), any()))
                .thenReturn(List.of(0L, 13L));

        Decision decision = limiter.check("1.2.3.4");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero(); // max(0, 10 - 13)
    }

    @Test
    void translatesRedisFailureToRateLimitUnavailable() {
        when(redisTemplate.execute(same(script), anyList(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThatThrownBy(() -> limiter.check("1.2.3.4"))
                .isInstanceOf(RateLimitUnavailableException.class);
    }
}
