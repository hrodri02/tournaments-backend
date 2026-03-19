package com.example.tournaments_backend.team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.PlayerDTO;
import com.example.tournaments_backend.team_invite.TeamInvite;
import com.example.tournaments_backend.team_invite.TeamInviteDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class TeamDTO extends TeamBaseDTO {
    private List<Long> leagueIds;

    // Constructor to map from Team entity
    public TeamDTO(Team team) {
        super(team);
        this.leagueIds = League.getIdsFromLeagues(team.getLeagues());
    }

    public static TeamDTO fromEntity(Team team, List<TeamInvite> invites) {
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .logoUrl(team.getLogoUrl())
                .ownerId(team.getOwner().getId())
                .playerDTOs(PlayerDTO.convert(team.getPlayers()))
                .invites(TeamInviteDTO.convert(invites))
                .leagueIds(new ArrayList<>())
                .build();
    }

    public static List<TeamBaseDTO> convert(List<Team> teams) {
        return teams.stream()
                    .map(TeamDTO::new)
                    .collect(Collectors.toList());
    }
}
