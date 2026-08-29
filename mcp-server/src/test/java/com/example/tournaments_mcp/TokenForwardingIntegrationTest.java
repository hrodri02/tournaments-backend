package com.example.tournaments_mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Proves the caller's bearer token survives the whole path: MCP client -> HTTP
 * request -> transport context -> tool -> outbound REST call.
 *
 * <p>This is the test that fails if {@code McpTransportConfig} is ever removed
 * or its context extractor reverted to the SDK default, which discards every
 * header. Nothing else would notice: the application still starts, tools still
 * list, and the unit tests still pass because they build the transport context
 * by hand.
 *
 * <p>The backend is a recording stub rather than the real one -- what is under
 * test is the token's journey, not what the API does with it -- and it is a JDK
 * {@link HttpServer} rather than a mock-server library so the module keeps a
 * single test dependency.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenForwardingIntegrationTest {

    private static final List<String> AUTHORIZATIONS_SEEN_BY_BACKEND = new CopyOnWriteArrayList<>();
    private static final HttpServer STUB_BACKEND = startStubBackend();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void backendAddress(DynamicPropertyRegistry registry) {
        registry.add("tournaments.api.base-url",
                () -> "http://localhost:" + STUB_BACKEND.getAddress().getPort());
    }

    @BeforeEach
    void forgetPreviousRequests() {
        AUTHORIZATIONS_SEEN_BY_BACKEND.clear();
    }

    @AfterAll
    static void stopStubBackend() {
        STUB_BACKEND.stop(0);
    }

    @Test
    void toolCall_ShouldForwardTheCallersTokenToTheBackend() {
        String response = callListMyTeams("Bearer alice.token");

        assertThat(AUTHORIZATIONS_SEEN_BY_BACKEND).containsExactly("Bearer alice.token");
        assertThat(response).contains("Barcelona FC");
        assertThat(response).contains("\"isError\":false");
    }

    /**
     * Identity is per request, not per process. The stdio server could not do
     * this: it resolved one account at launch and answered as that account for
     * as long as it lived.
     */
    @Test
    void toolCall_ShouldUseADifferentToken_WhenADifferentCallerAsks() {
        callListMyTeams("Bearer alice.token");
        callListMyTeams("Bearer bob.token");

        assertThat(AUTHORIZATIONS_SEEN_BY_BACKEND)
                .containsExactly("Bearer alice.token", "Bearer bob.token");
    }

    @Test
    void toolCall_ShouldFailWithoutCallingTheBackend_WhenNoTokenIsSent() {
        String response = callListMyTeams(null);

        assertThat(AUTHORIZATIONS_SEEN_BY_BACKEND).isEmpty();
        assertThat(response).contains("\"isError\":true");
        assertThat(response).contains("no Authorization header");
    }

    private String callListMyTeams(String authorizationHeader) {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"jsonrpc":"2.0","id":1,"method":"tools/call",
                         "params":{"name":"list_my_teams","arguments":{}}}
                        """));
        if (authorizationHeader != null) {
            request.header("Authorization", authorizationHeader);
        }

        try {
            HttpResponse<String> response =
                    httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return response.body();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static HttpServer startStubBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/teams", TokenForwardingIntegrationTest::respondWithOneTeam);
            server.start();
            return server;
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not start the stub tournaments API", ex);
        }
    }

    private static void respondWithOneTeam(HttpExchange exchange) throws IOException {
        AUTHORIZATIONS_SEEN_BY_BACKEND.add(exchange.getRequestHeaders().getFirst("Authorization"));

        byte[] body = """
                {"teams":[{"id":1,"name":"Barcelona FC","ownerId":2,"playerDTOs":[]}],
                 "teamsInvitedTo":[]}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (exchange) {
            exchange.getResponseBody().write(body);
        }
    }
}
