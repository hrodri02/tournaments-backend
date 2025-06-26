package com.example.tournaments_backend.game_stat;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class GameStatRequest {
    @NotNull
    private final Long gameId;
    @NotNull
    private final Long playerId;
    @NotNull
    private final GameStatType type;
    @NotNull
    private final LocalDateTime createdAt;
}
