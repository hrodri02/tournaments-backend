package com.example.tournaments_backend.league;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.ErrorType;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LeagueService {
    private final TeamService teamService;
    private final LeagueRepository leagueRepository;

    public League addLeague(LeagueRequest leagueRequest) {
        League league = new League(leagueRequest);
        return leagueRepository.save(league);
    }

    @Transactional
    public League addTeamToLeague(Long leagueId, Long teamId) throws ServiceException {
        League league = leagueRepository
                            .findById(leagueId)
                            .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "League", "League with given id was not found."));
        Team team = teamService.getTeamById(teamId);
        league.addTeam(team);
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }
    
    public List<League> getLeagues() {
        return leagueRepository.findAll();
    }

    public League getLeagueById(Long id) throws ServiceException {
        League league = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "League","The league with the given id was not found."));
        return league;
    }

    public void deleteLeagueById(Long id) {
        leagueRepository.deleteById(id);
    }

    public League updateLeague(Long id, League updatedLeauge) throws ServiceException {
        League oldLeague = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "League","The league with the given id was not found."));
        oldLeague.setName(updatedLeauge.getName());
        oldLeague.setStartDate(updatedLeauge.getStartDate());
        oldLeague.setDurationInWeeks(updatedLeauge.getDurationInWeeks());
        
        League leagueInDB = leagueRepository.save(oldLeague);
        return leagueInDB;
    }
}
