package com.example.tournaments_backend.game_stat;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.exception.ErrorType;
import com.example.tournaments_backend.game.Game;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.game.GameRepository;
import com.example.tournaments_backend.player.PlayerRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GameStatService {
    private final GameStatRepository gameStatRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public GameStat addGameStat(GameStatRequest gameStatRequest) throws ServiceException {
        GameStat gameStat = new GameStat(gameStatRequest);
        Long gameId = gameStatRequest.getGameId();
        Game game = 
            gameRepository
                .findById(gameId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game", "Game with id = " + gameId + " not found"));

        Long playerId = gameStatRequest.getPlayerId();
        Player player =
            playerRepository
                .findById(playerId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with id = " + playerId + " not found"));
        gameStat.setGame(game);
        gameStat.setPlayer(player);   
        return gameStatRepository.save(gameStat); 
    }

    @Transactional
    public List<GameStat> getGameStatsByGameId(Optional<Long> optionalGameId) throws ServiceException {
        List<GameStat> gameStats;
        
        if (optionalGameId.isPresent()) {
            Long gameId = optionalGameId.get();
            gameRepository
                .findById(gameId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game", "Game with id = " + gameId + " not found"));
            gameStats = gameStatRepository.findByGame_Id(gameId);
        } 
        else {
            gameStats = gameStatRepository.findAll();
        }
        
        return gameStats;
    }

    @Transactional
    public GameStat deleteGameStatById(Long id) throws ServiceException {
        GameStat gameStat = 
            gameStatRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game stat", "GameStat with id = " + id + " not found"));
        gameStatRepository.deleteById(id);
        return gameStat;
    }

    @Transactional
    public GameStat updateGameStatById(Long id, GameStatRequest gameStatRequest) throws ServiceException {
        GameStat gameStat = 
            gameStatRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game stat", "GameStat with id = " + id + " not found"));
        Long gameId = gameStatRequest.getGameId();
        Game game = 
            gameRepository
                .findById(gameId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game", "Game with id = " + gameId + " not found"));

        Long playerId = gameStatRequest.getPlayerId();
        Player player =
            playerRepository
                .findById(playerId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with id = " + playerId + " not found"));
        gameStat.setGame(game);
        gameStat.setPlayer(player);
        gameStat.setType(gameStatRequest.getType());
        gameStat.setCreatedAt(gameStatRequest.getCreatedAt());
        return gameStatRepository.save(gameStat);
    }
}
