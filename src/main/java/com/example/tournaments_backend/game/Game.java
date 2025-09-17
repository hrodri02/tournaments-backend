package com.example.tournaments_backend.game;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import com.example.tournaments_backend.game_stat.GameStat;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.team.Team;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Game {
    @Id
    @SequenceGenerator(
        name="game_sequence",
        sequenceName="game_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "game_sequence"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetching for performance
    @JoinColumn(name = "league_id", nullable = false) // Foreign key column in the 'games' table
    @JsonBackReference
    private League league;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetching for performance
    @JoinColumn(name = "home_team_id", nullable = false) // Foreign key column for home team
    @JsonBackReference
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetching for performance
    @JoinColumn(name = "away_team_id", nullable = false) // Foreign key column for away team
    @JsonBackReference
    private Team awayTeam;

    @Column(nullable = false)
    private LocalDateTime gameDateTime;
    @Column(nullable = false)
    private String address;
    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<GameStat> gameStats;
    
    public Game(LocalDateTime gameDateTime, String address, Integer durationInMinutes) {
        this.gameDateTime = gameDateTime;
        this.address = address;
        this.durationInMinutes = durationInMinutes;
    }

    public Game(GameRequest request) {
        this.gameDateTime = request.getGameDateTime();
        this.address = request.getAddress();
        this.durationInMinutes = request.getDurationInMinutes();
    }

    public boolean isActive() {
        LocalDateTime gameStartTime = getGameDateTime();
        Integer duration = getDurationInMinutes();
        LocalDateTime gameEndTime = gameStartTime.plusMinutes(duration);
        ZoneId sanFranciscoZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime sanFranciscoZonedTime = ZonedDateTime.now(sanFranciscoZone);
        LocalDateTime currentTime = sanFranciscoZonedTime.toLocalDateTime();
        return currentTime.isAfter(gameStartTime) && currentTime.isBefore(gameEndTime);
    }
}
