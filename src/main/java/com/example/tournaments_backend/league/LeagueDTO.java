package com.example.tournaments_backend.league;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.team.TeamDTO;

import lombok.Getter;

@Getter
public class LeagueDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private Integer durationInWeeks;
    private List<TeamDTO> teams;

    public LeagueDTO(League league) {
        this.id = league.getId();
        this.name = league.getName();
        this.startDate = league.getStartDate();
        this.durationInWeeks = league.getDurationInWeeks();
        if (league.getTeams() != null) {
            this.teams = 
                league.getTeams()
                    .stream()
                    .map(TeamDTO::new)
                    .collect(Collectors.toList());
        }
    }

    public static List<LeagueDTO> convertLeagues(List<League> leagues) {
        return leagues.stream()
            .map(LeagueDTO::new)
            .collect(Collectors.toList());
    }
}
