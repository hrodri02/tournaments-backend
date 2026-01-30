package com.example.tournaments_backend.auth.tokens.refreshToken;

import com.example.tournaments_backend.app_user.AppUser;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;
    @Column(name = "expires_in", nullable = false)
    private Long expiresIn;
    @ManyToOne
    @JoinColumn(name = "app_user_id", nullable = false)
    @JsonBackReference
    private AppUser appUser;
    private Boolean revoked;

    public RefreshToken(
        String token, 
        Long expiresIn, 
        AppUser appUser
    ) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.appUser = appUser;
        this.revoked = false;
    }
}
