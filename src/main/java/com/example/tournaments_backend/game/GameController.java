package com.example.tournaments_backend.game;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team or league with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping
    public ResponseEntity<GameDTO> addGame(@RequestBody @Valid GameRequest requestBody) throws ServiceException {
        Game gameInDB = gameService.addGame(requestBody);
        GameDTO gameDTO = new GameDTO(gameInDB);
        return ResponseEntity.ok().body(gameDTO);
    }

    @Operation(summary = "Get games", description = "Returns all games or by leagueId. If the leagueId is invalid an empty list is returned.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved games of a league", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping
    public ResponseEntity<List<GameDTO>> getGames(
        @Parameter(description = "The league id") 
        @RequestParam("leagueId") Optional<Long> optionalLeagueId) throws ServiceException
    {
        List<Game> games = gameService.getGames(optionalLeagueId);
        List<GameDTO> gameDTOs = GameDTO.convertGames(games);
        return ResponseEntity.ok(gameDTOs);
    }

    @Operation(summary = "Get a game", description = "Returns a game by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved game information", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping("{gameId}")
    public ResponseEntity<GameDTO> getGame(
        @Parameter(description = "The game id", required = true) @PathVariable("gameId") Long gameId) throws ServiceException
    {
        Game game = gameService.getGameById(gameId);
        GameDTO gameDTO = new GameDTO(game);
        return ResponseEntity.ok().body(gameDTO);
    }

    @Operation(summary = "Delete a game", description = "Returns the deleted game by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deletes game", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @DeleteMapping("{gameId}")
    public ResponseEntity<GameDTO> deleteGame(
        @Parameter(description = "The game id", required = true) @PathVariable("gameId") Long gameId) throws ServiceException
    {
        Game game = gameService.deleteGameById(gameId);
        GameDTO gameDTO = new GameDTO(game);
        return ResponseEntity.ok().body(gameDTO);
    }

    @Operation(summary = "Update a game", description = "Returns the updated game by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates game", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - game is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game, team, or league with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PutMapping("{gameId}")
    public ResponseEntity<GameDTO> updateGame(
        @Parameter(description = "The game id", required = true) @PathVariable("gameId") Long gameId, @RequestBody @Valid GameRequest updatedGame) throws ServiceException
    {
        Game game = gameService.updateGameById(gameId, updatedGame);
        GameDTO gameDTO = new GameDTO(game);
        return ResponseEntity.ok().body(gameDTO);
    }
}
