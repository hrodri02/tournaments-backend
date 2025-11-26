package com.example.tournaments_backend.game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.game_stat.GameStatDTO;
import com.example.tournaments_backend.team.TeamDTO;

import lombok.Getter;

@Getter
public class GameDTO {
    private Long id;
    private Long leagueId;
    private LocalDateTime gameDateTime;
    private String address;
    private Integer durationInMinutes;
    private TeamDTO homeTeam;
    private TeamDTO awayTeam;
    private List<GameStatDTO> stats;

    public GameDTO(Game game) {
        this.id = game.getId();
        this.leagueId = game.getLeague().getId();
        this.gameDateTime = game.getGameDateTime();
        this.address = game.getAddress();
        this.durationInMinutes = game.getDurationInMinutes();
        this.homeTeam = new TeamDTO(game.getHomeTeam());
        this.awayTeam = new TeamDTO(game.getAwayTeam());
        this.stats = game.getGameStats()
                            .stream()
                            .map(GameStatDTO::new)
                            .toList();
    }

    public static List<GameDTO> convertGames(List<Game> games) {
        return games.stream()
            .map(GameDTO::new)
            .collect(Collectors.toList());
    }
}
