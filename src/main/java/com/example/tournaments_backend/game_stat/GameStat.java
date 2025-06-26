package com.example.tournaments_backend.game_stat;

import java.time.LocalDateTime;

import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.player.Player;
import com.fasterxml.jackson.annotation.JsonBackReference;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class GameStat {
    @Id
    @SequenceGenerator(
        name="game_stat_sequence",
        sequenceName="game_stat_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "game_stat_sequence"
    )
    private Long id;
    @Enumerated(EnumType.STRING)
    private GameStatType type;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonBackReference
    private Player player;

    public GameStat(GameStatType type, LocalDateTime createdAt) {
        this.type = type;
        this.createdAt = createdAt;
    }

    public GameStat(GameStatRequest request) {
        this.type = request.getType();
        this.createdAt = request.getCreatedAt();
    }
}
