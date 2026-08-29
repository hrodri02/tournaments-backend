package com.example.tournaments_mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.stereotype.Component;

import com.example.tournaments_mcp.backend.MyTeams;
import com.example.tournaments_mcp.backend.TournamentsBackendClient;
import com.example.tournaments_mcp.transport.CallerToken;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * User-scoped team tools.
 *
 * <p>"Mine" is the interesting case, and it is the one each MCP variant answers
 * differently. The stdio server could only answer for the single account named
 * at launch. The in-process HTTP server answered for the real caller, but by
 * reading {@code SecurityContextHolder}, which held only while the tool body
 * stayed on the request thread. Here the caller's token is a method argument
 * that the backend then authenticates, so the answer is per-caller and no part
 * of it depends on which thread the tool runs on.
 */
@Component
public class McpTeamTools {

    private final TournamentsBackendClient backend;

    public McpTeamTools(TournamentsBackendClient backend) {
        this.backend = backend;
    }

    @McpTool(
            name = "list_my_teams",
            description = "List the teams the current user plays for, and the teams "
                    + "they have been invited to join.",
            annotations = @McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true))
    public MyTeams listMyTeams(McpTransportContext context) {
        return backend.myTeams(CallerToken.from(context));
    }
}
