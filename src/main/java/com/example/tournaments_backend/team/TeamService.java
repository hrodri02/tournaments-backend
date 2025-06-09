package com.example.tournaments_backend.team;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.TeamNotFoundException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;

    public Team addTeam(Team team) {
        Team teamInDB = teamRepository.save(team);
        return teamInDB;
    }

    public Team getTeamById(Long id) throws TeamNotFoundException {
        Team team = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new TeamNotFoundException("Team with given id not found."));
        return team;
    }

    public void deleteTeamById(Long id) throws TeamNotFoundException {
        teamRepository.deleteById(id);
    }
}
