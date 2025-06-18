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

import com.example.tournaments_backend.exception.ServiceException;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path="/api/v1/teams")
public class TeamController {
    private final TeamService teamService;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<Team> addTeam(@RequestBody @Valid Team team) {
        Team teamInDB = teamService.addTeam(team);
        return ResponseEntity.ok().body(teamInDB);
    }

    @GetMapping("{teamId}")
    public ResponseEntity<Team> getTeam(@PathVariable("teamId") Long teamId) throws ServiceException {
        Team team = teamService.getTeamById(teamId);
        return ResponseEntity.ok().body(team);
    }

    @DeleteMapping("{teamId}")
    public ResponseEntity<Team> deleteTeam(@PathVariable("teamId") Long teamId) throws ServiceException {
        Team deletedTeam = teamService.getTeamById(teamId);
        teamService.deleteTeamById(teamId);
        return ResponseEntity.ok().body(deletedTeam);
    }

    @PutMapping("{teamId}")
    public ResponseEntity<Team> updateTeam(@PathVariable("teamId") Long teamId, @RequestBody @Valid Team updatedTeam) throws ServiceException {
        Team team = teamService.updateTeam(teamId, updatedTeam);
        return ResponseEntity.ok().body(team);
    }

    @PostMapping("{teamId}/players/{playerId}")
    public ResponseEntity<TeamDTO> addPlayerToTeam(@PathVariable("teamId") Long teamId, @PathVariable("playerId") Long playerId) {
        TeamDTO teamDTO = teamService.addPlayerToTeam(playerId, teamId);
        return ResponseEntity.ok().body(teamDTO);
    }

    @DeleteMapping("{teamId}/players/{playerId}")
    public ResponseEntity<TeamDTO> deletePlayerFromTeam(@PathVariable("teamId") Long teamId, @PathVariable("playerId") Long playerId) {
        TeamDTO teamDTO = teamService.deletePlayerFromTeam(playerId, teamId);
        return ResponseEntity.ok().body(teamDTO);
    }
}
