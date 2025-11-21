package com.example.tournaments_backend.league_application;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByTeamId(Long teamId);
    List<Application> findAllByLeagueId(Long leagueId);
}
