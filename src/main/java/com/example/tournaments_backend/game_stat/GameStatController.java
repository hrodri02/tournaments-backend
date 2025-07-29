package com.example.tournaments_backend.game_stat;

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

    @Operation(summary = "Get game stats", description = "Returns all game stats or by gameId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved stats", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStatDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping
    public ResponseEntity<List<GameStatDTO>> getGameStats(
        @Parameter(description = "The game id") @RequestParam("gameId") Optional<Long> optionalGameId) throws ServiceException
    {
        List<GameStat> gameStats = gameStatService.getGameStatsByGameId(optionalGameId);
        List<GameStatDTO> gameStatDTOs = GameStatDTO.convert(gameStats);
        return ResponseEntity.ok(gameStatDTOs);
    }

    @Operation(summary = "Delete a game stat", description = "Returns the deleted game stat by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deletes game stat", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStatDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game stat with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @DeleteMapping("{gameStatId}")
    public ResponseEntity<GameStatDTO> deleteGameStat(
        @Parameter(description = "The game stat id", required = true) @PathVariable("gameStatId") Long gameStatId) throws ServiceException
    {
        GameStat gameStat = gameStatService.deleteGameStatById(gameStatId);
        GameStatDTO gameStatDTO = new GameStatDTO(gameStat);
        return ResponseEntity.ok(gameStatDTO);
    }

    @Operation(summary = "Update a game stat", description = "Returns the updated game stat by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates game stat", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStatDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - game stat is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - game stat, game, or player with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PutMapping("{gameStatId}")
    public ResponseEntity<GameStatDTO> updateGameStat(
        @Parameter(description = "The game stat id", required = true) @PathVariable("gameStatId") Long gameStatId, @RequestBody @Valid GameStatRequest gameStatRequest) throws ServiceException
    {
        GameStat gameStat = gameStatService.updateGameStatById(gameStatId, gameStatRequest);
        GameStatDTO gameStatDTO = new GameStatDTO(gameStat);
        return ResponseEntity.ok(gameStatDTO);
    }
}
