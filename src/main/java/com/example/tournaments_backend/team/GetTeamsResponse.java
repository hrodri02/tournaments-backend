package com.example.tournaments_backend.team;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class GetTeamsResponse {
    List<TeamDTO> teams;
    List<TeamDTO> teamsInvitedTo;   
}
