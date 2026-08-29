package com.example.tournaments_mcp.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One player on a team. Deliberately narrow: the backend's {@code PlayerDTO}
 * carries account details a tool result has no business putting in front of a
 * model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerSummary(
        Long id,
        String firstName,
        String lastName,
        String position) {
}
