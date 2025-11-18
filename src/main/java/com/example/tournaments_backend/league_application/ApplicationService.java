package com.example.tournaments_backend.league_application;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ErrorType;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.JoinLeagueRequest;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueRepository;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    @Transactional
    public Application addApplication(
        Long leagueId, 
        JoinLeagueRequest request, 
        Authentication authentication) throws ServiceException
    {
        Long teamId = request.getTeamId();
        Team team = teamRepository
                        .findById(teamId)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found."));
        
        String currentUserEmail = authentication.getName();
        String ownerEmail = team.getOwner().getEmail();
        // check if current user owns the team
        if (!currentUserEmail.equals(ownerEmail)) {
            throw new ServiceException(ErrorType.FORBIDDEN, "League Application", "Current user does not own this team.");
        }

        League league = leagueRepository
                            .findById(leagueId)
                            .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "League", "League with given id not found."));
        // check if team is already part of the league
        Set<Team> teamsInLeague = league.getTeams();
        if (teamsInLeague.contains(team)) {
            String teamName = team.getName();
            String leagueName = league.getName();
            throw new ServiceException(ErrorType.ALREADY_EXISTS, "League Application", teamName + " is already part of " + leagueName + ".");
        }
        
        Application application = 
            new Application(team, league, request.getCreatedAt(), ApplicationStatus.PENDING);
        return applicationRepository.save(application);
    }
}