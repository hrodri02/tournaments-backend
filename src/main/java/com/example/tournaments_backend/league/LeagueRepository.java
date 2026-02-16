package com.example.tournaments_backend.league;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {
    @EntityGraph(attributePaths = {"teams", "teams.players"})
    @NonNull
    List<League> findAll();

    List<League> findByStartDateAfter(LocalDate date);

    @Query(value ="SELECT * FROM league l WHERE " +
           "l.start_date <= :today AND " +
           ":today <= l.start_date + (l.duration_in_weeks * INTERVAL '1 week')", nativeQuery = true)
    List<League> findInProgressLeagues(@Param("today") LocalDate today);

    @Query(value = "SELECT * FROM league l WHERE " +
           ":today > l.start_date + (l.duration_in_weeks * INTERVAL '1 week')", nativeQuery = true)
    List<League> findEndedLeagues(@Param("today") LocalDate today);
}
