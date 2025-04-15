package com.example.tournaments_backend.auth.confirmationToken;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.app_user.AppUser;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConfirmationTokenService {
    private final ConfirmationTokenRepository confirmationTokenRepository;

    public void saveConfirmationToken(ConfirmationToken token) {
        confirmationTokenRepository.save(token);
    }

    public Optional<ConfirmationToken> getToken(String token) {
        return confirmationTokenRepository.findByToken(token);
    }

    public void setConfirmedAt(String token) {
        ConfirmationToken confirmationToken = 
            confirmationTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new IllegalStateException("token not found"));
            
        confirmationToken.setConfirmedAt(LocalDateTime.now());
        confirmationTokenRepository.save(confirmationToken);
    }

    public void invalidateUnconfirmedTokens(AppUser appUser) {
        List<ConfirmationToken> tokens = confirmationTokenRepository.findByAppUserAndConfirmedAtIsNull(appUser);
        for (ConfirmationToken token : tokens) {
            token.setExpiresAt(LocalDateTime.now()); // Mark old tokens as expired
            confirmationTokenRepository.save(token);
        }
    }
}
