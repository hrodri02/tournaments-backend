package com.example.tournaments_backend.game;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.ErrorType;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.league.LeagueRepository;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    public Game addGame(CreateGameRequest gameRequest) throws ServiceException {
        Game game = new Game(gameRequest);
        Long homeTeamId = gameRequest.getHomeTeamId();
        Long awayTeamId = gameRequest.getAwayTeamId();
        Long leagueId = gameRequest.getLeagueId();
        Team homeTeam = 
            teamRepository
                .findById(homeTeamId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found"));
        Team awayTeam = 
            teamRepository
                .findById(awayTeamId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found"));
        League league = 
            leagueRepository
                .findById(leagueId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "League", "League with given id not found"));
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setLeague(league);
        return gameRepository.save(game);
    }
}
