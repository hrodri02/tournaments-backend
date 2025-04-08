package com.example.tournaments_backend.registration.resetToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
}
