package com.example.tournaments_backend.league;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.example.tournaments_backend.league_application.Application;
import com.example.tournaments_backend.league_application.ApplicationDTO;
import com.example.tournaments_backend.league_application.ApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/leagues")
@Tag(name = "League Management", description = "API endpoints for managing leagues")
public class LeagueController {
    private final LeagueService leagueService;
    private final ApplicationService applicationService;

    @Autowired
    public LeagueController(LeagueService leagueService, ApplicationService applicationService) {
        this.leagueService = leagueService;
        this.applicationService = applicationService;
    }

    @Operation(summary = "Create a league", description = "Returns the league created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a league", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - league is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping
    public ResponseEntity<LeagueDTO> addLeague(@RequestBody @Valid LeagueRequest leagueRequest) {
        League leagueInDB = leagueService.addLeague(leagueRequest);
        LeagueDTO leagueDTO = new LeagueDTO(leagueInDB);
        return ResponseEntity.ok().body(leagueDTO);
    }

    @Operation(summary = "Add an existing team to a league", description = "Returns the updated league")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates a league",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - league or team with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @PostMapping("{leagueId}/teams/{teamId}")
    public ResponseEntity<LeagueDTO> addTeamToLeague(@PathVariable("leagueId") Long leagueId, @PathVariable("teamId") Long teamId) throws ServiceException {
        League league = leagueService.addTeamToLeague(leagueId, teamId);
        LeagueDTO leagueDTO = new LeagueDTO(league);
        return ResponseEntity.ok().body(leagueDTO);
    }

    @Operation(summary = "Get all the leagues", description = "Returns all the leagues")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved leagues", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping
    public ResponseEntity<List<LeagueDTO>> getLeagues(
        @Parameter(description = "status can be: NOT_STARTED, IN_PROGRESS, or ENDED")
        @RequestParam("status") Optional<LeagueStatus> optionalStatus
    )
    {
        List<League> leagues = leagueService.getLeagues(optionalStatus);
        List<LeagueDTO> leagueDTOs = LeagueDTO.convertLeagues(leagues);
        return ResponseEntity.ok().body(leagueDTOs);
    }

    @Operation(summary = "Get a league", description = "Returns a league by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved league information", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - league with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping("{leagueId}")
    public ResponseEntity<LeagueDTO> getLeague(@PathVariable("leagueId") Long leagueId) throws ServiceException {
        League league = leagueService.getLeagueById(leagueId);
        LeagueDTO leagueDTO = new LeagueDTO(league);
        return ResponseEntity.ok(leagueDTO);
    }

    @Operation(summary = "Delete a league", description = "Returns the deleted league by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deletes league", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - league with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @DeleteMapping("{leagueId}")
    public ResponseEntity<LeagueDTO> deleteLeague(@PathVariable("leagueId") Long leagueId) throws ServiceException {
        League deletedLeague = leagueService.deleteLeagueById(leagueId);
        LeagueDTO leagueDTO = 
            new LeagueDTO(deletedLeague.getId(), 
                          deletedLeague.getName(),
                          deletedLeague.getStartDate(),
                          deletedLeague.getDurationInWeeks());
        return ResponseEntity.ok(leagueDTO);
    }

    @Operation(summary = "Update a league", description = "Returns the updated league by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates league", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - league is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - league, team, or league with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PutMapping("{leagueId}")
    public ResponseEntity<LeagueDTO> updateLeague(@PathVariable("leagueId") Long leagueId, @RequestBody @Valid LeagueRequest leagueRequest) throws ServiceException {
        League leagueInDB = leagueService.updateLeague(leagueId, leagueRequest);
        LeagueDTO leagueDTO = new LeagueDTO(leagueInDB);
        return ResponseEntity.ok(leagueDTO);
    }

    @Operation(summary = "Apply to join a league", description = "Returns the application created")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully creates an application", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - the team is already part of the league",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - the client does not own the team",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - league or team with given ID not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("{leagueId}/applications")
    public ResponseEntity<ApplicationDTO> applyToLeague(
        @PathVariable("leagueId") Long leagueId, 
        @RequestBody @Valid JoinLeagueRequest request,
        Authentication authentication
    ) throws ServiceException {
        Application application = applicationService.addApplication(leagueId, request, authentication);
        ApplicationDTO applicationDTO = new ApplicationDTO(application);
        return ResponseEntity.ok(applicationDTO);
    }
}