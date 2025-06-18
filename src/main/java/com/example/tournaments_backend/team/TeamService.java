package com.example.tournaments_backend.team;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerService;
import com.example.tournaments_backend.exception.ErrorType;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final PlayerService playerService;

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

    @Transactional
    public Team updateTeam(Long id, Team updatedTeam) throws ServiceException {
        Team oldTeam = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team","Team with given id not found."));
        oldTeam.setName(updatedTeam.getName());
        
        Team teamInDB = teamRepository.save(oldTeam);
        return teamInDB;
    }

    @Transactional
    public TeamDTO addPlayerToTeam(Long playerId, Long teamId) throws ServiceException {
        Player player = playerService.getPlayerById(playerId);
        Team team = 
            teamRepository
                .findById(teamId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team","Team with given id not found."));
        team.addPlayer(player);
        Team teamInDB = teamRepository.save(team);
        TeamDTO teamDTO = new TeamDTO(teamInDB);
        return teamDTO;
    }

    @Transactional
    public TeamDTO deletePlayerFromTeam(Long playerId, Long teamId) throws ServiceException {
        Player player = playerService.getPlayerById(playerId);
        Team team = 
            teamRepository
                .findById(teamId)
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team","Team with given id not found."));
        team.deletePlayer(player);
        Team teamInDB = teamRepository.save(team);
        TeamDTO teamDTO = new TeamDTO(teamInDB);
        return teamDTO;
    }
}
