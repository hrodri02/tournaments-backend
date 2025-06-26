package com.example.tournaments_backend.game_stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/gamestats")
@Tag(name = "Game Stats Management", description = "API endpoints for managing game stats")
public class GameStatController {
    private final GameStatService gameStatService;

    @Autowired
    public GameStatController(GameStatService gameStatService) {
        this.gameStatService = gameStatService;
    }

    @Operation(summary = "Create a game stat", description = "Returns the game stat created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a game stat", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStatDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - game stat is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game or player with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping
    public ResponseEntity<GameStatDTO> addGameStat(@RequestBody @Valid GameStatRequest request) {
        GameStat gameStat = gameStatService.addGameStat(request);
        GameStatDTO gameStatDTO = new GameStatDTO(gameStat);
        return ResponseEntity.ok().body(gameStatDTO);
    }
}
