package com.example.tournaments_backend.league;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.LeagueNotFoundException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LeagueService {
    private final LeagueRepository leagueRepository;

    public League addLeague(League league) {
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }
    
    public List<League> getLeagues() {
        return leagueRepository.findAll();
    }

    public League getLeagueById(Long id) throws LeagueNotFoundException {
        League league = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new LeagueNotFoundException("League not found."));
        return league;
    }

    public void deleteLeagueById(Long id) throws LeagueNotFoundException {
        boolean leagueExists = leagueRepository.existsById(id);
        if (!leagueExists) {
            throw new LeagueNotFoundException("League not found.");
        }

        leagueRepository.deleteById(id);
    }

    public League updateLeague(Long id, League updatedLeauge) throws LeagueNotFoundException {
        League oldLeague = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new LeagueNotFoundException("League not found."));
        oldLeague.setName(updatedLeauge.getName());
        oldLeague.setStartDate(updatedLeauge.getStartDate());
        oldLeague.setDurationInWeeks(updatedLeauge.getDurationInWeeks());
        
        League leagueInDB = leagueRepository.save(oldLeague);
        return leagueInDB;
    }
}
