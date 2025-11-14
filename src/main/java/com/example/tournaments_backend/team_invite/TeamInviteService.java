package com.example.tournaments_backend.team_invite;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ErrorType;
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
        Team team = teamRepository
                        .findById(teamId)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team", "Team with given id not found."));

        String playerEmail = request.getEmail();
        Player player = playerRepository
                        .findByEmail(playerEmail)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));

        String currentUserEmail = authentication.getName();
        String ownerEmail = team.getOwner().getEmail();
        if (!currentUserEmail.equals(ownerEmail)) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Current user does not own this team.");
        }

        TeamInvite invite = teamInviteRepository
                                .findByTeamIdAndInviteeId(teamId, player.getId())
                                .orElse(new TeamInvite());
        invite.setStatus(TeamInviteStatus.PENDING);
        invite.setTeam(team);
        invite.setInvitee(player);
        invite.setCreatedAt(request.getCreatedAt());

        TeamInvite inviteInDB = teamInviteRepository.save(invite);
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
    public AcceptTeamInviteResponse accepInvite(Long inviteId, Authentication authentication) {
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team Invite", "Team Invite with given id not found."));

        if (invite.getStatus() == TeamInviteStatus.REVOKED) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Invite to join team was revoked.");
        }

        Player player = invite.getInvitee();
        String currentUserEmail = authentication.getName();
        String inviteeEmail = player.getEmail();

        if (!currentUserEmail.equals(inviteeEmail)) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Current user was not invited to join this team.");
        }

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
    public TeamInvite declineInvite(Long inviteId, Authentication authentication) {
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team Invite", "Team Invite with given id not found."));

        if (invite.getStatus() == TeamInviteStatus.REVOKED) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Invite to join team was revoked.");
        }

        Player player = invite.getInvitee();
        String currentUserEmail = authentication.getName();
        String inviteeEmail = player.getEmail();

        if (!currentUserEmail.equals(inviteeEmail)) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Current user was not invited to join this team.");
        }

        invite.setStatus(TeamInviteStatus.DECLINED);
        return teamInviteRepository.save(invite);
    }

    @Transactional
    public TeamInvite revokeInvite(@PathVariable Long inviteId, Authentication authentication) {
        TeamInvite invite = teamInviteRepository
                        .findById(inviteId)
                        .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Team Invite", "Team Invite with given id not found."));

        TeamInviteStatus status = invite.getStatus();
        if (status == TeamInviteStatus.ACCEPTED || status == TeamInviteStatus.DECLINED) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Invite was already accepted/declined.");
        }

        String currentUserEmail = authentication.getName();
        String ownerEmail = invite.getTeam().getOwner().getEmail();
        if (!currentUserEmail.equals(ownerEmail)) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Team Invite", "Current user does not own this team.");
        }

        invite.setStatus(TeamInviteStatus.REVOKED);
        return teamInviteRepository.save(invite);
    }

    public List<TeamInvite> getAllInvitesByPlayerId(Long playerId, Authentication authentication) throws ServiceException {
        Player player = playerRepository
            .findById(playerId)
            .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, "Player", "Player with given id not found."));

        if (!authentication.getName().equals(player.getEmail())) {
            throw new ServiceException(ErrorType.FORBIDDEN, "Player", "Current user does not match playerId.");
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