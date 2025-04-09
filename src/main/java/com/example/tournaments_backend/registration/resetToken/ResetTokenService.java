package com.example.tournaments_backend.registration.resetToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.registration.token.ConfirmationToken;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ResetTokenService {
    private final ResetTokenRepository resetTokenRepository;

    public void saveResetToken(ResetToken token) {
        resetTokenRepository.save(token);
    }

    public Optional<ResetToken> getToken(String token, AppUser user) {
        return resetTokenRepository.findByTokenAndAppUser(token, user);
    }

    public void setResetAt(String token) {
        ResetToken resetToken = 
            resetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new IllegalStateException("token not found"));
            
        resetToken.setResetAt(LocalDateTime.now());
        resetTokenRepository.save(resetToken);
    }

    public void invalidateUnconfirmedTokens(AppUser appUser) {
        List<ResetToken> tokens = resetTokenRepository.findByAppUserAndResetAtIsNull(appUser);
        for (ResetToken token : tokens) {
            token.setExpiresAt(LocalDateTime.now()); // Mark old tokens as expired
            resetTokenRepository.save(token);
        }
    }
}
