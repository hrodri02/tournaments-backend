package com.example.tournaments_backend.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueDTO;
import com.example.tournaments_backend.league.LeagueService;
import com.example.tournaments_backend.league.LeagueStatus;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.team.Team;

@ExtendWith(MockitoExtension.class)
public class McpLeagueToolsTest {
    @Mock
    private LeagueService leagueService;
    @InjectMocks
    private McpLeagueTools mcpLeagueTools;

    /**
     * A league that genuinely has a team with a player, so a test asserting that
     * the roster is absent is proving the summary mapping dropped it rather than
     * that there was nothing to drop.
     */
    private League leagueWithOneTeam() {
        Player owner = new Player(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "password",
                AppUserRole.PLAYER,
                Position.MIDFIELDER);

        Team team = new Team("Barcelona FC");
        team.setOwner(owner);
        team.addPlayer(owner);

        League league = League.builder()
                .name("La Liga")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(12)
                .build();
        league.setId(1L);
        league.setTeams(Set.of(team));
        return league;
    }

    @Test
    void listLeagues_ShouldOmitTeams_WhenReturningSummaries() {
        // 1. Arrange
        when(leagueService.getLeagues()).thenReturn(List.of(leagueWithOneTeam()));

        // 2. Act
        List<LeagueDTO> result = mcpLeagueTools.listLeagues(null);

        // 3. Assert
        assertThat(result).hasSize(1);
        LeagueDTO summary = result.get(0);
        // The roster is the expensive half of the payload and browsing does not
        // need it; get_league is what returns it.
        assertThat(summary.getTeams()).isNull();
        assertThat(summary.getId()).isEqualTo(1L);
        assertThat(summary.getName()).isEqualTo("La Liga");
        assertThat(summary.getDurationInWeeks()).isEqualTo(12);
        assertThat(summary.getStatus()).isEqualTo(LeagueStatus.NOT_STARTED);
    }

    @Test
    void listLeagues_ShouldFilterByStatus_WhenStatusIsGiven() {
        // 1. Arrange
        when(leagueService.getLeagues(LeagueStatus.IN_PROGRESS))
                .thenReturn(List.of(leagueWithOneTeam()));

        // 2. Act
        List<LeagueDTO> result = mcpLeagueTools.listLeagues(LeagueStatus.IN_PROGRESS);

        // 3. Assert
        assertThat(result).hasSize(1);
        verify(leagueService).getLeagues(LeagueStatus.IN_PROGRESS);
        verify(leagueService, never()).getLeagues();
    }

    @Test
    void getLeague_ShouldIncludeTeamsAndPlayers_WhenLeagueExists() {
        // 1. Arrange
        when(leagueService.getLeagueById(1L)).thenReturn(leagueWithOneTeam());

        // 2. Act
        LeagueDTO result = mcpLeagueTools.getLeague(1L);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("La Liga");
        assertThat(result.getTeams()).hasSize(1);
        assertThat(result.getTeams().get(0).getName()).isEqualTo("Barcelona FC");
        assertThat(result.getTeams().get(0).getPlayerDTOs()).hasSize(1);
    }

    @Test
    void getLeague_ShouldPropagateServiceException_WhenLeagueDoesNotExist() {
        // 1. Arrange
        when(leagueService.getLeagueById(999L)).thenThrow(new ServiceException(
                HttpStatus.NOT_FOUND,
                ClientErrorKey.LEAGUE_NOT_FOUND,
                "League",
                "League with id 999 not found"));

        // 2. Act & Assert
        // The exception is not swallowed: the MCP client needs to see the failure
        // rather than an empty result.
        assertThatThrownBy(() -> mcpLeagueTools.getLeague(999L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(serviceException.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
    }
}
