package com.example.tournaments_backend.team;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TeamRequest {
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "Team name must be between 2 and 255 characters.")
    private String name;
    private String logoUrl;
    private List<String> playersToInvite;
    @NotNull
    private LocalDateTime createdAt;
}
