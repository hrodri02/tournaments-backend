package com.example.tournaments_mcp.backend;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One league, without its roster, as {@code GET /api/v1/leagues} returns it.
 *
 * <p>A tolerant reader: it names the fields the tools surface and ignores the
 * rest, so the backend can add to {@code LeagueDTO} without breaking this
 * process. What the tool returns is this shape, not the backend's -- the two
 * contracts are free to differ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeagueSummary(
        Long id,
        String name,
        String status,
        LocalDate startDate,
        Integer durationInWeeks) {
}
