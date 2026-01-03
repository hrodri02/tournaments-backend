package com.example.tournaments_backend.auth.tokens.refreshToken;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.app_user.AppUser;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void save(RefreshToken token) {
        refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteById(Long tokenId) {
        refreshTokenRepository.deleteById(tokenId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(AppUser user) {
        refreshTokenRepository.revokeAllByAppUser(user);
    }
}
