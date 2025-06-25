package com.example.tournaments_backend.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/games")
@Tag(name = "Game Management", description = "API endpoints for managing games")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Create a game", description = "Returns the game created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a game", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - game is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping
    public ResponseEntity<GameDTO> addGame(@RequestBody @Valid CreateGameRequest requestBody) throws ServiceException {
        Game gameInDB = gameService.addGame(requestBody);
        GameDTO gameDTO = new GameDTO(gameInDB);
        return ResponseEntity.ok().body(gameDTO);
    }
}
