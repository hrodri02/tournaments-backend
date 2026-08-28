package com.example.tournaments_backend.mcp;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueDTO;
import com.example.tournaments_backend.league.LeagueService;
import com.example.tournaments_backend.league.LeagueStatus;

/**
 * Read-only league tools exposed to MCP clients.
 *
 * <p>These call {@link LeagueService} directly rather than the REST controllers:
 * the service methods take no {@code Authentication}, so league reads carry no
 * user scope and need no identity to answer.
 */
@Component
public class McpLeagueTools {

  private final LeagueService leagueService;

  public McpLeagueTools(LeagueService leagueService) {
    this.leagueService = leagueService;
  }

  @McpTool(
    name = "list_leagues",
    description = "List tournament leagues, optionally filtered by status. "
            + "Returns a summary of each league without its teams; "
            + "call get_league for the full roster.")
  public List<LeagueDTO> listLeagues(
    @McpToolParam(
        description = "Optional filter: NOT_STARTED, IN_PROGRESS, or ENDED",
        required = false) LeagueStatus status) {

      List<League> leagues = (status == null)
              ? leagueService.getLeagues()
              : leagueService.getLeagues(status);

      return leagues.stream()
              .map(league -> new LeagueDTO(
                      league.getId(),
                      league.getName(),
                      league.getStartDate(),
                      league.getDurationInWeeks()))
              .toList();
  }

  @McpTool(
    name = "get_league",
    description = "Get one league by id, including its teams and their players.")
  public LeagueDTO getLeague(
      @McpToolParam(description = "The league id", required = true) Long leagueId)
      throws ServiceException {
      return new LeagueDTO(leagueService.getLeagueById(leagueId));
  }
}
