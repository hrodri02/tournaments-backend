package com.example.tournaments_backend.app_user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private AppUserRole appUserRole;

    public UserDTO(Long id, String firstName, String lastName, String email, AppUserRole appUserRole) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.appUserRole = appUserRole;
    }
}
