package com.example.tournaments_backend.league;

import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.team.Team;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class League {
    @Id
    @SequenceGenerator(
        name="league_sequence",
        sequenceName="league_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "league_sequence"
    )
    @EqualsAndHashCode.Include
    private Long id;
    private String name;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "duration_in_weeks")
    private Integer durationInWeeks;
    @ManyToMany
    @JoinTable(
        name = "league_team",
        joinColumns = @JoinColumn(name = "league_id"),
        inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private Set<Team> teams = new HashSet<>();
    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Game> games;

    public League(String name, LocalDate startDate, Integer durationInWeeks) {
        this.name = name;
        this.startDate = startDate;
        this.durationInWeeks = durationInWeeks;
    }

    public League(LeagueRequest leagueRequest) {
        this.name = leagueRequest.getName();
        this.startDate = leagueRequest.getStartDate();
        this.durationInWeeks = leagueRequest.getDurationInWeeks();
    }

    public void addTeam(Team team) {
        teams.add(team);
        team.getLeagues().add(this);
    }
}
