package com.example.tournaments_backend.app_user;

import com.example.tournaments_backend.auth.ValidEmail;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Long id;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "first name must be between 2 and 255 characters.")
    private String firstName;
    @NotNull
    @NotEmpty
    @Size(min = 2, max = 255, message = "first name must be between 2 and 255 characters.")
    private String lastName;
    @ValidEmail
    @NotNull
    @NotEmpty
    private String email;
    @NotNull
    private AppUserRole appUserRole;

    public UserDTO(Long id, String firstName, String lastName, String email, AppUserRole appUserRole) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.appUserRole = appUserRole;
    }
}
