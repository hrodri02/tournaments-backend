package com.example.tournaments_backend.league;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.LeagueNotFoundException;
import com.example.tournaments_backend.exception.TeamNotFoundException;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LeagueService {
    private final TeamService teamService;
    private final LeagueRepository leagueRepository;

    public League addLeague(League league) {
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }

    @Transactional
    public League addTeamToLeague(Long leagueId, Long teamId) throws LeagueNotFoundException, TeamNotFoundException {
        League league = leagueRepository
                            .findById(leagueId)
                            .orElseThrow(() -> new LeagueNotFoundException("League with given id was not found."));
        Team team = teamService.getTeamById(teamId);
        league.addTeam(team);
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }
    
    public List<League> getLeagues() {
        return leagueRepository.findAll();
    }

    public League getLeagueById(Long id) throws LeagueNotFoundException {
        League league = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new LeagueNotFoundException("The league with the given id was not found."));
        return league;
    }

    public void deleteLeagueById(Long id) throws LeagueNotFoundException {
        leagueRepository.deleteById(id);
    }

    public League updateLeague(Long id, League updatedLeauge) throws LeagueNotFoundException {
        League oldLeague = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new LeagueNotFoundException("The league with the given id was not found."));
        oldLeague.setName(updatedLeauge.getName());
        oldLeague.setStartDate(updatedLeauge.getStartDate());
        oldLeague.setDurationInWeeks(updatedLeauge.getDurationInWeeks());
        
        League leagueInDB = leagueRepository.save(oldLeague);
        return leagueInDB;
    }
}
