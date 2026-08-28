package com.example.tournaments_backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.league.LeagueDTO;

import tools.jackson.databind.ObjectMapper;

/**
 * Pins the JSON wire format of the API.
 *
 * <p>The rest of the suite asserts on domain objects and status codes, so a
 * changed Jackson default — dates as arrays, nulls omitted, enums as ordinals —
 * would pass CI and reach clients unnoticed. That gap is what made the Jackson
 * 2 to 3 upgrade require a manual before/after capture of real response bodies.
 *
 * <p>These assertions use the context's own {@code ObjectMapper}, the same bean
 * the HTTP message converters use, so they fail if Boot's configuration of it
 * changes and not merely if a hand-built mapper does.
 */
class JsonSerializationContractTests extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localDate_serializesAsIsoString_notAnArrayOrEpoch() {
        LeagueDTO league = new LeagueDTO(1L, "La Liga", LocalDate.of(2026, 9, 3), 12);

        String json = objectMapper.writeValueAsString(league);

        assertThat(json).contains("\"startDate\":\"2026-09-03\"");
    }

    @Test
    void localDateTime_serializesAsIsoString_notAnArrayOrEpoch() {
        ErrorDetails error = new ErrorDetails(
                HttpStatus.NOT_FOUND,
                ClientErrorKey.LEAGUE_NOT_FOUND.name(),
                LocalDateTime.of(2026, 9, 3, 15, 42, 25, 510064000));

        String json = objectMapper.writeValueAsString(error);

        assertThat(json).contains("\"timestamp\":\"2026-09-03T15:42:25.510064\"");
    }

    @Test
    void nullProperties_areEmitted_notOmitted() {
        LeagueDTO league = new LeagueDTO(1L, "La Liga", LocalDate.of(2026, 9, 3), 12);

        String json = objectMapper.writeValueAsString(league);

        // logoUrl and teams are both null on this constructor; clients rely on
        // the keys being present.
        assertThat(json).contains("\"logoUrl\":null");
        assertThat(json).contains("\"teams\":null");
    }

    @Test
    void enums_serializeByName_notOrdinal() {
        LeagueDTO league = new LeagueDTO(1L, "La Liga", LocalDate.of(2026, 9, 3), 12);

        String json = objectMapper.writeValueAsString(league);

        assertThat(json).contains("\"status\":\"NOT_STARTED\"");
    }

    @Test
    void errorDetails_keepsItsFieldNamesAndTypes() {
        ErrorDetails error = new ErrorDetails(
                HttpStatus.TOO_MANY_REQUESTS,
                ClientErrorKey.RATE_LIMIT_EXCEEDED.name(),
                LocalDateTime.of(2026, 9, 3, 15, 42, 25));

        String json = objectMapper.writeValueAsString(error);

        assertThat(json).contains("\"status\":429");
        assertThat(json).contains("\"errorKey\":\"RATE_LIMIT_EXCEEDED\"");
        assertThat(json).contains("\"validationErrors\":[]");
    }
}
