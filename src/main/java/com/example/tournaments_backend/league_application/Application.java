package com.example.tournaments_backend.league_application;

import java.time.LocalDateTime;

import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.team.Team;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class Application {
    @Id
    @SequenceGenerator(
        name="application_sequence",
        sequenceName="application_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "application_sequence"
    )
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    public Application(Team team, League league, LocalDateTime createdAt, ApplicationStatus status) {
        this.team = team;
        this.league = league;
        this.createdAt = createdAt;
        this.status = status;
    }
}
