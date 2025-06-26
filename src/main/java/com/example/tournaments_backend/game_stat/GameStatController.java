package com.example.tournaments_backend.game_stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path="/api/v1/gamestats")
@Tag(name = "Game Stats Management", description = "API endpoints for managing game stats")
public class GameStatController {
    private final GameStatService gameStatService;

    @Autowired
    public GameStatController(GameStatService gameStatService) {
        this.gameStatService = gameStatService;
    }
}
