package com.example.tournaments_backend.player;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

import com.example.tournaments_backend.exception.ErrorType;
import com.example.tournaments_backend.exception.ServiceException;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public Player save(Player player) {
        return playerRepository.save(player);
    }

    public Player getPlayerById(Long id) throws ServiceException {
        Player player = 
            playerRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));
        return player;
    }

    public void deletePlayerById(Long id) {
        playerRepository.deleteById(id);
    }
}
