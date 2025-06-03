package com.example.tournaments_backend.league;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LeagueService {
    private final LeagueRepository leagueRepository;

    public League addLeague(League league) {
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }    
}
