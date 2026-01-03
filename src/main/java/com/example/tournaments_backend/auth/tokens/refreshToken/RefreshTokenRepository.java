package com.example.tournaments_backend.auth.tokens.refreshToken;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.tournaments_backend.app_user.AppUser;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.appUser = :user AND r.revoked = false")
    void revokeAllByAppUser(@Param("user") AppUser user);
}
