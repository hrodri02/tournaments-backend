package com.example.tournaments_backend.league;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class LeagueRequest {
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "League name must be between 2 and 255 characters.")
    private final String name;
    @NotNull
    @Future(groups = OnCreate.class)
    private final LocalDate startDate;
    @NotNull
    @Min(value = 4, message = "League duration must be at least 4 weeks long.")
    private final Integer durationInWeeks;
    private final String logoUrl;
    
    public interface OnCreate {}
    public interface OnUpdate {}
}
