package com.example.tournaments_backend.game_stat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.player.PlayerDTO;

import lombok.Getter;

@Getter
public class GameStatDTO {
    private Long id;
    private Long gameId;
    private GameStatType type;
    private LocalDateTime createdAt;
    private PlayerDTO player;

    public GameStatDTO(GameStat gameStat) {
        this.id = gameStat.getId();
        this.gameId = gameStat.getGame().getId();
        this.type = gameStat.getType();
        this.createdAt = gameStat.getCreatedAt();
        this.player = new PlayerDTO(gameStat.getPlayer());
    }

    public static List<GameStatDTO> convert(List<GameStat> gameStats) {
        return gameStats.stream()
                .map(GameStatDTO::new)
                .collect(Collectors.toList());
    }
}
