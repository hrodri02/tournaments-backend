package com.example.tournaments_mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * What a client actually sees, asked over HTTP rather than through the beans.
 *
 * <p>The schemas in {@code ToolCatalog} are hand-written Java text blocks, so
 * two things can go wrong that unit tests cannot see: the JSON can be malformed
 * in a way that only surfaces when the server parses it at startup, and the
 * server has to be the one enforcing it for the schemas to be worth writing.
 * Both are assumptions this branch took on when it dropped Spring AI's
 * generated schemas, and both are checked here.
 *
 * <p>No backend is stubbed. Nothing asserted below should reach one -- if a
 * rejected argument still produced an outbound call, the test would hang or
 * fail on a connection refused rather than quietly passing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToolContractIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void toolsList_ShouldAdvertiseTheThreeReadTools() {
        String tools = post("""
                {"jsonrpc":"2.0","id":1,"method":"tools/list"}
                """);

        assertThat(tools).contains("\"list_leagues\"", "\"get_league\"", "\"list_my_teams\"");
    }

    /**
     * Proves the text blocks parsed into real schemas. A malformed or empty one
     * would leave {@code inputSchema} without these, and every tool would then
     * accept any arguments at all.
     */
    @Test
    void toolsList_ShouldCarryTheHandWrittenSchemas() {
        String tools = post("""
                {"jsonrpc":"2.0","id":1,"method":"tools/list"}
                """);

        assertThat(tools).contains("https://json-schema.org/draft/2020-12/schema");
        assertThat(tools).contains("\"required\":[\"leagueId\"]");
        assertThat(tools).contains("\"NOT_STARTED\",\"IN_PROGRESS\",\"ENDED\"");
    }

    @Test
    void aCall_ShouldBeRejected_WhenARequiredArgumentIsMissing() {
        String response = call("""
                {"name":"get_league","arguments":{}}
                """);

        assertThat(response).contains("\"isError\":true");
        assertThat(response).contains("leagueId");
    }

    @Test
    void aCall_ShouldBeRejected_WhenAnArgumentIsTheWrongType() {
        String response = call("""
                {"name":"get_league","arguments":{"leagueId":"seven"}}
                """);

        assertThat(response).contains("\"isError\":true");
        assertThat(response).contains("integer expected");
    }

    @Test
    void aCall_ShouldBeRejected_WhenTheStatusIsNotALeagueState() {
        String response = call("""
                {"name":"list_leagues","arguments":{"status":"ABANDONED"}}
                """);

        assertThat(response).contains("\"isError\":true");
        assertThat(response).contains("enumeration");
    }

    /** Rejected before the missing-credential check, and before any tool code. */
    private String call(String params) {
        return post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":"
                + params + "}");
    }

    private String post(String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
}
