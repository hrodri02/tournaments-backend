package com.example.tournaments_backend.team;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.league.League;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerDTO;
import com.example.tournaments_backend.player.PlayerService;
import com.example.tournaments_backend.team_invite.TeamInvite;
import com.example.tournaments_backend.team_invite.TeamInviteDTO;
import com.example.tournaments_backend.team_invite.TeamInviteService;
import com.example.tournaments_backend.team_invite.TeamInviteStatus;
import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserService;

@Service
@AllArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final AppUserService appUserService;
    private final PlayerService playerService;
    private final TeamInviteService teamInviteService;

    @Transactional
    public GetTeamsResponse getTeams(Authentication authentication) {
        String clientEmail = authentication.getName();
        AppUser client = appUserService.getAppUserByEmail(clientEmail);
        Long userId = client.getId();
        List<Team> teams = teamRepository.findByPlayers_Id(userId);
        List<TeamDTO> teamsPartOfDTOs = getTeamDTOsWithInvites(teams);

        // get the invites for current user
        List<TeamInvite> invitesForUser = client.isAdmin()? List.of() : teamInviteService.getAllInvitesByPlayerId(userId, authentication);
        // get team ids that user was invited to
        List<Long> teamIdsInvitedTo = invitesForUser.stream()
                                .map(invite -> invite.getTeam().getId())
                                .collect(Collectors.toList());
        // get the teams the user was invited to
        List<Team> teamsInvitedTo = teamRepository.findAllById(teamIdsInvitedTo);
        List<TeamDTO> teamsInvitedToDTOs = getTeamDTOsWithInvites(teamsInvitedTo);

        GetTeamsResponse response = new GetTeamsResponse(teamsPartOfDTOs, teamsInvitedToDTOs);
        return response;
    }

    @Transactional
    public TeamDTO addTeam(TeamRequest teamRequest, Authentication authentication) {
        Team team = new Team(teamRequest);
        String ownerEmail = authentication.getName();
        Player owner = playerService.getPlayerByEmail(ownerEmail);
        team.setOwner(owner);
        team.addPlayer(owner);
        Team teamInDB = teamRepository.save(team);

        // get all the players by email
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
        TeamDTO teamDTO = new TeamDTO(teamInDB, invitesInDB);
        return teamDTO;
    }

    public Team getTeamById(Long id) throws ServiceException {
        Team team = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_NOT_FOUND, 
                            "Team", 
                            "Team with given id not found."
                        ));
        return team;
    }

    @Transactional
    public Team deleteTeamById(Long id) throws ServiceException {
        Team team = getTeamById(id);
        for (League league : team.getLeagues()) {
            league.getTeams().remove(team);
        }
        for (Player player : team.getPlayers()) {
            player.getTeams().remove(team);
        }
        teamRepository.deleteById(id);
        return team;
    }

    @Transactional
    public Team updateTeam(Long id, TeamRequest teamRequest) throws ServiceException {
        Team oldTeam = teamRepository
                        .findById(id)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_NOT_FOUND, 
                            "Team",
                            "Team with given id not found."
                        ));
        oldTeam.setName(teamRequest.getName());
        
        return teamRepository.save(oldTeam);
    }

    @Transactional
    public TeamDTO addPlayerToTeam(Long playerId, Long teamId) throws ServiceException {
        Player player = playerService.getPlayerById(playerId);
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team",
                    "Team with given id not found."
                ));
        team.addPlayer(player);
        Team teamInDB = teamRepository.save(team);
        TeamDTO teamDTO = new TeamDTO(teamInDB);
        return teamDTO;
    }

    @Transactional
    public TeamDTO deletePlayerFromTeam(Long playerId, Long teamId) throws ServiceException {
        Player player = playerService.getPlayerById(playerId);
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new ServiceException(
                    HttpStatus.NOT_FOUND, 
                    ClientErrorKey.TEAM_NOT_FOUND, 
                    "Team",
                    "Team with given id not found."
                ));
        team.deletePlayer(player);
        Team teamInDB = teamRepository.save(team);
        TeamDTO teamDTO = new TeamDTO(teamInDB);
        return teamDTO;
    }

    private List<TeamDTO> getTeamDTOsWithInvites(List<Team> teams) {
        if (teams.size() == 0) return List.of();

        // get team ids
        List<Long> teamIds = teams.stream()
                                .map(Team::getId)
                                .collect(Collectors.toList());
        // get all the team invites
        List<TeamInvite> invites = teamInviteService.getAllTeamInvites(teamIds);
        // group team invites by teamId
        Map<Long, List<TeamInvite>> groupedInvites = invites.stream()
                .collect(Collectors.groupingBy(invite -> invite.getTeam().getId()));
        // create TeamDTOs and add invites to each
        List<TeamDTO> teamDTOs = new ArrayList<>();
        for (Team team : teams) {
            Long teamId = team.getId();
            // set the invites of the current team
            List<TeamInvite> teamInvites = groupedInvites.getOrDefault(teamId, List.of());
            TeamDTO teamDTO = new TeamDTO(team);
            List<TeamInviteDTO> inviteDTOs = TeamInviteDTO.convert(teamInvites);
            teamDTO.setInvites(inviteDTOs);
            // set the players invited to the current team
            List<Long> playerIds = teamInvites.stream()
                                    .map(invite -> invite.getInvitee().getId())
                                    .toList();
            List<Player> players = playerService.getAllPlayersByIds(playerIds);
            List<PlayerDTO> playerDTOs = PlayerDTO.convert(players);
            teamDTO.setInvitees(playerDTOs);
            teamDTOs.add(teamDTO);
        }
        return teamDTOs;
    }
}