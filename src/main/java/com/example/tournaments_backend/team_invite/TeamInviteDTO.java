package com.example.tournaments_backend.team_invite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamInviteDTO {
    private Long id;
    private TeamInviteStatus status;
    private Long teamId;
    private Long playerId;
    private LocalDateTime createdAt;

    public TeamInviteDTO(TeamInvite teamInvite) {
        this.id = teamInvite.getId();
        this.status = teamInvite.getStatus();
        this.teamId = teamInvite.getTeam().getId();
        this.playerId = teamInvite.getInvitee().getId();
        this.createdAt = teamInvite.getCreatedAt();
    }

    public static List<TeamInviteDTO> convert(List<TeamInvite> invites) {
        return invites
            .stream()
            .map(TeamInviteDTO::new)
            .collect(Collectors.toList());
    }
}