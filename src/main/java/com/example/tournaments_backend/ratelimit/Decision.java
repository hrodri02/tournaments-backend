package com.example.tournaments_backend.ratelimit;

/**
 * Outcome of a single rate-limit check, carrying everything the filter needs to
 * build its response headers.
 *
 * @param allowed           whether the request may proceed
 * @param limit             the configured request limit
 * @param remaining         requests left in the window ({@code max(0, limit - estimate)})
 * @param retryAfterSeconds seconds until the window rolls over (min 1); used for {@code Retry-After}
 * @param resetSeconds      seconds until the current window ends; used for {@code X-RateLimit-Reset}
 */
public record Decision(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        long resetSeconds) {
}
