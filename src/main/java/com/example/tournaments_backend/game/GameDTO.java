package com.example.tournaments_backend.game;

import java.time.LocalDateTime;

import com.example.tournaments_backend.team.TeamDTO;

import lombok.Getter;

@Getter
public class GameDTO {
    private Long id;
    private LocalDateTime gameDateTime;
    private String address;
    private Integer durationInMinutes;
    private TeamDTO homeTeam;
    private TeamDTO awayTeam;

    public GameDTO(Game game) {
        this.id = game.getId();
        this.gameDateTime = game.getGameDateTime();
        this.address = game.getAddress();
        this.durationInMinutes = game.getDurationInMinutes();
        this.homeTeam = new TeamDTO(game.getHomeTeam());
        this.awayTeam = new TeamDTO(game.getAwayTeam());
    }
}
