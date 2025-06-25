package com.example.tournaments_backend.game;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@AllArgsConstructor 
@ToString
public class GameRequest {
    @NotNull
    private Long leagueId;
    @NotNull
    private Long homeTeamId;
    @NotNull
    private Long awayTeamId;
    @NotNull
    @Future
    private LocalDateTime gameDateTime;
    @NotNull
    @Size(min = 2, max = 255, message = "Game address must be between 2 and 255 characters.")
    private String address;
    @NotNull
    @Min(value = 20, message = "Game duration must be at least 20 minutes.")
    @Max(value = 90, message = "Game duration must be at most 90 minutes.")
    private Integer durationInMinutes;
}
