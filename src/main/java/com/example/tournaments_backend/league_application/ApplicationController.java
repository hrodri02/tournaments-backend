package com.example.tournaments_backend.league_application;

import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.game_stat.GameStatDTO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/applications")
@Tag(name = "League Application Management", description = "API endpoints for managing league applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Update an application", description = "Returns the updated application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates application", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStatDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - application request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - client does not have admin role",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - application with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PutMapping("{applicationId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApplicationDTO> updateApplication(
        @PathVariable("applicationId") Long applicationId,
        @RequestBody @Valid UpdateApplicationRequest request) 
    {
        Application application = applicationService.updateApplication(applicationId, request);
        ApplicationDTO applicationDTO = new ApplicationDTO(application);
        return ResponseEntity.ok(applicationDTO);
    }

    @Operation(summary = "Get applications", description = "Returns the applications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully returns applications", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - application request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - client does not own the team they want to retrieve the applications for",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping
    public ResponseEntity<List<ApplicationDTO>> getApplications(
        @RequestParam("teamId") Optional<Long> optionalTeamId,
        @RequestParam("leagueId") Optional<Long> optionalLeagueId,
        Authentication authentication
    )
    {
        List<Application> applications = applicationService.getApplications(optionalTeamId, optionalLeagueId, authentication);
        List<ApplicationDTO> applicationDTOs = ApplicationDTO.convert(applications);
        return ResponseEntity.ok(applicationDTOs);
    }
}