package com.example.tournaments_backend.team_invite;

import com.example.tournaments_backend.team.TeamDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class AcceptTeamInviteResponse {
    TeamInviteDTO teamInvite;
    TeamDTO updatedTeam;
}
