package com.example.tournaments_backend.game_stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStatRepository extends JpaRepository<GameStat, Long> {
    
}
