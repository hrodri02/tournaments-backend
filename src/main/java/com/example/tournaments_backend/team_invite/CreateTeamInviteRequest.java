package com.example.tournaments_backend.team_invite;

import java.time.LocalDateTime;

import com.example.tournaments_backend.auth.ValidEmail;

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
public class CreateTeamInviteRequest {
    @ValidEmail
    @NotNull
    private String email;
    @NotNull
    private LocalDateTime createdAt;
}
