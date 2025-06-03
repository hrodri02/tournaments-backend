package com.example.tournaments_backend.league;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/api/v1/leagues")
public class LeagueController {
    private final LeagueService leagueService;

    @Autowired
    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping
    public ResponseEntity<League> addLeague(@RequestBody League league) {
        League leagueInDB =leagueService.addLeague(league);
        return ResponseEntity.ok().body(leagueInDB);
    }

    @GetMapping
    public ResponseEntity<List<League>> getLeagues() {
        List<League> leagues = leagueService.getLeagues();
        return ResponseEntity.ok().body(leagues);
    }
}