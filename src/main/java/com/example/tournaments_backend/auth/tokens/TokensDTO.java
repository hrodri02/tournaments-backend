package com.example.tournaments_backend.auth.tokens;

public record TokensDTO (
    String accessToken,
    String tokenType,
    String refreshToken,
    Long expiresIn
) {}
