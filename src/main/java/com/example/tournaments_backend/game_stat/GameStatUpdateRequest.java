package com.example.tournaments_backend.game_stat;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GameStatUpdateRequest extends GameStatRequest {
    @NotNull
    private final Long id;

    public GameStatUpdateRequest(Long id, Long gameId, Long playerId, GameStatType type, LocalDateTime createAt) {
        super(gameId, playerId, type, createAt);
        this.id = id;
    }
}
