package com.example.tournaments_backend.league;

import com.example.tournaments_backend.team.Team;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "League name must be between 2 and 255 characters.")
    private String name;
    @NotNull
    @Future
    @Column(name = "start_date")
    private LocalDate startDate;
    @NotNull
    @Min(value = 4, message = "League duration must be at least 4 weeks long.")
    @Column(name = "duration_in_weeks")
    private Integer durationInWeeks;
    @ManyToMany
    @JoinTable(
        name = "league_team",
        joinColumns = @JoinColumn(name = "league_id"),
        inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private Set<Team> teams = new HashSet<>();

    public League(String name, LocalDate startDate, Integer durationInWeeks) {
        this.name = name;
        this.startDate = startDate;
        this.durationInWeeks = durationInWeeks;
    }

    public void addTeam(Team team) {
        teams.add(team);
        team.getLeagues().add(this);
    }
}
