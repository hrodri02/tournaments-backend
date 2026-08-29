package com.example.tournaments_mcp.tools;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.tournaments_mcp.backend.LeagueDetail;
import com.example.tournaments_mcp.backend.LeagueStatus;
import com.example.tournaments_mcp.backend.LeagueSummary;
import com.example.tournaments_mcp.backend.TournamentsBackendClient;
import com.example.tournaments_mcp.transport.CallerToken;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * Read-only league tools.
 *
 * <p>The {@link McpTransportContext} parameter is injected by the MCP SDK and
 * is not part of the tool's input schema -- {@code
 * SyncStatelessMcpToolMethodCallback} recognises the type and fills it in
 * rather than looking for an argument of that name. So the model sees
 * {@code get_league(leagueId)}, and the caller's credential arrives beside it
 * without ever being something the model can set.
 */
@Component
public class McpLeagueTools {

    private final TournamentsBackendClient backend;

    public McpLeagueTools(TournamentsBackendClient backend) {
        this.backend = backend;
    }

    @McpTool(
            name = "list_leagues",
            description = "List tournament leagues, optionally filtered by status. "
                    + "Returns a summary of each league without its teams; "
                    + "call get_league for the full roster.",
            annotations = @McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true))
    public List<LeagueSummary> listLeagues(
            McpTransportContext context,
            @McpToolParam(
                    description = "Optional filter: NOT_STARTED, IN_PROGRESS, or ENDED",
                    required = false) LeagueStatus status) {
        return backend.leagues(CallerToken.from(context), status);
    }

    @McpTool(
            name = "get_league",
            description = "Get one league by id, including its teams and their players.",
            annotations = @McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true))
    public LeagueDetail getLeague(
            McpTransportContext context,
            @McpToolParam(description = "The league id", required = true) Long leagueId) {
        return backend.league(CallerToken.from(context), leagueId);
    }
}
