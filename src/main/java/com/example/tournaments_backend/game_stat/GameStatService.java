package com.example.tournaments_backend.game_stat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        // if the game ended or hasn't started throw exception
        if (!game.isActive()) {
            throw new ServiceException(
                ErrorType.GAME_INACTIVE,
                "Game",
                "Game is inactive."
            );
        }

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

        Long gameId = gameStat.getGame().getId();
        Game game = 
            gameRepository
                .findById(gameId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Game", "Game with id = " + gameId + " not found"));
                
        // if the game ended or hasn't started throw exception
        if (!game.isActive()) {
            throw new ServiceException(
                ErrorType.GAME_INACTIVE,
                "Game",
                "Game is inactive."
            );
        }

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

        // if the game ended or hasn't started throw exception
        if (!game.isActive()) {
            throw new ServiceException(
                ErrorType.GAME_INACTIVE,
                "Game",
                "Game is inactive."
            );
        }

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

    @Transactional
    public GameStatUpdateResponse updateGameStats(List<GameStatUpdateRequest> gameStatsToUpdate) {
        Set<Long> gameStatIds = gameStatsToUpdate.stream()
                .map(GameStatUpdateRequest::getId)
                .collect(Collectors.toSet());
        Set<Long> gameIds = gameStatsToUpdate.stream()
                .map(GameStatUpdateRequest::getGameId)
                .collect(Collectors.toSet());
        Set<Long> playerIds = gameStatsToUpdate.stream()
                .map(GameStatUpdateRequest::getPlayerId)
                .collect(Collectors.toSet());

        List<GameStat> existingGameStats = gameStatRepository.findAllById(gameStatIds);
        Map<Long, Game> gamesMap = gameRepository.findAllById(gameIds).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        Map<Long, Player> playersMap = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<Long, GameStat> existingGameStatsMap = existingGameStats.stream()
                .collect(Collectors.toMap(GameStat::getId, Function.identity()));

        List<GameStat> updatedGameStats = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        for (GameStatUpdateRequest request : gameStatsToUpdate) {
            Long gameStatId = request.getId();
            GameStat gameStat = existingGameStatsMap.get(gameStatId);
            if (gameStat == null) {
                // Handle missing game stat: log or add to a failure list
                failures.add(new Failure(gameStatId, "GameStat with ID " + gameStatId + " not found."));
                continue;
            }

            // Get related entities from the pre-fetched maps
            Long gameId = request.getGameId();
            Game game = gamesMap.get(gameId);
            if (game == null) {
                failures.add(new Failure(gameStatId, "Game with ID " + gameId + " not found."));
                continue;
            }

            // if the game ended or hasn't started add to failure list
            if (!game.isActive()) {
                failures.add(new Failure(gameStatId, "Game with ID " + gameId + " is inactive."));
                continue;
            }

            Long playerId = request.getPlayerId();
            Player player = playersMap.get(playerId);
            if (player == null) {
                failures.add(new Failure(gameStatId, "Player with ID " + playerId + " not found."));
                continue;
            }

            gameStat.setGame(game);
            gameStat.setPlayer(player);
            gameStat.setType(request.getType());
            updatedGameStats.add(gameStat);
        }

        List<GameStat> successfulUpdates = gameStatRepository.saveAll(updatedGameStats);
        GameStatUpdateResponse response = new GameStatUpdateResponse(successfulUpdates, failures);
        return response;
    }
}
