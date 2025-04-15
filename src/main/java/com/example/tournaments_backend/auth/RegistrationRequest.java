package com.example.tournaments_backend.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.tournaments_backend.app_user.AppUserRole;

import jakarta.validation.constraints.NotEmpty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@AllArgsConstructor 
@ToString
@PasswordMatches
public class RegistrationRequest {
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "first name must be between 2 and 255 characters.")
    private final String firstName;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "last name must be between 2 and 255 characters.")
    private final String lastName;
    @ValidEmail
    @NotNull
    @NotEmpty
    private final String email;
    @NotNull
    @NotEmpty
    private final String password;
    private final String matchingPassword;
    @NotNull
    private final AppUserRole role;
}
