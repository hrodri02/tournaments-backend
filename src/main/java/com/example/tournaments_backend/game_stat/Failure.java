package com.example.tournaments_backend.game_stat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Failure {
    private final Long gameStatId;
    private final String message;
}
