package com.example.tournaments_backend.auth.tokens.refreshToken;

import java.time.LocalDateTime;

import com.example.tournaments_backend.app_user.AppUser;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class RefreshToken {
    @Id
    @SequenceGenerator(
        name="refresh_token_sequence",
        sequenceName="refresh_token_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "refresh_token_sequence"
    )
    private Long id;
    @Column(nullable = false)
    private String token;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @JoinColumn(
        name = "app_user_id", 
        nullable = false
    )
    @JsonBackReference
    private AppUser appUser;
    private Boolean revoked;

    public RefreshToken(
        String token, 
        LocalDateTime expiresAt, 
        AppUser appUser,
        Boolean revoked
    ) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.appUser = appUser;
        this.revoked = revoked;
    }
}
