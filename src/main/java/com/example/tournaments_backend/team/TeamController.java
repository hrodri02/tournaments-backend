package com.example.tournaments_backend.team;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.team_invite.CreateTeamInviteRequest;
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
@RequestMapping(path="/api/v1/teams")
@Tag(name = "Team Management", description = "API endpoints for managing teams")
public class TeamController {
    private final TeamService teamService;
    private final TeamInviteService teamInviteService;

    @Autowired
    public TeamController(TeamService teamService, TeamInviteService teamInviteService) {
        this.teamService = teamService;
        this.teamInviteService = teamInviteService;
    }

    @Operation(summary = "Get a teams a user belongs to", description = "Returns a teams")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved teams information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = GetTeamsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping
    public ResponseEntity<GetTeamsResponse> getTeams(Authentication authentication)
    {
        GetTeamsResponse response = teamService.getTeams(authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a team", description = "Returns the team created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a team", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - team is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping
    public ResponseEntity<TeamDTO> addTeam(@RequestBody @Valid TeamRequest teamRequest, Authentication authentication) {
        TeamDTO teamDTO = teamService.addTeam(teamRequest, authentication);
        return ResponseEntity.ok(teamDTO);
    }

    @Operation(summary = "Invite a player to a team", description = "Returns the team invitation created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a team inivitation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamInviteDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - team invite is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - user sending invite does not own the team",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("{teamId}/invites")
    public ResponseEntity<TeamInviteDTO> addTeamInvite(
        @RequestBody @Valid CreateTeamInviteRequest request,
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId,
        Authentication authentication) 
    {
        TeamInvite invite = teamInviteService.addTeamInvite(request, teamId, authentication);
        TeamInviteDTO inviteDTO = new TeamInviteDTO(invite);
        return ResponseEntity.ok(inviteDTO);
    }

    @Operation(summary = "Get a team", description = "Returns a team by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved team information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @GetMapping("{teamId}")
    public ResponseEntity<TeamDTO> getTeam(
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId) throws ServiceException 
    {
        Team team = teamService.getTeamById(teamId);
        TeamDTO teamDTO = new TeamDTO(team);
        return ResponseEntity.ok(teamDTO);
    }

    @Operation(summary = "Delete a team", description = "Returns deleted team by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deletes team", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @DeleteMapping("{teamId}")
    public ResponseEntity<TeamDTO> deleteTeam(
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId) throws ServiceException 
    {
        Team deletedTeam = teamService.deleteTeamById(teamId);
        TeamDTO teamDTO = new TeamDTO(deletedTeam.getId(), deletedTeam.getName());
        return ResponseEntity.ok(teamDTO);
    }

    @Operation(summary = "Update a team", description = "Returns the updated team")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated team", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - team is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team with given id not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PutMapping("{teamId}")
    public ResponseEntity<TeamDTO> updateTeam(
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId, @RequestBody @Valid TeamRequest teamRequest) throws ServiceException 
    {
        Team team = teamService.updateTeam(teamId, teamRequest);
        TeamDTO teamDTO = new TeamDTO(team);
        return ResponseEntity.ok(teamDTO);
    }

    @Operation(summary = "Add a player to a team", description = "Returns the updated team")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully added player to team", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team and/or player with given id not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("{teamId}/players/{playerId}")
    public ResponseEntity<TeamDTO> addPlayerToTeam(
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId, 
        @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId) 
    {
        TeamDTO teamDTO = teamService.addPlayerToTeam(playerId, teamId);
        return ResponseEntity.ok(teamDTO);
    }

    @Operation(summary = "Remove a player to a team", description = "Returns the updated team")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully removed player from team", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "404", description = "Not found - team and/or player with given id not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @DeleteMapping("{teamId}/players/{playerId}")
    public ResponseEntity<TeamDTO> deletePlayerFromTeam(
        @Parameter(description = "The team id", required = true) @PathVariable("teamId") Long teamId, 
        @Parameter(description = "The player id", required = true) @PathVariable("playerId") Long playerId) 
    {
        TeamDTO teamDTO = teamService.deletePlayerFromTeam(playerId, teamId);
        return ResponseEntity.ok(teamDTO);
    }
}
