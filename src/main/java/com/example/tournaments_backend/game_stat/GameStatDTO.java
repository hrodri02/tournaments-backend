package com.example.tournaments_backend.game_stat;

import java.time.LocalDateTime;

import com.example.tournaments_backend.player.PlayerDTO;

import lombok.Getter;

@Getter
public class GameStatDTO {
    private Long id;
    private GameStatType type;
    private LocalDateTime createdAt;
    private PlayerDTO playerDTO;

    public GameStatDTO(GameStat gameStat) {
        this.id = gameStat.getId();
        this.type = gameStat.getType();
        this.createdAt = gameStat.getCreatedAt();
        this.playerDTO = new PlayerDTO(gameStat.getPlayer());
    }
}
