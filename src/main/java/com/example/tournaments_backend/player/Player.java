package com.example.tournaments_backend.player;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@PrimaryKeyJoinColumn(name = "player_id") 
@Getter
@Setter
@NoArgsConstructor
public class Player extends AppUser {
    @NotNull
    @NotEmpty
    private Position position;

    public Player(String firstName,
                  String lastName, 
                  String email, 
                  String password,
                  AppUserRole appUserRole,
                  Position position)
    {
        super(firstName, lastName, email, password, appUserRole);
        this.position = position;
    }
}
