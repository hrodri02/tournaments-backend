package com.example.tournaments_backend.game;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class GameDTO {
    private Long id;
    private LocalDateTime gameDateTime;
    private String address;
    private Integer durationInMinutes;

    public GameDTO(Game game) {
        this.id = game.getId();
        this.gameDateTime = game.getGameDateTime();
        this.address = game.getAddress();
        this.durationInMinutes = game.getDurationInMinutes();
    }
}
