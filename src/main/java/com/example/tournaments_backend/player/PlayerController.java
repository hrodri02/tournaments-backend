package com.example.tournaments_backend.player;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.team_invite.TeamInvite;
import com.example.tournaments_backend.team_invite.TeamInviteDTO;
import com.example.tournaments_backend.team_invite.TeamInviteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final TeamInviteService teamInviteService;

    @Autowired
    public PlayerController(PlayerService playerService, TeamInviteService teamInviteService) {
        this.playerService = playerService;
        this.teamInviteService = teamInviteService;
    }

    @Operation(summary = "Get a player", description = "Returns a player by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved player information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found - Player with given id not found.", content = @Content)
    })
    @GetMapping("{playerId}")
    public ResponseEntity<PlayerDTO> getPlayer(
        @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId) throws ServiceException 
    {
        PlayerDTO playerDTO = playerService.getPlayerDTOById(playerId);
        return ResponseEntity.ok(playerDTO);
    }

    @Operation(summary = "Get a player's team invites", description = "Returns a team invites by playerID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved team invite information", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamInviteDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Current user does not match player ID", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - Player with given id not found", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping("{playerId}/invites")
    public ResponseEntity<List<TeamInviteDTO>> getPlayerInvites(
        @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId,
        Authentication authentication
        ) throws ServiceException
    {
        List<TeamInvite> invites = teamInviteService.getAllInvitesByPlayerId(playerId, authentication);
        List<TeamInviteDTO> inviteDTOs = TeamInviteDTO.convert(invites);
        return ResponseEntity.ok(inviteDTOs);
    }

    @Operation(summary = "Delete a player", description = "Returns a deleted player by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted player information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found - Player with given id not found.", content = @Content)
    })
    @DeleteMapping("{playerId}")
    public ResponseEntity<PlayerDTO> deletePlayer(
            @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId) throws ServiceException 
    {
        PlayerDTO playerDTO = playerService.getPlayerDTOById(playerId);
        playerService.deletePlayerById(playerId);
        return ResponseEntity.ok().body(playerDTO);
    }

    @Operation(summary = "Update a player", description = "Returns the updated player by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated player information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerDTO.class))),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid player", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found - Player with given id not found.", content = @Content)
    })
    @PutMapping("{playerId}")
    public ResponseEntity<PlayerDTO> updatePlayer(
        @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId, @RequestBody @Valid PlayerDTO playerDTO) throws ServiceException 
    {
        PlayerDTO updatedPlayerDTO = playerService.updatePlayer(playerId, playerDTO);
        return ResponseEntity.ok().body(updatedPlayerDTO);
    }
}
