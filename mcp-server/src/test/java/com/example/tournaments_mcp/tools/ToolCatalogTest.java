package com.example.tournaments_mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tournaments_mcp.backend.BackendException;
import com.example.tournaments_mcp.backend.LeagueStatus;
import com.example.tournaments_mcp.backend.LeagueSummary;
import com.example.tournaments_mcp.backend.MyTeams;
import com.example.tournaments_mcp.transport.McpTransportConfig;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers the plumbing that Spring AI used to generate.
 *
 * <p>Two things live here that no test needed on the Spring AI branch. The
 * first is the declared contract: the schemas in {@link ToolCatalog} are now
 * hand-written, so nothing but a test stops one from drifting away from the
 * method it describes. The second is that every handler converts a thrown
 * exception into an error <em>result</em> -- the SDK would otherwise let it
 * escape as a JSON-RPC protocol error, and a client would see the server
 * malfunctioning rather than the tool reporting a problem it can explain.
 *
 * <p>The failure case is asserted per tool rather than once against
 * {@link ToolResults}, because the mistake being guarded against is a handler
 * that forgets to route through it.
 */
class ToolCatalogTest {

    private static final McpTransportContext CALLER = McpTransportContext.create(
            Map.of(McpTransportConfig.AUTHORIZATION_KEY, "Bearer header.payload.signature"));

    private McpLeagueTools leagueTools;
    private McpTeamTools teamTools;
    private ToolCatalog catalog;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        this.leagueTools = mock(McpLeagueTools.class);
        this.teamTools = mock(McpTeamTools.class);
        this.catalog = new ToolCatalog(
                new ToolResults(jsonMapper), new JacksonMcpJsonMapper(jsonMapper));
    }

    // --- the declared contract ---------------------------------------------

    @Test
    void listLeagues_ShouldDeclareAnOptionalStatusFilterOfTheThreeKnownStates() {
        Tool tool = catalog.listLeaguesTool(leagueTools).tool();

        assertThat(tool.name()).isEqualTo("list_leagues");
        assertThat(required(tool)).isEmpty();
        assertThat(property(tool, "status"))
                .containsEntry("type", "string")
                .containsEntry("enum", List.of("NOT_STARTED", "IN_PROGRESS", "ENDED"));
    }

    /**
     * The enum in the schema is written out by hand, so it can fall behind
     * {@link LeagueStatus} as states are added. Nothing else would notice: a
     * state missing from the schema is simply a filter the model cannot ask
     * for, and every existing test still passes.
     */
    @Test
    void listLeagues_ShouldOfferEveryLeagueStatus() {
        Tool tool = catalog.listLeaguesTool(leagueTools).tool();

        assertThat(property(tool, "status").get("enum"))
                .isEqualTo(java.util.Arrays.stream(LeagueStatus.values()).map(Enum::name).toList());
    }

    @Test
    void getLeague_ShouldDeclareLeagueIdAsARequiredInteger() {
        Tool tool = catalog.getLeagueTool(leagueTools).tool();

        assertThat(tool.name()).isEqualTo("get_league");
        assertThat(required(tool)).containsExactly("leagueId");
        assertThat(property(tool, "leagueId")).containsEntry("type", "integer");
    }

    @Test
    void listMyTeams_ShouldTakeNoArguments() {
        Tool tool = catalog.listMyTeamsTool(teamTools).tool();

        assertThat(tool.name()).isEqualTo("list_my_teams");
        assertThat(required(tool)).isEmpty();
        assertThat(properties(tool)).isEmpty();
    }

    @Test
    void everyTool_ShouldAdvertiseItselfAsReadOnly() {
        List<Tool> tools = List.of(
                catalog.listLeaguesTool(leagueTools).tool(),
                catalog.getLeagueTool(leagueTools).tool(),
                catalog.listMyTeamsTool(teamTools).tool());

        assertThat(tools).allSatisfy(tool -> {
            assertThat(tool.annotations().readOnlyHint()).isTrue();
            assertThat(tool.annotations().destructiveHint()).isFalse();
            assertThat(tool.annotations().idempotentHint()).isTrue();
            assertThat(tool.annotations().openWorldHint()).isTrue();
        });
    }

    // --- arguments in ------------------------------------------------------

    @Test
    void listLeagues_ShouldPassNoFilter_WhenTheClientOmitsStatus() {
        call(catalog.listLeaguesTool(leagueTools), Map.of());

        verify(leagueTools).listLeagues(CALLER, null);
    }

    @Test
    void listLeagues_ShouldTurnTheStatusStringIntoTheEnum() {
        call(catalog.listLeaguesTool(leagueTools), Map.of("status", "ENDED"));

        verify(leagueTools).listLeagues(CALLER, LeagueStatus.ENDED);
    }

    /**
     * JSON has one number type, so an id small enough arrives as an Integer and
     * a large one as a Long. Both have to reach the tool as the same Long.
     */
    @Test
    void getLeague_ShouldAcceptTheIdWhicheverNumberTypeJacksonChose() {
        call(catalog.getLeagueTool(leagueTools), Map.of("leagueId", 7));
        call(catalog.getLeagueTool(leagueTools), Map.of("leagueId", 3_000_000_000L));

        verify(leagueTools).getLeague(CALLER, 7L);
        verify(leagueTools).getLeague(CALLER, 3_000_000_000L);
    }

    // --- results out -------------------------------------------------------

    @Test
    void aTool_ShouldReturnItsValueAsJsonText() {
        when(teamTools.listMyTeams(any())).thenReturn(new MyTeams(List.of(), List.of()));

        CallToolResult result = call(catalog.listMyTeamsTool(teamTools), Map.of());

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo("{\"teams\":[],\"teamsInvitedTo\":[]}");
    }

    @Test
    void listLeagues_ShouldReportAFailureAsAnErrorResult() {
        when(leagueTools.listLeagues(any(), eq(null)))
                .thenThrow(new BackendException("No such league or team (404)."));

        CallToolResult result = call(catalog.listLeaguesTool(leagueTools), Map.of());

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("No such league or team (404).");
    }

    @Test
    void getLeague_ShouldReportAFailureAsAnErrorResult() {
        when(leagueTools.getLeague(any(), any()))
                .thenThrow(new IllegalStateException("no Authorization header"));

        CallToolResult result = call(catalog.getLeagueTool(leagueTools), Map.of("leagueId", 1));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("no Authorization header");
    }

    @Test
    void listMyTeams_ShouldReportAFailureAsAnErrorResult() {
        when(teamTools.listMyTeams(any()))
                .thenThrow(new BackendException("The tournaments API is unreachable."));

        CallToolResult result = call(catalog.listMyTeamsTool(teamTools), Map.of());

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("The tournaments API is unreachable.");
    }

    @Test
    void aTool_ShouldSerialiseWhatTheBackendReturned_WithoutReshapingIt() {
        when(leagueTools.listLeagues(any(), eq(LeagueStatus.IN_PROGRESS))).thenReturn(
                List.of(new LeagueSummary(1L, "League A", "IN_PROGRESS",
                        java.time.LocalDate.of(2026, 1, 5), 4)));

        CallToolResult result =
                call(catalog.listLeaguesTool(leagueTools), Map.of("status", "IN_PROGRESS"));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).isEqualTo(
                "[{\"id\":1,\"name\":\"League A\",\"status\":\"IN_PROGRESS\","
                + "\"startDate\":\"2026-01-05\",\"durationInWeeks\":4}]");
    }

    // --- helpers -----------------------------------------------------------

    private static CallToolResult call(SyncToolSpecification specification,
            Map<String, Object> arguments) {
        return specification.callHandler().apply(CALLER, CallToolRequest.builder()
                .name(specification.tool().name())
                .arguments(arguments)
                .build());
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(Tool tool) {
        return (Map<String, Object>) tool.inputSchema().get("properties");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Tool tool, String name) {
        return (Map<String, Object>) properties(tool).get(name);
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(Tool tool) {
        return (List<String>) tool.inputSchema().get("required");
    }
}
