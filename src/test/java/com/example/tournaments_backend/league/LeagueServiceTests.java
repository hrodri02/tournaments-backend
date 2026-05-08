package com.example.tournaments_backend.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        List<League> result = leagueService.getLeagues(Optional.empty());

        // 3. Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(league1, league2);
        verify(leagueRepository).findAll();
    }

    @Test
    void getLeagues_ShouldReturnNotStartedLeagues_WhenNoStatusIsNotStarted() {
        // 1. Arrange
        League league1 = League.builder()
                .name("League A")
                .startDate(LocalDate.now().plusWeeks(1))
                .durationInWeeks(4)
                .build();

        when(leagueRepository.findByStartDateAfter(LocalDate.now())).thenReturn(List.of(league1));

        // 2. Act
        Optional<LeagueStatus> status = Optional.of(LeagueStatus.NOT_STARTED);
        List<League> result = leagueService.getLeagues(status);

        // 3. Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(league1);
        verify(leagueRepository).findByStartDateAfter(LocalDate.now());
    }
}
