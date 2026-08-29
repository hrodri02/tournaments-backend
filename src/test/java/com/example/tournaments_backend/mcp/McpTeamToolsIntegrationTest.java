package com.example.tournaments_backend.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.example.tournaments_backend.AbstractIntegrationTest;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerRepository;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.security.JwtService;
import com.example.tournaments_backend.team.Team;

/**
 * Exercises {@code list_my_teams} over a real HTTP request to {@code /mcp}.
 *
 * <p>The unit tests install a principal on the thread that then calls the tool,
 * so they would pass whether or not the identity survives the MCP transport.
 * Only this test answers the question that matters: the tool reads
 * {@code SecurityContextHolder}, which is thread-local, and that is sound solely
 * because Spring AI runs stateless sync tools inline on the servlet thread. If
 * that ever stops holding, the tool sees no principal and this test goes red --
 * verified by probe, by reading the context from another thread, which turns
 * {@code listMyTeams_ShouldReturnTheCallersOwnTeams} into the tool's
 * "no authenticated caller" error surfaced as an MCP {@code isError} result.
 *
 * <p>Runs its own {@code RANDOM_PORT} context rather than extending
 * {@link AbstractIntegrationTest}, which is {@code WebEnvironment.NONE} and so
 * never builds the filter chain this test is about. Rate limiting is off because
 * the filter runs before authentication on every route and the window is
 * narrower than this test's request count.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ratelimit.enabled=false")
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.ContainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class McpTeamToolsIntegrationTest {

    private static final String MY_TEAM = "Ada's XI";
    private static final String SOMEONE_ELSES_TEAM = "Grace's XI";

    @LocalServerPort
    private int port;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private JwtService jwtService;

    private String myToken;

    /**
     * Two players on two different teams. One team would be enough to show the
     * tool returns something; a second is what lets the assertions show it
     * returns the <em>caller's</em> something.
     */
    @BeforeAll
    void seedTwoPlayersOnDifferentTeams() {
        Player me = savePlayerWithTeam("ada@example.com", MY_TEAM);
        savePlayerWithTeam("grace@example.com", SOMEONE_ELSES_TEAM);
        myToken = jwtService.createAccessToken(me);
    }

    private Player savePlayerWithTeam(String email, String teamName) {
        Player player = playerRepository.save(new Player(
                "Test",
                "Player",
                email,
                "password",
                AppUserRole.PLAYER,
                Position.MIDFIELDER));

        Team team = new Team(teamName);
        team.setOwner(player);
        team.addPlayer(player);

        // Player owns the join table, and cascades PERSIST, so saving the
        // player is what writes both the team and the membership.
        return playerRepository.save(player);
    }

    private ResponseEntity<String> callMcp(String jsonRpc, String bearerToken) {
        return RestClient.create("http://localhost:" + port)
                .post()
                .uri("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                .headers(headers -> {
                    if (bearerToken != null) {
                        headers.setBearerAuth(bearerToken);
                    }
                })
                .body(jsonRpc)
                .retrieve()
                // A 401 is an expected outcome here, not a transport failure,
                // so the default throw-on-error handler has to be replaced.
                .onStatus(status -> status.isError(), (request, response) -> { })
                .toEntity(String.class);
    }

    private static String toolsCall(String toolName) {
        return """
               {"jsonrpc":"2.0","id":1,"method":"tools/call",\
               "params":{"name":"%s","arguments":{}}}""".formatted(toolName);
    }

    @Test
    void toolsList_ShouldAdvertiseListMyTeams() {
        ResponseEntity<String> response = callMcp(
                """
                {"jsonrpc":"2.0","id":1,"method":"tools/list"}""",
                myToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("list_my_teams");
    }

    @Test
    void listMyTeams_ShouldReturnTheCallersOwnTeams() {
        ResponseEntity<String> response = callMcp(toolsCall("list_my_teams"), myToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(MY_TEAM);
        // The negative is the real assertion. A tool that lost the caller's
        // identity, or picked up the wrong one, still returns a well-formed
        // payload -- it just answers for somebody else.
        assertThat(response.getBody()).doesNotContain(SOMEONE_ELSES_TEAM);
    }

    @Test
    void mcpEndpoint_ShouldRejectUnauthenticatedCalls() {
        ResponseEntity<String> response = callMcp(toolsCall("list_my_teams"), null);

        // /mcp is covered by anyRequest().authenticated(), so this is 401 by
        // construction rather than by anything the tool does.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
