package com.example.tournaments_backend.player;

import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.UserDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlayerDTO extends UserDTO {
    @NotNull
    private Position position;
    public PlayerDTO(Long id, String firstName, String lastName, String email, AppUserRole appUserRole, Position position) {
        super(id, firstName, lastName, email, appUserRole);
        this.position = position;
    }

    public PlayerDTO(Player player) {
        super(player.getId(), 
              player.getFirstName(), 
              player.getLastName(), 
              player.getEmail(), 
              player.getAppUserRole());
        this.position = player.getPosition();
    }
}
