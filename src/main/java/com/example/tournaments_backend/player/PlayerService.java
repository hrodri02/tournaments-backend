package com.example.tournaments_backend.player;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;
}
