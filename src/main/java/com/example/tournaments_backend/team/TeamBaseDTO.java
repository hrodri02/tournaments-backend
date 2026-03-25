package com.example.tournaments_backend.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.player.PlayerDTO;
import com.example.tournaments_backend.team_invite.TeamInviteDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class TeamBaseDTO {
    private Long id;
    private String name;
    private String logoUrl;
    private Long ownerId;
    private List<PlayerDTO> playerDTOs;
    private List<TeamInviteDTO> invites;
    private List<PlayerDTO> invitees;

    // Constructor to map from Team entity
    public TeamBaseDTO(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.logoUrl = team.getLogoUrl();
        this.ownerId = team.getOwner().getId();
        this.playerDTOs = PlayerDTO.convert(team.getPlayers());
        this.invites = new ArrayList<>();
        this.invitees = new ArrayList<>();
    }

    public static List<TeamBaseDTO> convert(Collection<Team> teams) {
        if (teams == null || teams.isEmpty()) return List.of();
        return teams.stream()
                    .map(TeamBaseDTO::new)
                    .collect(Collectors.toList());
    }
}
