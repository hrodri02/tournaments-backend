package com.example.tournaments_backend.league;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamService;

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
                            .orElseThrow(() -> new ServiceException(
                                HttpStatus.NOT_FOUND, 
                                ClientErrorKey.LEAGUE_NOT_FOUND, 
                                "League", 
                                "League with given id was not found."
                            ));
                        
        Team team = teamService.getTeamById(teamId);
        league.addTeam(team);
        League leagueInDB = leagueRepository.save(league);
        return leagueInDB;
    }
    
    public List<League> getLeagues(Optional<LeagueStatus> optionalStatus) {
        LocalDate today = LocalDate.now();
        List<League> leagues;
        if (optionalStatus.isPresent()) {
            LeagueStatus status = optionalStatus.get();
            switch (status) {
                case NOT_STARTED:
                    leagues = leagueRepository.findByStartDateAfter(today);
                    break;
                case IN_PROGRESS:
                    leagues = leagueRepository.findInProgressLeagues(today);
                    break;
                case ENDED:
                    leagues = leagueRepository.findEndedLeagues(today);
                    break;
                default:
                    leagues = List.of();
            }
        }
        else {
            leagues = leagueRepository.findAll();
        }
        return leagues;
    }

    public League getLeagueById(Long id) throws ServiceException {
        League league = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new ServiceException(
                                HttpStatus.NOT_FOUND, 
                                ClientErrorKey.LEAGUE_NOT_FOUND, 
                                "League",
                                "The league with the given id was not found."
                            ));
        return league;
    }

    @Transactional    
    public League deleteLeagueById(Long id) throws ServiceException {
        League deletedLeague = getLeagueById(id);
        leagueRepository.deleteById(id);
        return deletedLeague;
    }

    @Transactional
    public League updateLeague(Long id, LeagueRequest leagueRequest) throws ServiceException {
        League oldLeague = leagueRepository
                            .findById(id)
                            .orElseThrow(() -> new ServiceException(
                                HttpStatus.NOT_FOUND, 
                                ClientErrorKey.LEAGUE_NOT_FOUND, 
                                "League",
                                "The league with the given id was not found."
                            ));
        oldLeague.setName(leagueRequest.getName());
        oldLeague.setStartDate(leagueRequest.getStartDate());
        oldLeague.setDurationInWeeks(leagueRequest.getDurationInWeeks());
        oldLeague.setLogoUrl(leagueRequest.getLogoUrl());
        return leagueRepository.save(oldLeague);
    }
}