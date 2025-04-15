package com.example.tournaments_backend.auth.resetToken;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tournaments_backend.app_user.AppUser;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
    Optional<ResetToken> findByTokenAndAppUser(String token, AppUser user);
    Optional<ResetToken> findByToken(String token);
    List<ResetToken> findByAppUserAndResetAtIsNull(AppUser appUser);
}
