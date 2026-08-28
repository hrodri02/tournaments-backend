package com.example.tournaments_backend.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.team.GetTeamsResponse;
import com.example.tournaments_backend.team.TeamService;

/**
 * User-scoped team tools.
 *
 * <p>Unlike the league tools, these answer "mine" questions and so need a
 * principal. There is no request to take one from, so they act as the identity
 * configured at launch; see {@link McpIdentity}.
 *
 * <p>Restricted to the {@code mcp} profile, like {@link McpIdentity} itself,
 * since it cannot be constructed without one.
 */
@Component
@Profile("mcp")
public class McpTeamTools {

    private final TeamService teamService;
    private final McpIdentity identity;

    public McpTeamTools(TeamService teamService, McpIdentity identity) {
        this.teamService = teamService;
        this.identity = identity;
    }

    @Transactional(readOnly = true)
    @McpTool(
            name = "list_my_teams",
            description = "List the teams the current user plays for, and the teams "
                    + "they have been invited to join.")
    public GetTeamsResponse listMyTeams() {
        return identity.runAs(() -> teamService.getTeams(identity.authentication()));
    }
}
