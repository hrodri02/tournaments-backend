package com.example.tournaments_backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void usesFirstXffEntry_whenHeaderPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void trimsToFirstEntry_whenXffHasMultipleValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", " 203.0.113.7 , 70.41.3.18 , 150.172.238.178 ");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToRemoteAddr_whenXffAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void fallsBackToRemoteAddr_whenXffBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "   ");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }
}
