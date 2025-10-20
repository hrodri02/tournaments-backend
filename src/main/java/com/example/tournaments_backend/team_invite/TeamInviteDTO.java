package com.example.tournaments_backend.team_invite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.tournaments_backend.player.PlayerDTO;

@Getter
@NoArgsConstructor
public class TeamInviteDTO {
    private Long id;
    private TeamInviteStatus status;
    private Long teamId;
    private PlayerDTO player;
    private LocalDateTime createdAt;

    public TeamInviteDTO(TeamInvite teamInvite) {
        this.id = teamInvite.getId();
        this.status = teamInvite.getStatus();
        this.teamId = teamInvite.getTeam().getId();
        this.player = new PlayerDTO(teamInvite.getInvitee());
        this.createdAt = teamInvite.getCreatedAt();
    }

    public static List<TeamInviteDTO> convert(List<TeamInvite> invites) {
        if (invites == null || invites.size() == 0) return List.of();
        return invites
            .stream()
            .map(TeamInviteDTO::new)
            .collect(Collectors.toList());
    }
}