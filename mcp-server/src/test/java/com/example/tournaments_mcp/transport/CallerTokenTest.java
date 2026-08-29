package com.example.tournaments_mcp.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.common.McpTransportContext;

class CallerTokenTest {

    private static McpTransportContext contextWith(String authorizationHeader) {
        return McpTransportContext.create(
                Map.of(McpTransportConfig.AUTHORIZATION_KEY, authorizationHeader));
    }

    @Test
    void from_ShouldKeepTheHeaderVerbatim_WhenItIsABearerToken() {
        CallerToken token = CallerToken.from(contextWith("Bearer abc.def.ghi"));

        assertThat(token.headerValue()).isEqualTo("Bearer abc.def.ghi");
    }

    @Test
    void from_ShouldAcceptAnyCasingOfTheScheme() {
        assertThat(CallerToken.from(contextWith("bearer abc.def.ghi")).headerValue())
                .isEqualTo("bearer abc.def.ghi");
    }

    @Test
    void from_ShouldExplainHowToConfigureAToken_WhenTheHeaderIsAbsent() {
        assertThatThrownBy(() -> CallerToken.from(McpTransportContext.EMPTY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no Authorization header")
                .hasMessageContaining("/api/v1/auth/login");
    }

    @Test
    void from_ShouldReject_WhenTheSchemeIsNotBearer() {
        assertThatThrownBy(() -> CallerToken.from(contextWith("Basic dXNlcjpwYXNz")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a bearer token");
    }

    @Test
    void from_ShouldReject_WhenTheSchemeCarriesNoToken() {
        assertThatThrownBy(() -> CallerToken.from(contextWith("Bearer   ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a bearer token");
    }
}
