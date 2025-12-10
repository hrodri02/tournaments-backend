package com.example.tournaments_backend.game;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ClientErrorKey;
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

    @Transactional
    public Game addGame(GameRequest gameRequest) throws ServiceException {
        Game game = new Game(gameRequest);
        Long homeTeamId = gameRequest.getHomeTeamId();
        Long awayTeamId = gameRequest.getAwayTeamId();
        Long leagueId = gameRequest.getLeagueId();

        Team homeTeam = teamRepository
                .findById(homeTeamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team", 
                    "Home Team with id = " + homeTeamId + " not found"
                ));
        Team awayTeam = teamRepository
                .findById(awayTeamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team", 
                    "Away Team with id = " + awayTeamId + " not found"
                ));
        League league = 
            leagueRepository
                .findById(leagueId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.LEAGUE_NOT_FOUND, 
                    "League", 
                    "League with id = " + leagueId + " not found"
                ));
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setLeague(league);
        return gameRepository.save(game);
    }

    public List<Game> getGames(Optional<Long> optionalLeagueId) throws ServiceException {
        if (optionalLeagueId.isPresent()) {
            Long leagueId = optionalLeagueId.get();
            return gameRepository.findByLeagueId(leagueId);
        }
        return gameRepository.findAll();
    }

    public Game getGameById(Long gameId) throws ServiceException {
        Game game = gameRepository
                .findById(gameId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.GAME_NOT_FOUND, 
                    "Game", 
                    "Game with id = " + gameId + " not found"
                ));
        return game;
    }

    @Transactional
    public Game deleteGameById(Long gameId) throws ServiceException {
        Game game = getGameById(gameId);
        gameRepository.deleteById(gameId);
        return game;
    }

    @Transactional
    public Game updateGameById(Long gameId, GameRequest updatedGame) throws ServiceException {
        Game gameInDB = getGameById(gameId);
        gameInDB.setAddress(updatedGame.getAddress());
        gameInDB.setDurationInMinutes(updatedGame.getDurationInMinutes());
        gameInDB.setGameDateTime(updatedGame.getGameDateTime());

        Long homeTeamId = updatedGame.getHomeTeamId();
        Team homeTeam = teamRepository
                .findById(homeTeamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team", 
                    "Home Team with id = " + homeTeamId + " not found"
                ));
        
        Long awayTeamId = updatedGame.getAwayTeamId();
        Team awayTeam = teamRepository
                .findById(awayTeamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team", 
                    "Away Team with id = " + awayTeamId + " not found"
                ));
        
        Long leagueId = updatedGame.getLeagueId();
        League league = leagueRepository
                .findById(leagueId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.LEAGUE_NOT_FOUND, 
                    "League", 
                    "League with id = " + leagueId + " not found"
                ));
        
        gameInDB.setHomeTeam(homeTeam);
        gameInDB.setAwayTeam(awayTeam);
        gameInDB.setLeague(league);
        return gameRepository.save(gameInDB);
    }
}