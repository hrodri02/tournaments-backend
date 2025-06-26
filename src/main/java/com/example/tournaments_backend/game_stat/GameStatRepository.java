package com.example.tournaments_backend.game_stat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStatRepository extends JpaRepository<GameStat, Long> {
    List<GameStat> findByGame_Id(Long gameId);
}
