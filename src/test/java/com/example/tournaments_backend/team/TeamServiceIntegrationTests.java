package com.example.tournaments_backend.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.AbstractIntegrationTest;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerRepository;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.team_invite.TeamInviteRepository;

@Transactional
public class TeamServiceIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamInviteRepository teamInviteRepository;

    private Player buildPlayer(String email) {
        return playerRepository.save(
            new Player(
                "John",
                 "Doe", email,
                 "password",
                   AppUserRole.PLAYER, 
                   Position.STRIKER)
        );
    }

    // ─── getTeamById ──────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getTeamById_ShouldReturnTeam_WhenTeamExists() {
        Player owner = buildPlayer("owner@test.com");
        Team team = new Team("Team A");
        team.setOwner(owner);
        Team saved = teamRepository.save(team);

        Team result = teamService.getTeamById(saved.getId());

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        assertThatThrownBy(() -> teamService.getTeamById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
    }

    // ─── addTeam ──────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void addTeam_ShouldPersistTeamWithOwner_WhenRequestIsValid() {
        Player owner = buildPlayer("owner@test.com");
        TeamRequest request = new TeamRequest("Team Alpha", null, List.of(), LocalDateTime.now());
        var auth = new UsernamePasswordAuthenticationToken(owner.getEmail(), null);

        TeamDTO result = teamService.addTeam(request, auth);

        assertNotNull(result.getId());
        assertThat(result.getName()).isEqualTo("Team Alpha");
        assertThat(teamRepository.findById(result.getId())).isPresent();
    }

    // ─── deleteTeamById ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void deleteTeamById_ShouldRemoveTeamFromDb_WhenTeamExists() {
        Player owner = buildPlayer("owner@test.com");
        Team team = new Team("Team B");
        team.setOwner(owner);
        Team saved = teamRepository.save(team);

        TeamDTO deleted = teamService.deleteTeamById(saved.getId());

        assertThat(deleted.getName()).isEqualTo("Team B");
        assertThat(teamRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        assertThatThrownBy(() -> teamService.deleteTeamById(-1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
    }

    // ─── updateTeam ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void updateTeam_ShouldPersistUpdatedFields_WhenTeamExists() {
        Player owner = buildPlayer("owner@test.com");
        Team team = new Team("Original Name");
        team.setOwner(owner);
        Team saved = teamRepository.save(team);

        TeamRequest updateRequest = new TeamRequest("Updated Name", "logo.png", List.of(), LocalDateTime.now());
        teamService.updateTeam(saved.getId(), updateRequest);

        Team inDb = teamRepository.findById(saved.getId()).get();
        assertThat(inDb.getName()).isEqualTo("Updated Name");
        assertThat(inDb.getLogoUrl()).isEqualTo("logo.png");
    }

    @Test
    void updateTeam_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        TeamRequest updateRequest = new TeamRequest("Updated Name", null, List.of(), LocalDateTime.now());

        assertThatThrownBy(() -> teamService.updateTeam(-1L, updateRequest))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
    }

    // ─── addPlayerToTeam ──────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void addPlayerToTeam_ShouldPopulateJoinTable_WhenBothExist() {
        Player owner = buildPlayer("owner@test.com");
        Team team = new Team("Team C");
        team.setOwner(owner);
        Team savedTeam = teamRepository.save(team);
        Player secondPlayer = buildPlayer("player2@test.com");

        TeamDTO result = teamService.addPlayerToTeam(secondPlayer.getId(), savedTeam.getId());

        assertThat(result.getPlayerDTOs()).anyMatch(p -> p.getId().equals(secondPlayer.getId()));
    }

    // ─── deletePlayerFromTeam ─────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void deletePlayerFromTeam_ShouldRemoveFromJoinTable_WhenBothExist() {
        Player owner = buildPlayer("owner@test.com");
        Team team = new Team("Team D");
        team.setOwner(owner);
        Team savedTeam = teamRepository.save(team);

        teamService.addPlayerToTeam(owner.getId(), savedTeam.getId());

        TeamDTO result = teamService.deletePlayerFromTeam(owner.getId(), savedTeam.getId());

        assertThat(result.getPlayerDTOs()).noneMatch(p -> p.getId().equals(owner.getId()));
    }
}
