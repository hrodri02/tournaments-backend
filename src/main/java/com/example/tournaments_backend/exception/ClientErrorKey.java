package com.example.tournaments_backend.exception;

public enum ClientErrorKey {
    TOKEN_EXPIRED,
    INVALID_TOKEN,
    MISSING_TOKEN,
    ALREADY_EXISTS,
    EMAIL_ALREADY_CONFIRMED,
    PASSWORD_ALREADY_RESET,
    INVALID_INPUT,
    PERMISSION_DENIED,
    GAME_INACTIVE
}