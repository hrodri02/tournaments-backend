package com.example.tournaments_backend.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ServiceException;

@RestController
@RequestMapping(path="/api/v1/players")
public class PlayerController {
    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("{playerId}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable("playerId") Long playerId) throws ServiceException {
        Player player = playerService.getPlayerById(playerId);
        PlayerDTO playerDTO = 
            new PlayerDTO(playerId, 
                          player.getFirstName(),
                          player.getLastName(),
                          player.getEmail(), 
                          player.getAppUserRole(),
                          player.getPosition());
        return ResponseEntity.ok().body(playerDTO);
    }
}
