package com.example.tournaments_backend.team;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.exception.ErrorType;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;

    public Team addTeam(Team team) {
        Team teamInDB = teamRepository.save(team);
        return teamInDB;
    }

    public Team getTeamById(Long id) throws ServiceException {
        Team team = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found."));
        return team;
    }

    public void deleteTeamById(Long id) {
        teamRepository.deleteById(id);
    }

    public Team updateTeam(Long id, Team updatedTeam) throws ServiceException {
        Team oldTeam = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team","Team with given id not found."));
        oldTeam.setName(updatedTeam.getName());
        
        Team teamInDB = teamRepository.save(oldTeam);
        return teamInDB;
    }
}
