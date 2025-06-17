package com.example.tournaments_backend.player;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import com.example.tournaments_backend.exception.ServiceException;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public Player save(Player player) throws ServiceException {
        return playerRepository.save(player);
    }
}
