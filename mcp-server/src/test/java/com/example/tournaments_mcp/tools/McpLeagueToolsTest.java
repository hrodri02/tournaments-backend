package com.example.tournaments_mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.tournaments_mcp.backend.BackendException;
import com.example.tournaments_mcp.backend.LeagueDetail;
import com.example.tournaments_mcp.backend.LeagueStatus;
import com.example.tournaments_mcp.backend.LeagueSummary;
import com.example.tournaments_mcp.backend.TournamentsBackendClient;
import com.example.tournaments_mcp.transport.McpTransportConfig;

import io.modelcontextprotocol.common.McpTransportContext;

class McpLeagueToolsTest {

    private static final String BASE_URL = "http://tournaments.test";
    private static final String TOKEN = "Bearer header.payload.signature";

    private static final McpTransportContext CALLER = McpTransportContext.create(
            Map.of(McpTransportConfig.AUTHORIZATION_KEY, TOKEN));

    private MockRestServiceServer backend;
    private McpLeagueTools tools;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.backend = MockRestServiceServer.bindTo(builder).build();
        this.tools = new McpLeagueTools(new TournamentsBackendClient(builder, BASE_URL));
    }

    @Test
    void listLeagues_ShouldCallTheApiWithoutAFilter_WhenNoStatusIsGiven() {
        backend.expect(requestTo(BASE_URL + "/api/v1/leagues"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andRespond(withSuccess("""
                        [{"id":1,"name":"League A","status":"IN_PROGRESS",
                          "startDate":"2026-01-05","durationInWeeks":4}]
                        """, MediaType.APPLICATION_JSON));

        List<LeagueSummary> leagues = tools.listLeagues(CALLER, null);

        assertThat(leagues).containsExactly(
                new LeagueSummary(1L, "League A", "IN_PROGRESS", LocalDate.of(2026, 1, 5), 4));
        backend.verify();
    }

    @Test
    void listLeagues_ShouldPassTheStatusAsAQueryParameter_WhenOneIsGiven() {
        backend.expect(requestTo(BASE_URL + "/api/v1/leagues?status=ENDED"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(tools.listLeagues(CALLER, LeagueStatus.ENDED)).isEmpty();
        backend.verify();
    }

    @Test
    void getLeague_ShouldMapTheRosterAndIgnoreFieldsItDoesNotDeclare() {
        // The payload carries logoUrl, invites, invitees, email and leagueIds --
        // everything LeagueDTO/TeamBaseDTO actually sends. None of it is declared
        // by the records, and none of it may break the call.
        backend.expect(requestTo(BASE_URL + "/api/v1/leagues/7"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andRespond(withSuccess("""
                        {"id":7,"name":"League A","status":"IN_PROGRESS","logoUrl":null,
                         "startDate":"2026-01-05","durationInWeeks":4,
                         "teams":[{"id":3,"name":"Barcelona FC","logoUrl":null,"ownerId":2,
                                   "playerDTOs":[{"id":9,"firstName":"Sarah","lastName":"Connor",
                                                  "email":"sconnor@example.com",
                                                  "appUserRole":"PLAYER","position":"DEFENDER"}],
                                   "invites":[],"invitees":[],"leagueIds":[7]}]}
                        """, MediaType.APPLICATION_JSON));

        LeagueDetail league = tools.getLeague(CALLER, 7L);

        assertThat(league.id()).isEqualTo(7L);
        assertThat(league.teams()).singleElement().satisfies(team -> {
            assertThat(team.name()).isEqualTo("Barcelona FC");
            assertThat(team.players()).singleElement().satisfies(player -> {
                assertThat(player.firstName()).isEqualTo("Sarah");
                assertThat(player.position()).isEqualTo("DEFENDER");
            });
        });
        backend.verify();
    }

    @Test
    void getLeague_ShouldReportHowToGetAFreshToken_WhenTheApiAnswers401() {
        backend.expect(requestTo(BASE_URL + "/api/v1/leagues/7"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> tools.getLeague(CALLER, 7L))
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("/api/v1/auth/login");
    }

    @Test
    void getLeague_ShouldReportAMissingLeague_WhenTheApiAnswers404() {
        backend.expect(requestTo(BASE_URL + "/api/v1/leagues/404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> tools.getLeague(CALLER, 404L))
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("No such league or team");
    }

    @Test
    void listLeagues_ShouldNotCallTheApiAtAll_WhenTheCallerSentNoToken() {
        assertThatThrownBy(() -> tools.listLeagues(McpTransportContext.EMPTY, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no Authorization header");

        backend.verify();  // no request was made
    }
}
