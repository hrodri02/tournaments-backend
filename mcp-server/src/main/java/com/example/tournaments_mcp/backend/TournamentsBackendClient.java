package com.example.tournaments_mcp.backend;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.example.tournaments_mcp.transport.CallerToken;

/**
 * The tournaments REST API, as this process sees it.
 *
 * <p>Every method takes the {@link CallerToken} rather than reading an ambient
 * one, so a request can only be made on behalf of somebody who actually asked
 * for it. There is no service identity to fall back to.
 *
 * <p>All three endpoints sit behind {@code anyRequest().authenticated()} in the
 * backend's {@code SecurityConfig} -- including the league reads, which needed
 * no principal on the in-process branches because they called
 * {@code LeagueService} directly. Going through HTTP removes that shortcut.
 */
@Component
public class TournamentsBackendClient {

    private static final ParameterizedTypeReference<List<LeagueSummary>> LEAGUE_LIST =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public TournamentsBackendClient(
            RestClient.Builder restClientBuilder,
            @Value("${tournaments.api.base-url}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, TournamentsBackendClient::translate)
                .build();
    }

    public List<LeagueSummary> leagues(CallerToken token, LeagueStatus status) {
        return get(token, uriBuilder -> {
            uriBuilder.path("/api/v1/leagues");
            if (status != null) {
                uriBuilder.queryParam("status", status.name());
            }
            return uriBuilder.build();
        }).body(LEAGUE_LIST);
    }

    public LeagueDetail league(CallerToken token, Long leagueId) {
        return get(token, uriBuilder -> uriBuilder
                .path("/api/v1/leagues/{leagueId}")
                .build(leagueId))
                .body(LeagueDetail.class);
    }

    public MyTeams myTeams(CallerToken token) {
        return get(token, uriBuilder -> uriBuilder
                .path("/api/v1/teams")
                .build())
                .body(MyTeams.class);
    }

    private RestClient.ResponseSpec get(CallerToken token, Function<UriBuilder, URI> uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, token.headerValue())
                    .retrieve();
        }
        catch (ResourceAccessException ex) {
            throw new BackendException(
                    "The tournaments API is unreachable. Check that the backend and nginx "
                    + "are running: docker compose -f devops/local/docker-compose.yml ps",
                    ex);
        }
    }

    /**
     * Turns a backend status code into something a model can act on.
     *
     * <p>The backend answers errors with an {@code ErrorDetails} body, but a
     * model reading a tool result is better served by a sentence naming the
     * remedy than by that structure -- and 401 in particular has a remedy this
     * process cannot apply itself, since it does not mint tokens.
     */
    private static void translate(HttpRequest request, ClientHttpResponse response)
            throws IOException {
        HttpStatusCode status = response.getStatusCode();
        throw new BackendException(switch (status.value()) {
            case 401 -> "The tournaments API rejected the caller's token (401). It has "
                    + "expired or is not valid; get a new one from POST /api/v1/auth/login "
                    + "and update the Authorization header configured for this MCP server.";
            case 403 -> "The caller is authenticated but not allowed to read this (403).";
            case 404 -> "No such league or team (404).";
            case 429 -> "The tournaments API is rate limiting this client (429). Wait a "
                    + "moment and retry.";
            default -> "The tournaments API returned " + status.value() + " "
                    + response.getStatusText() + ".";
        });
    }
}
