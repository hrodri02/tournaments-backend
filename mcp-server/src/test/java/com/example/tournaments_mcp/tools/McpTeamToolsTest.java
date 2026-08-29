package com.example.tournaments_mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.tournaments_mcp.backend.MyTeams;
import com.example.tournaments_mcp.backend.TournamentsBackendClient;
import com.example.tournaments_mcp.transport.McpTransportConfig;

import io.modelcontextprotocol.common.McpTransportContext;

class McpTeamToolsTest {

    private static final String BASE_URL = "http://tournaments.test";

    private MockRestServiceServer backend;
    private McpTeamTools tools;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.backend = MockRestServiceServer.bindTo(builder).build();
        this.tools = new McpTeamTools(new TournamentsBackendClient(builder, BASE_URL));
    }

    /**
     * The claim this whole variant rests on: whose teams get returned is decided
     * by whose token arrived, not by anything configured in this process. Two
     * callers, two tokens, two different outbound requests.
     */
    @Test
    void listMyTeams_ShouldSendTheCallersOwnToken() {
        expectTeamsCallWith("Bearer alice.token", """
                {"teams":[{"id":1,"name":"Barcelona FC","ownerId":2,"playerDTOs":[]}],
                 "teamsInvitedTo":[]}
                """);
        expectTeamsCallWith("Bearer bob.token", """
                {"teams":[],
                 "teamsInvitedTo":[{"id":9,"name":"Real Madrid","ownerId":5,"playerDTOs":[]}]}
                """);

        MyTeams alice = tools.listMyTeams(callerWith("Bearer alice.token"));
        MyTeams bob = tools.listMyTeams(callerWith("Bearer bob.token"));

        assertThat(alice.teams()).singleElement()
                .satisfies(team -> assertThat(team.name()).isEqualTo("Barcelona FC"));
        assertThat(alice.teamsInvitedTo()).isEmpty();
        assertThat(bob.teams()).isEmpty();
        assertThat(bob.teamsInvitedTo()).singleElement()
                .satisfies(team -> assertThat(team.name()).isEqualTo("Real Madrid"));
        backend.verify();
    }

    private void expectTeamsCallWith(String token, String responseBody) {
        backend.expect(requestTo(BASE_URL + "/api/v1/teams"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, token))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private static McpTransportContext callerWith(String token) {
        return McpTransportContext.create(Map.of(McpTransportConfig.AUTHORIZATION_KEY, token));
    }
}
