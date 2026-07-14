package com.example.tournaments_backend.ratelimit;

/**
 * Raised when the rate limiter cannot reach its Redis backing store. The filter
 * maps this to HTTP 503 (RATE_LIMIT_UNAVAILABLE), keeping it distinct from a client
 * that has simply exceeded its limit (429). Signals the "fail closed" stance.
 */
public class RateLimitUnavailableException extends RuntimeException {

    public RateLimitUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
