package com.example.tournaments_backend.league;

import java.util.List;

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
@RequestMapping(path="/api/v1/leagues")
@Tag(name = "League Management", description = "API endpoints for managing leagues")
public class LeagueController {
    private final LeagueService leagueService;

    @Autowired
    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
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

    @PostMapping("{leagueId}/teams/{teamId}")
    public ResponseEntity<League> addTeamToLeague(@PathVariable("leagueId") Long leagueId, @PathVariable("teamId") Long teamId) throws ServiceException {
        League league = leagueService.addTeamToLeague(leagueId, teamId);
        return ResponseEntity.ok().body(league);
    }

    @GetMapping
    public ResponseEntity<List<League>> getLeagues() {
        List<League> leagues = leagueService.getLeagues();
        return ResponseEntity.ok().body(leagues);
    }

    @GetMapping("{leagueId}")
    public ResponseEntity<League> getLeague(@PathVariable("leagueId") Long leagueId) throws ServiceException {
        League league = leagueService.getLeagueById(leagueId);
        return ResponseEntity.ok().body(league);
    }

    @DeleteMapping("{leagueId}")
    public ResponseEntity<League> deleteLeague(@PathVariable("leagueId") Long leagueId) throws ServiceException {
        League deletedLeague = leagueService.getLeagueById(leagueId);
        leagueService.deleteLeagueById(leagueId);
        return ResponseEntity.ok().body(deletedLeague);
    }

    @PutMapping("{leagueId}")
    public ResponseEntity<League> updateLeague(@PathVariable("leagueId") Long leagueId, @RequestBody @Valid League updatedLeauge) throws ServiceException {
        League leagueInDB = leagueService.updateLeague(leagueId, updatedLeauge);
        return ResponseEntity.ok().body(leagueInDB);
    }
}