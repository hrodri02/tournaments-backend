package com.example.tournaments_backend.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Wiring for the rate limiter: the authoritative {@link Clock} used for window math
 * and the Lua {@link RedisScript} that performs the atomic increment-and-decide step.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public Clock rateLimitClock() {
        return Clock.systemUTC();
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public RedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("sliding_window.lua"));
        script.setResultType(List.class);
        return script;
    }

    /**
     * Prevent Spring Boot from auto-registering {@link RateLimitFilter} as a
     * servlet-level filter. It is added explicitly inside the Spring Security
     * chain (via {@code addFilterBefore}); this keeps it from running twice.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
