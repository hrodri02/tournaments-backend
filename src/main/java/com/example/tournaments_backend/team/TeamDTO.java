package com.example.tournaments_backend.team;

import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.player.PlayerDTO;

import lombok.Getter;

@Getter
public class TeamDTO {
    private Long id;
    private String name;
    private List<PlayerDTO> players; // List of PlayerDTOs

    // Constructor to map from Team entity
    public TeamDTO(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        // Use stream to map Player entities to PlayerDTOs
        if (team.getPlayers() != null) {
            this.players = team.getPlayers().stream()
                             .map(PlayerDTO::new) // Call the PlayerDTO constructor that takes a Player entity
                             .collect(Collectors.toList());
        }
    }
}
