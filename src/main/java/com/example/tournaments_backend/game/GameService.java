package com.example.tournaments_backend.game;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
}
