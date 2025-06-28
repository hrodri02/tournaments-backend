package com.example.tournaments_backend.team;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerService;
import com.example.tournaments_backend.exception.ErrorType;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final PlayerService playerService;

    public Team addTeam(TeamRequest teamRequest) {
        Team team = new Team(teamRequest);
        return teamRepository.save(team);
    }

    public Team getTeamById(Long id) throws ServiceException {
        Team team = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found."));
        return team;
    }

    @Transactional
    public Team deleteTeamById(Long id) {
        Team team = getTeamById(id);
        for (League league : team.getLeagues()) {
            league.getTeams().remove(team);
        }
        for (Player player : team.getPlayers()) {
            player.setTeam(null);
        }
        teamRepository.deleteById(id);
        return team;
    }

    @Transactional
    public Team updateTeam(Long id, TeamRequest teamRequest) throws ServiceException {
        Team oldTeam = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team","Team with given id not found."));
        oldTeam.setName(teamRequest.getName());
        
        return teamRepository.save(oldTeam);
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
