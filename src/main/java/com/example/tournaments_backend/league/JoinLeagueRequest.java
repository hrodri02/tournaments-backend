package com.example.tournaments_backend.league;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor 
@ToString
public class JoinLeagueRequest {
    @NotNull
    private Long teamId;
    @NotNull
    private LocalDateTime createdAt;
}