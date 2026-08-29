package com.example.tournaments_backend.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.tournaments_backend.team.GetTeamsResponse;
import com.example.tournaments_backend.team.TeamService;

/**
 * User-scoped team tools exposed to MCP clients.
 *
 * <p>Unlike {@link McpLeagueTools}, these answer "mine" questions and so need a
 * principal. Serving MCP from this application rather than a separate process
 * is what makes that free: {@code /mcp} falls under
 * {@code anyRequest().authenticated()}, so the JWT filter chain has already put
 * the caller in the security context by the time a tool runs, and its
 * {@code getName()} is the {@code sub} claim -- the email the service layer
 * reads.
 *
 * <p>Reading that context here is only sound because the tool body runs inline
 * on the request thread, and that holds by a fairly narrow margin. The MCP SDK
 * wraps sync tools in {@code Mono.fromCallable(...)} and defaults to adding
 * {@code subscribeOn(Schedulers.boundedElastic())}; Spring AI suppresses it by
 * calling {@code immediateExecution(true)} on the stateless sync server, but
 * only when it finds a {@code StandardServletEnvironment}, and the WebMVC
 * transport then subscribes with a plain {@code block()}. Since
 * {@link SecurityContextHolder} is thread-local, anything that reintroduces that
 * offload -- a non-servlet environment, or declaring the server bean by hand
 * without {@code immediateExecution} -- empties the context here.
 * {@code McpTeamToolsIntegrationTest} is what fails if it ever does.
 *
 * <p>Setting {@code spring.ai.mcp.server.type=ASYNC} is not that failure, and is
 * worth knowing apart from it: the async server never registers sync
 * {@code @McpTool} methods at all, so {@code tools/list} comes back empty rather
 * than the tool running without a principal.
 */
@Component
public class McpTeamTools {

    private final TeamService teamService;

    public McpTeamTools(TeamService teamService) {
        this.teamService = teamService;
    }

    @McpTool(
        name = "list_my_teams",
        description = "List the teams the current user plays for, and the teams "
                + "they have been invited to join.")
    public GetTeamsResponse listMyTeams() {
        return teamService.getTeams(currentUser());
    }

    private Authentication currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException(
                    "No authenticated caller in the security context. The filter chain "
                    + "populates it, so a tool only sees it while running on the request "
                    + "thread; check that stateless sync tools are still executing inline "
                    + "rather than on a scheduler.");
        }
        return authentication;
    }
}
