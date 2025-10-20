package com.example.tournaments_backend.team;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerDTO;
import com.example.tournaments_backend.team_invite.TeamInviteDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamDTO {
    private Long id;
    private String name;
    private String logoUrl;
    private Long ownerId;
    private List<Long> leagueIds;
    private List<PlayerDTO> playerDTOs;
    private String invitationStatus;
    private List<TeamInviteDTO> invites;

    // Constructor to map from Team entity
    public TeamDTO(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.logoUrl = team.getLogoUrl();
        this.ownerId = team.getOwner().getId();
        this.leagueIds = 
            team.getLeagues()
                .stream()
                .map(League::getId)
                .collect(Collectors.toList());
        this.invitationStatus = null;
        Set<Player> players = team.getPlayers();
        if (players != null) {
            this.playerDTOs = players.stream()
                                .map(PlayerDTO::new)
                                .collect(Collectors.toList());
        } 
    }

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

    public static List<TeamDTO> convert(List<Team> teams) {
        return teams.stream()
                    .map(TeamDTO::new)
                    .collect(Collectors.toList());
    }
}
