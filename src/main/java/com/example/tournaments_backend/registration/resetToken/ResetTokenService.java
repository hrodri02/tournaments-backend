package com.example.tournaments_backend.registration.resetToken;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ResetTokenService {
    private final ResetTokenRepository resetTokenRepository;

    public void saveResetToken(ResetToken token) {
        resetTokenRepository.save(token);
    }
}
