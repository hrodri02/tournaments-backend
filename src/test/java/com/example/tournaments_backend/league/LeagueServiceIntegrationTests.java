package com.example.tournaments_backend.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.AbstractIntegrationTest;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerRepository;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamRepository;

@Transactional
public class LeagueServiceIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private LeagueService leagueService;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    // ─── addLeague ────────────────────────────────────────────────────────────

    @Test
    void addLeague_ShouldPersistLeagueWithGeneratedId_WhenRequestIsValid() {
        LeagueRequest request = LeagueRequest.builder()
                .name("Liga A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        League result = leagueService.addLeague(request);

        assertNotNull(result.getId());
        assertThat(result.getName()).isEqualTo("Liga A");
        assertThat(result.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(result.getDurationInWeeks()).isEqualTo(4);
    }

    // ─── getLeagueById ────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getLeagueById_ShouldReturnLeague_WhenLeagueExists() {
        League leagueB = League.builder()
                .name("Liga B")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();
        League saved = leagueRepository.save(leagueB);

        League result = leagueService.getLeagueById(saved.getId());

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getLeagueById_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        assertThatThrownBy(() -> leagueService.getLeagueById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
    }

    // ─── getLeagues() ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getLeagues_ShouldReturnAllLeagues_WhenMultipleLeaguesExist() {
        League league1 = leagueRepository.save(League.builder()
                .name("Liga C")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());
        League league2 = leagueRepository.save(League.builder()
                .name("Liga D")
                .startDate(LocalDate.now().minusWeeks(1))
                .durationInWeeks(8)
                .build());

        List<League> result = leagueService.getLeagues();

        assertThat(result).contains(league1, league2);
    }

    // ─── getLeagues(LeagueStatus) ─────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getLeagues_ShouldReturnOnlyNotStartedLeagues_WhenStatusIsNotStarted() {
        League notStarted = leagueRepository.save(League.builder()
                .name("NOT_STARTED")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());
        League ended = leagueRepository.save(League.builder()
                .name("ENDED")
                .startDate(LocalDate.now().minusWeeks(10))
                .durationInWeeks(4)
                .build());

        List<League> result = leagueService.getLeagues(LeagueStatus.NOT_STARTED);

        assertThat(result).contains(notStarted);
        assertThat(result).doesNotContain(ended);
    }

    @Test
    @SuppressWarnings("null")
    void getLeagues_ShouldReturnOnlyInProgressLeagues_WhenStatusIsInProgress() {
        League inProgress = leagueRepository.save(League.builder()
                .name("IN_PROGRESS")
                .startDate(LocalDate.now().minusWeeks(2))
                .durationInWeeks(8)
                .build());
        League notStarted = leagueRepository.save(League.builder()
                .name("NOT_STARTED")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());

        List<League> result = leagueService.getLeagues(LeagueStatus.IN_PROGRESS);

        assertThat(result).contains(inProgress);
        assertThat(result).doesNotContain(notStarted);
    }

    @Test
    @SuppressWarnings("null")
    void getLeagues_ShouldReturnOnlyEndedLeagues_WhenStatusIsEnded() {
        League ended = leagueRepository.save(League.builder()
                .name("ENDED")
                .startDate(LocalDate.now().minusWeeks(10))
                .durationInWeeks(4)
                .build());
        League inProgress = leagueRepository.save(League.builder()
                .name("IN_PROGRESS")
                .startDate(LocalDate.now().minusWeeks(2))
                .durationInWeeks(8)
                .build());

        List<League> result = leagueService.getLeagues(LeagueStatus.ENDED);

        assertThat(result).contains(ended);
        assertThat(result).doesNotContain(inProgress);
    }

    // ─── addTeamToLeague ──────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void addTeamToLeague_ShouldPopulateJoinTable_WhenLeagueAndTeamExist() {
        Player owner = playerRepository.save(new Player(
                "Jane", "Doe", "jane@test.com", "password", AppUserRole.PLAYER, Position.STRIKER));
        Team team = new Team("Team Alpha");
        team.setOwner(owner);
        team = teamRepository.save(team);

        League league = leagueRepository.save(League.builder()
                .name("Liga con Equipo")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());

        League result = leagueService.addTeamToLeague(league.getId(), team.getId());

        assertThat(result.getTeams()).contains(team);
    }

    @Test
    void addTeamToLeague_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        assertThatThrownBy(() -> leagueService.addTeamToLeague(-1L, -1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
    }

    // ─── deleteLeagueById ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void deleteLeagueById_ShouldRemoveLeagueFromDb_WhenLeagueExists() {
        League saved = leagueRepository.save(League.builder()
                .name("Liga a eliminar")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());

        League deleted = leagueService.deleteLeagueById(saved.getId());

        assertThat(deleted).isEqualTo(saved);
        assertThat(leagueRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteLeagueById_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        assertThatThrownBy(() -> leagueService.deleteLeagueById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ─── updateLeague ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void updateLeague_ShouldPersistUpdatedFields_WhenLeagueExists() {
        League saved = leagueRepository.save(League.builder()
                .name("Liga original")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build());

        LocalDate newStartDate = LocalDate.now().plusWeeks(3);
        LeagueRequest updateRequest = LeagueRequest.builder()
                .name("Liga actualizada")
                .startDate(newStartDate)
                .durationInWeeks(8)
                .logoUrl("new-logo.png")
                .build();

        leagueService.updateLeague(saved.getId(), updateRequest);

        League inDb = leagueRepository.findById(saved.getId()).get();
        assertThat(inDb.getName()).isEqualTo("Liga actualizada");
        assertThat(inDb.getStartDate()).isEqualTo(newStartDate);
        assertThat(inDb.getDurationInWeeks()).isEqualTo(8);
        assertThat(inDb.getLogoUrl()).isEqualTo("new-logo.png");
    }

    @Test
    void updateLeague_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        LeagueRequest request = LeagueRequest.builder()
                .name("No importa")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        assertThatThrownBy(() -> leagueService.updateLeague(-1L, request))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
    }
}
