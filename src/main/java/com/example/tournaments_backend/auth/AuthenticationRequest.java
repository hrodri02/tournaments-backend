package com.example.tournaments_backend.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
@Getter
@EqualsAndHashCode
@AllArgsConstructor 
@ToString
public class AuthenticationRequest {
    @ValidEmail
    @NotNull
    @NotEmpty
    private final String email;
    @NotNull
    @NotEmpty
    private final String password;
}
