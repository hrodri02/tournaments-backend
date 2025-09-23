package com.example.tournaments_backend.team_invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    
}

