package com.example.tournaments_backend.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;

import org.springframework.http.HttpStatus;

import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LeagueServiceTests {
    @Mock
    private TeamService teamService;
    @Mock
    private LeagueRepository leagueRepository;
    @InjectMocks
    private LeagueService leagueService;

    @Test
    @SuppressWarnings("null")
    void addLeague_ShouldSaveAndReturnLeague_WhenRequestIsValid() {
        // 1. Arrange
        LeagueRequest request = new LeagueRequest("League A", LocalDate.now().plusWeeks(1), 4, null);
        League savedLeague = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.save(any(League.class))).thenReturn(savedLeague);

        // 2. Act
        League result = leagueService.addLeague(request);

        // 3. Assert
        assertThat(result).isEqualTo(savedLeague);
        verify(leagueRepository).save(any(League.class));
    }

    @Test
    void getLeagueById_ShouldReturnLeague_WhenLeagueExists() {
        // 1. Arrange
        League league = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        // 2. Act
        League result = leagueService.getLeagueById(1L);

        // 3. Assert
        assertThat(result).isEqualTo(league);
        verify(leagueRepository).findById(1L);
    }

    @Test
    void getLeagueById_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        // 1. Arrange
        when(leagueRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> leagueService.getLeagueById(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(serviceException.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
        verify(leagueRepository).findById(1L);
    }

    @Test
    void getLeagues_ShouldReturnAllLeagues_WhenNoStatusIsProvided() {
        // 1. Arrange
        League league1 = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();
        League league2 = League.builder()
                .name("League B")
                .startDate(LocalDate.now().minusWeeks(1))
                .durationInWeeks(6)
                .build();
        List<League> allLeagues = List.of(league1, league2);

        when(leagueRepository.findAll()).thenReturn(allLeagues);

        // 2. Act
        List<League> result = leagueService.getLeagues();

        // 3. Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(league1, league2);
        verify(leagueRepository).findAll();
    }

    @Test
    void getLeagues_ShouldReturnNotStartedLeagues_WhenStatusIsNotStarted() {
        // 1. Arrange
        League league1 = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findByStartDateAfter(LocalDate.now())).thenReturn(List.of(league1));

        // 2. Act
        List<League> result = leagueService.getLeagues(LeagueStatus.NOT_STARTED);

        // 3. Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(league1);
        verify(leagueRepository).findByStartDateAfter(LocalDate.now());
    }

    @Test
    void getLeagues_ShouldReturnInProgressLeagues_WhenStatusIsInProgress() {
        // 1. Arrange
        League league1 = League.builder()
                .name("League A")
                .startDate(LocalDate.now().minusWeeks(2))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findInProgressLeagues(LocalDate.now())).thenReturn(List.of(league1));

        // 2. Act
        List<League> result = leagueService.getLeagues(LeagueStatus.IN_PROGRESS);

        // 3. Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(league1);
        verify(leagueRepository).findInProgressLeagues(LocalDate.now());
    }

    @Test
    @SuppressWarnings("null")
    void addTeamToLeague_ShouldAddTeamAndReturnLeague_WhenLeagueAndTeamExist() {
        // 1. Arrange
        League league = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();
        Team team = new Team("Team A");

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamService.getTeamById(2L)).thenReturn(team);
        when(leagueRepository.save(any(League.class))).thenReturn(league);

        // 2. Act
        League result = leagueService.addTeamToLeague(1L, 2L);

        // 3. Assert
        assertThat(result.getTeams()).contains(team);
    }

    @Test
    void addTeamToLeague_ShouldThrowServiceException_WhenLeagueDoesNotExist() {
        // 1. Arrange
        when(leagueRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThatThrownBy(() -> leagueService.addTeamToLeague(1L, 2L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(serviceException.getErrorKey()).isEqualTo(ClientErrorKey.LEAGUE_NOT_FOUND);
                });
    }

    @Test
    void addTeamToLeague_ShouldThrowServiceException_WhenTeamDoesNotExist() {
        // 1. Arrange
        League league = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamService.getTeamById(2L)).thenThrow(new ServiceException(
                HttpStatus.NOT_FOUND,
                ClientErrorKey.TEAM_NOT_FOUND,
                "Team",
                "Team with given id not found."
        ));

        // 2. Act & Assert
        assertThatThrownBy(() -> leagueService.addTeamToLeague(1L, 2L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(serviceException.getErrorKey()).isEqualTo(ClientErrorKey.TEAM_NOT_FOUND);
                });
        verify(leagueRepository).findById(1L);
    }

    @Test
    void getLeagues_ShouldReturnEndedLeagues_WhenStatusIsEnded() {
        // 1. Arrange
        League league1 = League.builder()
                .name("League A")
                .startDate(LocalDate.now().minusWeeks(10))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findEndedLeagues(LocalDate.now())).thenReturn(List.of(league1));

        // 2. Act
        List<League> result = leagueService.getLeagues(LeagueStatus.ENDED);

        // 3. Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(league1);
        verify(leagueRepository).findEndedLeagues(LocalDate.now());
    }
}
