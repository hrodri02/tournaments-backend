package com.example.tournaments_backend.team;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerService;
import com.example.tournaments_backend.team_invite.TeamInvite;
import com.example.tournaments_backend.team_invite.TeamInviteService;
import com.example.tournaments_backend.team_invite.TeamInviteStatus;
import com.example.tournaments_backend.exception.ErrorType;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final PlayerService playerService;
    private final TeamInviteService teamInviteService;

    public List<Team> getTeams(Authentication authentication) {
        String clientEmail = authentication.getName();
        Player client = playerService.getPlayerByEmail(clientEmail);
        List<Team> teams = teamRepository.findByPlayers_Id(client.getId());
        return teams;
    }

    @Transactional
    public TeamDTO addTeam(TeamRequest teamRequest, Authentication authentication) {
        Team team = new Team(teamRequest);
        String ownerEmail = authentication.getName();
        Player owner = playerService.getPlayerByEmail(ownerEmail);
        team.setOwner(owner);
        Team teamInDB = teamRepository.save(team);

        // get all the players by id
        List<String> playerEmails = teamRequest.getPlayersToInvite();
        List<Player> players = playerService.getAllPlayersByEmail(playerEmails);
        // create a team invitation for each player
        List<TeamInvite> invites = new ArrayList<>();
        for (Player player : players) {
            TeamInvite invite = new TeamInvite();
            invite.setStatus(TeamInviteStatus.PENDING);
            invite.setTeam(teamInDB);
            invite.setInvitee(player);
            invite.setCreatedAt(teamRequest.getCreatedAt());
            invites.add(invite);
        }
        // save the team invitations
        List<TeamInvite> invitesInDB = teamInviteService.addAll(invites);
        
        String invitationStatus = invitesInDB.size() + " invitations sent successfully.";
        TeamDTO teamDTO = new TeamDTO(teamInDB, invitationStatus);
        return teamDTO;
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
