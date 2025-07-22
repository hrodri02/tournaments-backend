package com.example.tournaments_backend.league;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.game.GameDTO;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamDTO;

import lombok.Getter;

@Getter
public class LeagueDTO {
    private Long id;
    private String name;
    private LeagueStatus status;
    private LocalDate startDate;
    private Integer durationInWeeks;
    private List<TeamDTO> teams;
    private List<GameDTO> games;

    public LeagueDTO(League league) {
        this.id = league.getId();
        this.name = league.getName();
        this.startDate = league.getStartDate();
        this.durationInWeeks = league.getDurationInWeeks();
        setStatus();
        Set<Team> teamsSet = league.getTeams();
        if (teams != null) {
            this.teams = teamsSet
                    .stream()
                    .map(TeamDTO::new)
                    .collect(Collectors.toList());
        }
    }

    public LeagueDTO(Long id, String name, LocalDate startDate, Integer durationInWeeks) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.durationInWeeks = durationInWeeks;
        setStatus();
        this.teams = null;
    }

    private void setStatus() {
        LocalDate today = LocalDate.now();
        LocalDate end = this.startDate.plusWeeks(this.durationInWeeks);
        if (this.startDate.isAfter(today)) {
            this.status = LeagueStatus.NOT_STARTED;
        }
        else if (end.isBefore(today)) {
            this.status = LeagueStatus.ENDED;
        }
        else {
            this.status = LeagueStatus.IN_PROGRESS;
        }
    }

    public static List<LeagueDTO> convertLeagues(List<League> leagues) {
        return leagues.stream()
            .map(LeagueDTO::new)
            .collect(Collectors.toList());
    }
}
