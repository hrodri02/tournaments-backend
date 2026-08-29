package com.example.tournaments_mcp.backend;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The caller's teams, as {@code GET /api/v1/teams} returns them: the ones they
 * play for, and the ones they have been invited to join.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MyTeams(
        List<TeamSummary> teams,
        List<TeamSummary> teamsInvitedTo) {
}
