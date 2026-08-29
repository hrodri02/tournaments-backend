package com.example.tournaments_mcp.backend;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One team and its players.
 *
 * <p>{@code players} is aliased to the backend's {@code playerDTOs}: the wire
 * name leaks a mapping detail, and this is the boundary where it stops.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamSummary(
        Long id,
        String name,
        Long ownerId,
        @JsonAlias("playerDTOs") List<PlayerSummary> players) {
}
