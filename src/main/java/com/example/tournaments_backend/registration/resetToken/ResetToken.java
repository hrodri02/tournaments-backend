package com.example.tournaments_backend.registration.resetToken;

import java.time.LocalDateTime;

import com.example.tournaments_backend.app_user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ResetToken {
    @Id
    @SequenceGenerator(
        name="reset_token_sequence",
        sequenceName="reset_token_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "reset_token_sequence"
    )
    private Long id;
    @Column(nullable = false)
    private String token;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @ManyToOne
    @JoinColumn(
        name = "app_user_id", 
        nullable = false
    )
    private AppUser appUser;

    public ResetToken(String token,
                    LocalDateTime createdAt,
                    LocalDateTime expiresAt,
                    AppUser appUser) 
    {
        this.token = token;                                
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.appUser = appUser;
    }
}
