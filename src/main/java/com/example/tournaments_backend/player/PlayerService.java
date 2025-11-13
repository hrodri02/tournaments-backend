package com.example.tournaments_backend.player;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public PlayerDTO getPlayerDTOById(Long id) throws ServiceException {
        Player player = 
            playerRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));
        PlayerDTO playerDTO = new PlayerDTO(player);
        return playerDTO;
    }

    public Player getPlayerById(Long id) throws ServiceException {
        Player player = 
            playerRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));
        return player;
    }

    public Player getPlayerByEmail(String email) throws ServiceException {
        Player player = 
            playerRepository
                .findByEmail(email)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));
        return player;
    }

    public void deletePlayerById(Long id) {
        playerRepository.deleteById(id);
    }

    @Transactional
    public PlayerDTO updatePlayer(Long id, PlayerDTO updatedPlayer) throws ServiceException {
        Player player = 
            playerRepository
                .findById(id)
                .orElseThrow(() ->
                    new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found.")
                );
        player.setFirstName(updatedPlayer.getFirstName());
        player.setLastName(updatedPlayer.getLastName());
        player.setEmail(updatedPlayer.getEmail());
        player.setPosition(updatedPlayer.getPosition());
        Player playerInDB = playerRepository.save(player);
        PlayerDTO playerDTO = new PlayerDTO(playerInDB);
        return playerDTO;
    }

    public List<Player> getAllPlayersByEmail(List<String> emails) {
        return playerRepository.findAllByEmailIn(emails);
    }

    public List<Player> getAllPlayersByIds(List<Long> ids) {
        return playerRepository.findAllById(ids);
    }
}
