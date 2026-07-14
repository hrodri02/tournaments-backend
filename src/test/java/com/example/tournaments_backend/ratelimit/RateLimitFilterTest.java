package com.example.tournaments_backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;

class RateLimitFilterTest {

    private SlidingWindowRateLimiter limiter;
    private ClientIpResolver ipResolver;
    private RateLimitProperties properties;
    private RateLimitFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        limiter = mock(SlidingWindowRateLimiter.class);
        ipResolver = mock(ClientIpResolver.class);
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new RateLimitFilter(limiter, ipResolver, properties, objectMapper);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);

        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
    }

    @Test
    void allowsRequest_setsHeaders_andProceedsDownChain() throws Exception {
        when(limiter.check("1.2.3.4")).thenReturn(new Decision(true, 10, 7, 1, 1));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("7");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1");
        assertThat(response.getHeader("Retry-After")).isNull();
    }

    @Test
    void throttledRequest_returns429_withErrorBody_andRetryAfter() throws Exception {
        when(limiter.check("1.2.3.4")).thenReturn(new Decision(false, 10, 0, 3, 3));

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains(ClientErrorKey.RATE_LIMIT_EXCEEDED.name());
        assertThat(response.getHeader("Retry-After")).isEqualTo("3");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void redisUnavailable_returns503_withErrorBody() throws Exception {
        when(limiter.check("1.2.3.4"))
                .thenThrow(new RateLimitUnavailableException("down", null));

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(ClientErrorKey.RATE_LIMIT_UNAVAILABLE.name());
    }

    @Test
    void disabled_passesThrough_withoutTouchingRedis() throws Exception {
        properties.setEnabled(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(limiter);
        assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
    }
}
