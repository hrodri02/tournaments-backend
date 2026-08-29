package com.example.tournaments_mcp.backend;

/**
 * The league states {@code GET /api/v1/leagues} accepts as a filter.
 *
 * <p>Declared here rather than borrowed from the backend so this process has no
 * compile-time dependency on it. It is used for the tool <em>input</em> only:
 * an enum gives the model a closed set of values to choose from, and a bad one
 * is rejected before a request is made. Responses keep {@code status} as a
 * plain string, so a state added to the backend cannot break deserialization.
 */
public enum LeagueStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ENDED
}
