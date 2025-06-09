package com.example.tournaments_backend.team;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.TeamNotFoundException;

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
    public ResponseEntity<Team> getTeam(@PathVariable("teamId") Long teamId) throws TeamNotFoundException {
        Team team = teamService.getTeamById(teamId);
        return ResponseEntity.ok().body(team);
    }

    @DeleteMapping("{teamId}")
    public ResponseEntity<Team> deleteTeam(@PathVariable("teamId") Long teamId) throws TeamNotFoundException {
        Team deletedTeam = teamService.getTeamById(teamId);
        teamService.deleteTeamById(teamId);
        return ResponseEntity.ok().body(deletedTeam);
    }
}
