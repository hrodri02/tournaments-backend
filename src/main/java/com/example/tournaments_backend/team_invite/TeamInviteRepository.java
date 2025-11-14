package com.example.tournaments_backend.team_invite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
     List<TeamInvite> findAllByInviteeId(Long inviteeId);
     Optional<TeamInvite> findByTeamIdAndInviteeId(Long teamId, Long inviteeId);
}