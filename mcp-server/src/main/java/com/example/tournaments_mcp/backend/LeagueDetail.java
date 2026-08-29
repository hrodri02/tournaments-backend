package com.example.tournaments_mcp.backend;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One league with its teams, as {@code GET /api/v1/leagues/{id}} returns it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeagueDetail(
        Long id,
        String name,
        String status,
        LocalDate startDate,
        Integer durationInWeeks,
        List<TeamSummary> teams) {
}
