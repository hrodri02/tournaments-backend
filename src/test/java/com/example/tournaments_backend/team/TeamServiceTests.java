package com.example.tournaments_backend.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerService;
import com.example.tournaments_backend.team_invite.TeamInvite;
import com.example.tournaments_backend.team_invite.TeamInviteService;
import com.example.tournaments_backend.team_invite.TeamInviteStatus;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTests {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private PlayerService playerService;
    @Mock
    private TeamInviteService teamInviteService;
    @InjectMocks
    private TeamService teamService;

    // ─── getTeamById ──────────────────────────────────────────────────────────

    @Test
    void getTeamById_ShouldReturnTeam_WhenTeamExists() {
        // 1. Arrange
        Team team = new Team("Team A");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        // 2. Act
        Team result = teamService.getTeamById(1L);

        // 3. Assert
        assertThat(result).isEqualTo(team);
    }

    @Test
    void getTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> teamService.getTeamById(1L))
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
    void addTeam_ShouldSaveTeamAndReturnDTO_WhenRequestIsValid() {
        // 1. Arrange
        String ownerEmail = "owner@test.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(ownerEmail, null);
        TeamRequest request = new TeamRequest("Team A", null, List.of(), LocalDateTime.now());

        Player mockOwner = mock(Player.class);
        when(mockOwner.getTeams()).thenReturn(new HashSet<>());
        when(playerService.getPlayerByEmail(ownerEmail)).thenReturn(mockOwner);
        when(playerService.getAllPlayersByEmail(List.of())).thenReturn(List.of());
        when(teamInviteService.addAll(List.of())).thenReturn(List.of());

        Player mockOwnerInSaved = mock(Player.class);
        when(mockOwnerInSaved.getId()).thenReturn(10L);

        Team savedTeam = mock(Team.class);
        when(savedTeam.getId()).thenReturn(1L);
        when(savedTeam.getName()).thenReturn("Team A");
        when(savedTeam.getLogoUrl()).thenReturn(null);
        when(savedTeam.getOwner()).thenReturn(mockOwnerInSaved);
        when(savedTeam.getPlayers()).thenReturn(Set.of());
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        // 2. Act
        TeamDTO result = teamService.addTeam(request, auth);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("Team A");
        verify(teamRepository).save(any(Team.class));
    }

    // ─── deleteTeamById ───────────────────────────────────────────────────────

    @Test
    void deleteTeamById_ShouldReturnDTO_WhenTeamExists() {
        // 1. Arrange
        Team team = new Team("Team B");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        // 2. Act
        TeamDTO result = teamService.deleteTeamById(1L);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("Team B");
        verify(teamRepository).deleteById(1L);
    }

    @Test
    void deleteTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> teamService.deleteTeamById(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
        verify(teamRepository, never()).deleteById(any());
    }

    // ─── updateTeam ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void updateTeam_ShouldUpdateAndReturnTeam_WhenTeamExists() {
        // 1. Arrange
        Team oldTeam = new Team("Old Name");
        TeamRequest updateRequest = new TeamRequest("New Name", "logo.png", List.of(), LocalDateTime.now());
        when(teamRepository.findById(1L)).thenReturn(Optional.of(oldTeam));
        when(teamRepository.save(any(Team.class))).thenReturn(oldTeam);

        // 2. Act
        Team result = teamService.updateTeam(1L, updateRequest);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getLogoUrl()).isEqualTo("logo.png");
        verify(teamRepository).save(oldTeam);
    }

    @Test
    void updateTeam_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        TeamRequest updateRequest = new TeamRequest("New Name", null, List.of(), LocalDateTime.now());
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> teamService.updateTeam(1L, updateRequest))
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
    void addPlayerToTeam_ShouldAddPlayerAndReturnDTO_WhenBothExist() {
        // 1. Arrange
        Player mockOwner = mock(Player.class);
        when(mockOwner.getId()).thenReturn(10L);

        Set<Team> playerTeams = new HashSet<>();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getId()).thenReturn(20L);
        when(mockPlayer.getTeams()).thenReturn(playerTeams);
        when(playerService.getPlayerById(20L)).thenReturn(mockPlayer);

        Team team = new Team("Team A");
        team.setOwner(mockOwner);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        // 2. Act
        TeamDTO result = teamService.addPlayerToTeam(20L, 1L);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("Team A");
        assertThat(result.getPlayerDTOs()).anyMatch(p -> p.getId().equals(20L));
        verify(teamRepository).save(team);
    }

    @Test
    void addPlayerToTeam_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        Player mockPlayer = mock(Player.class);
        when(playerService.getPlayerById(20L)).thenReturn(mockPlayer);
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> teamService.addPlayerToTeam(20L, 1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
    }

    // ─── deletePlayerFromTeam ─────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void deletePlayerFromTeam_ShouldRemovePlayerAndReturnDTO_WhenBothExist() {
        // 1. Arrange
        Player mockOwner = mock(Player.class);
        when(mockOwner.getId()).thenReturn(10L);

        Set<Team> playerTeams = new HashSet<>();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getTeams()).thenReturn(playerTeams);
        when(playerService.getPlayerById(20L)).thenReturn(mockPlayer);

        Team team = new Team("Team A");
        team.setOwner(mockOwner);
        team.addPlayer(mockPlayer);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        // 2. Act
        TeamDTO result = teamService.deletePlayerFromTeam(20L, 1L);

        // 3. Assert
        assertThat(result.getName()).isEqualTo("Team A");
        assertThat(result.getPlayerDTOs()).isEmpty();
        verify(teamRepository).save(team);
    }

    @Test
    void deletePlayerFromTeam_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        Player mockPlayer = mock(Player.class);
        when(playerService.getPlayerById(20L)).thenReturn(mockPlayer);
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> teamService.deletePlayerFromTeam(20L, 1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
    }

    // ─── getTeams ─────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void getTeams_ShouldReturnTeamsPartOfAndInvitedTo_WhenUserHasTeamsAndInvites() {
        // 1. Arrange
        String email = "player@test.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);

        AppUser mockUser = mock(AppUser.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.isAdmin()).thenReturn(false);
        when(appUserService.getAppUserByEmail(email)).thenReturn(mockUser);

        Player mockOwner = mock(Player.class);
        when(mockOwner.getId()).thenReturn(2L);

        Team myTeam = new Team("My Team");
        myTeam.setOwner(mockOwner);

        Team invitedTeam = new Team("Invited Team");
        invitedTeam.setOwner(mockOwner);

        when(teamRepository.findByPlayers_Id(1L)).thenReturn(List.of(myTeam));

        Player mockInvitee = mock(Player.class);
        TeamInvite invite = new TeamInvite();
        invite.setTeam(invitedTeam);
        invite.setStatus(TeamInviteStatus.PENDING);
        invite.setInvitee(mockInvitee);

        when(teamInviteService.getAllInvitesByPlayerId(1L, auth)).thenReturn(List.of(invite));
        when(teamRepository.findAllById(any())).thenReturn(List.of(invitedTeam));
        when(teamInviteService.getAllTeamInvites(any())).thenReturn(List.of());
        when(playerService.getAllPlayersByIds(any())).thenReturn(List.of());

        // 2. Act
        GetTeamsResponse result = teamService.getTeams(auth);

        // 3. Assert
        assertThat(result.getTeams()).hasSize(1);
        assertThat(result.getTeamsInvitedTo()).hasSize(1);
    }
}
