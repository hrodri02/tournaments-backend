package com.example.tournaments_backend.game_stat;

import java.util.List;

import lombok.Getter;

@Getter
public class GameStatUpdateResponse {
    private final List<GameStatDTO> successfulUpdates;
    private final List<Failure> failures;

    public GameStatUpdateResponse(List<GameStat> successfulUpdates, List<Failure> failures) {
        this.successfulUpdates = GameStatDTO.convert(successfulUpdates);
        this.failures = failures;
    }
}
