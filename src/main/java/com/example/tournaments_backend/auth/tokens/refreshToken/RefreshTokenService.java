package com.example.tournaments_backend.auth.tokens.refreshToken;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void save(RefreshToken token) {
        refreshTokenRepository.save(token);
    }
}
