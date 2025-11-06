package com.example.tournaments_backend.team_invite;

import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path="/api/v1/team-invites")
@Tag(name = "Team Invites Management", description = "API endpoints for managing team invites")
public class TeamInviteController {
    private final TeamInviteService teamInviteService;

    @Autowired
    public TeamInviteController(TeamInviteService teamInviteService) {
        this.teamInviteService = teamInviteService;
    }

    @GetMapping("/{inviteId}/accept")
    public String showAcceptPage(@PathVariable Long inviteId, Model model) {
        model.addAttribute("inviteId", inviteId);
        return "accept-invite-page"; // This is the name of your HTML template
    }

    @Operation(summary = "Accept an invitation to join a team", description = "Returns the updated team invitation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates team inivitation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AcceptTeamInviteResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - user accepting invite was not invited to join team or invite was revoked",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not Found - invite with given ID was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("/{inviteId}/accept")
    public ResponseEntity<AcceptTeamInviteResponse> acceptInvite(@PathVariable Long inviteId, Authentication authentication) {
        AcceptTeamInviteResponse response = teamInviteService.accepInvite(inviteId, authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Decline an invitation to join a team", description = "Returns the updated team invitation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates team inivitation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamInviteDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - user accepting invite was not invited to join team or invite was revoked",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not Found - invite with given ID was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("/{inviteId}/decline")
    public ResponseEntity<TeamInviteDTO> declineInvite(@PathVariable Long inviteId, Authentication authentication) {
        TeamInvite invite = teamInviteService.declineInvite(inviteId, authentication);
        TeamInviteDTO inviteDTO = new TeamInviteDTO(invite);
        return ResponseEntity.ok(inviteDTO);
    }

    @Operation(summary = "Revoke an invitation to join a team", description = "Returns the updated team invitation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates team inivitation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamInviteDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - user revoking invite does own the team or invite was already accepted or declined",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not Found - invite with given ID was not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("/{inviteId}/revoke")
    public ResponseEntity<TeamInviteDTO> revokeInvite(@PathVariable Long inviteId, Authentication authentication) {
        TeamInvite invite = teamInviteService.revokeInvite(inviteId, authentication);
        TeamInviteDTO inviteDTO = new TeamInviteDTO(invite);
        return ResponseEntity.ok(inviteDTO);
    }
}
