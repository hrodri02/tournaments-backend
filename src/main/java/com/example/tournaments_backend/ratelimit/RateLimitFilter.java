package com.example.tournaments_backend.ratelimit;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-IP rate-limiting filter. Runs before authentication (registered via
 * {@code addFilterBefore} in the security chain), so it covers every route.
 *
 * <p>Extends {@link OncePerRequestFilter} so the check — and its Redis INCR —
 * runs exactly once per client request even across FORWARD/ASYNC re-dispatches.
 * Because it runs before {@code @RestControllerAdvice}, it serializes the
 * {@link ErrorDetails} body itself with the injected {@link ObjectMapper}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    private final SlidingWindowRateLimiter limiter;
    private final ClientIpResolver ipResolver;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            SlidingWindowRateLimiter limiter,
            ClientIpResolver ipResolver,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.ipResolver = ipResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ipResolver.resolve(request);

        Decision decision;
        try {
            decision = limiter.check(clientIp);
        } catch (RateLimitUnavailableException ex) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, ClientErrorKey.RATE_LIMIT_UNAVAILABLE);
            return;
        }

        setRateLimitHeaders(response, decision);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
        } else {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, ClientErrorKey.RATE_LIMIT_EXCEEDED);
        }
    }

    private void setRateLimitHeaders(HttpServletResponse response, Decision decision) {
        response.setHeader(HEADER_LIMIT, String.valueOf(decision.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(decision.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(decision.resetSeconds()));
    }

    private void writeError(HttpServletResponse response, HttpStatus status, ClientErrorKey errorKey)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorDetails body = new ErrorDetails(status, errorKey.name(), LocalDateTime.now());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
