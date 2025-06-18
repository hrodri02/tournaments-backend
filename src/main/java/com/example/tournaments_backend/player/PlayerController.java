package com.example.tournaments_backend.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ServiceException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/players")
@Tag(name = "Player Management", description = "API endpoints for managing players")
public class PlayerController {
    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(summary = "Get a player", description = "Returns a player by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved player information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerDTO.class))),
        @ApiResponse(responseCode = "404", description = "Player with given id not found."),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
    @GetMapping("{playerId}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable("playerId") Long playerId) throws ServiceException {
        PlayerDTO playerDTO = playerService.getPlayerDTOById(playerId);
        return ResponseEntity.ok().body(playerDTO);
    }

    @DeleteMapping("{playerId}")
    public ResponseEntity<PlayerDTO> deletePlayer(@PathVariable("playerId") Long playerId) throws ServiceException {
        PlayerDTO playerDTO = playerService.getPlayerDTOById(playerId);
        playerService.deletePlayerById(playerId);
        return ResponseEntity.ok().body(playerDTO);
    }

    @PutMapping("{playerId}")
    public ResponseEntity<PlayerDTO> updatePlayer(@PathVariable("playerId") Long playerId, @RequestBody @Valid PlayerDTO playerDTO) throws ServiceException {
        PlayerDTO updatedPlayerDTO = playerService.updatePlayer(playerId, playerDTO);
        return ResponseEntity.ok().body(updatedPlayerDTO);
    }
}
