package com.example.tournaments_backend.team;

import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.league.League;

import lombok.Getter;

@Getter
public class TeamDTO {
    private Long id;
    private String name;
    private String logoUrl;
    private Long ownerId;
    private List<Long> leagueIds;
    private String invitationStatus;

    // Constructor to map from Team entity
    public TeamDTO(Team team, String invitationStatus) {
        this.id = team.getId();
        this.name = team.getName();
        this.logoUrl = team.getLogoUrl();
        this.ownerId = team.getOwner().getId();
        this.leagueIds = 
            team.getLeagues()
                .stream()
                .map(League::getId)
                .collect(Collectors.toList());
        this.invitationStatus = invitationStatus;
    }

    public TeamDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
