package com.example.tournaments_backend.team_invite;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.PlayerRepository;
import com.example.tournaments_backend.team.Team;
import com.example.tournaments_backend.team.TeamDTO;
import com.example.tournaments_backend.team.TeamRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TeamInviteService {
    private final TeamInviteRepository teamInviteRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final EmailSender emailSender;

    @Transactional
    public TeamInvite addTeamInvite(CreateTeamInviteRequest request, Long teamId, Authentication authentication) throws ServiceException {
        // 1. Check if Team exists
        Team team = teamRepository
                        .findById(teamId)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_NOT_FOUND, 
                            "Team", 
                            "Team with given id not found."
                        ));

        // 2. Check if Player (invitee) exists
        String playerEmail = request.getEmail();
        Player player = playerRepository
                        .findByEmail(playerEmail)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.USER_NOT_FOUND, 
                            "Player", 
                            "Player with given email not found."
                        ));

        // 3. Check if current user is the team owner
        String currentUserEmail = authentication.getName();
        String ownerEmail = team.getOwner().getEmail();
        if (!currentUserEmail.equals(ownerEmail)) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.NOT_TEAM_OWNER, 
                "Team Invite", 
                "Current user does not own this team."
            );
        }

        // 4. Create or update invite
        TeamInvite invite = teamInviteRepository
                                .findByTeamIdAndInviteeId(teamId, player.getId())
                                .orElse(new TeamInvite());
        invite.setStatus(TeamInviteStatus.PENDING);
        invite.setTeam(team);
        invite.setInvitee(player);
        invite.setCreatedAt(request.getCreatedAt());

        TeamInvite inviteInDB = teamInviteRepository.save(invite);
        
        // 5. Send email notification
        Long inviteId = inviteInDB.getId();
        String acceptLink = "http://localhost:8080/api/v1/team-invites/" + inviteId + "/accept";
        String declineLink = "http://localhost:8080/api/v1/team-invites/" + inviteId + "/decline";
        String teamName = team.getName();
        emailSender.send(
            "Invite to join " + teamName,
            player.getEmail(), 
            buildEmail(player.getFirstName(), teamName, acceptLink, declineLink)
        );

        return inviteInDB;
    }

    public List<TeamInvite> addAll(List<TeamInvite> invites) {
        return teamInviteRepository.saveAll(invites);
    }

    @Transactional
    public AcceptTeamInviteResponse accepInvite(Long inviteId, Authentication authentication) throws ServiceException {
        // 1. Check if Invite exists
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_INVITE_NOT_FOUND, 
                            "Team Invite", 
                            "Team Invite with given id not found."
                        ));

        // 2. Check invite status (if revoked)
        if (invite.getStatus() == TeamInviteStatus.REVOKED) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.TEAM_INVITE_REVOKED, 
                "Team Invite", 
                "Invite to join team was revoked."
            );
        }

        Player player = invite.getInvitee();
        String currentUserEmail = authentication.getName();
        String inviteeEmail = player.getEmail();

        // 3. Check if current user is the invitee
        if (!currentUserEmail.equals(inviteeEmail)) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.USER_NOT_INVITED_TO_TEAM, 
                "Team Invite", 
                "Current user was not invited to join this team."
            );
        }

        // 4. Accept invite
        Team team = invite.getTeam();
        team.addPlayer(player);
        invite.setStatus(TeamInviteStatus.ACCEPTED);

        teamInviteRepository.save(invite);

        TeamInviteDTO inviteDTO = new TeamInviteDTO(invite);
        TeamDTO teamDTO = new TeamDTO(team);
        AcceptTeamInviteResponse response = new AcceptTeamInviteResponse(inviteDTO, teamDTO);
        return response;
    }

    @Transactional
    public TeamInvite declineInvite(Long inviteId, Authentication authentication) throws ServiceException {
        // 1. Check if Invite exists
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_INVITE_NOT_FOUND, 
                            "Team Invite", 
                            "Team Invite with given id not found."
                        ));

        // 2. Check invite status (if revoked)
        if (invite.getStatus() == TeamInviteStatus.REVOKED) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.TEAM_INVITE_REVOKED, 
                "Team Invite", 
                "Invite to join team was revoked."
            );
        }

        Player player = invite.getInvitee();
        String currentUserEmail = authentication.getName();
        String inviteeEmail = player.getEmail();

        // 3. Check if current user is the invitee
        if (!currentUserEmail.equals(inviteeEmail)) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.USER_NOT_INVITED_TO_TEAM, 
                "Team Invite", 
                "Current user was not invited to join this team."
            );
        }

        // 4. Decline invite
        invite.setStatus(TeamInviteStatus.DECLINED);
        return teamInviteRepository.save(invite);
    }

    @Transactional
    public TeamInvite revokeInvite(@PathVariable Long inviteId, Authentication authentication) throws ServiceException {
        // 1. Check if Invite exists
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, 
                            ClientErrorKey.TEAM_INVITE_NOT_FOUND, 
                            "Team Invite", 
                            "Team Invite with given id not found."
                        ));

        TeamInviteStatus status = invite.getStatus();
        // 2. Check if invite status prevents revocation
        if (status == TeamInviteStatus.ACCEPTED || status == TeamInviteStatus.DECLINED) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.TEAM_INVITE_ALREADY_ACCEPTED_OR_DENIED, 
                "Team Invite", 
                "Invite was already accepted/declined."
            );
        }

        // 3. Check if current user is the team owner
        String currentUserEmail = authentication.getName();
        String ownerEmail = invite.getTeam().getOwner().getEmail();
        if (!currentUserEmail.equals(ownerEmail)) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.NOT_TEAM_OWNER, 
                "Team Invite", 
                "Current user does not own this team."
            );
        }

        // 4. Revoke invite
        invite.setStatus(TeamInviteStatus.REVOKED);
        return teamInviteRepository.save(invite);
    }

    public List<TeamInvite> getAllInvitesByPlayerId(Long playerId, Authentication authentication) throws ServiceException {
        // 1. Check if Player exists
        Player player = playerRepository
            .findById(playerId)
            .orElseThrow(() -> new ServiceException(
                HttpStatus.NOT_FOUND, 
                ClientErrorKey.USER_NOT_FOUND, 
                "Player", 
                "Player with given id not found."
            ));

        // 2. Check if current user matches the player ID
        if (!authentication.getName().equals(player.getEmail())) {
            throw new ServiceException(
                HttpStatus.FORBIDDEN, 
                ClientErrorKey.UNAUTHORIZED_TO_ACCESS_TEAM_INVITES, 
                "Player", 
                "Current user does not match playerId."
            );
        }

        return teamInviteRepository.findAllByInviteeId(playerId);
    }

    public List<TeamInvite> getAllTeamInvites(List<Long> teamIds) {
        return teamInviteRepository.findAllById(teamIds);
    }

    private String buildEmail(String name, String teamName, String acceptLink, String declineLink) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Team invite</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> You have been invited to join " + teamName + ". Please use the links below to accept or decline the invitation: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + acceptLink + "\">Accept</a> <a href=\"" + declineLink + "\">Decline</a> </p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}