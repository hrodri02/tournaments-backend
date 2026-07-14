package com.example.tournaments_backend.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the per-IP request rate limiter, bound from {@code ratelimit.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /** Whether the rate limiter is active. When {@code false} the filter passes every request through. */
    private boolean enabled = true;

    /** Maximum number of requests allowed per client IP within one window. */
    private int requests = 10;

    /** Length of the sliding window in seconds. */
    private int windowSeconds = 1;
}
