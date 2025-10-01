package com.example.tournaments_backend.player;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> finAllByEmailIn(List<String> emails);
    Optional<Player> findByEmail(String email);
}
