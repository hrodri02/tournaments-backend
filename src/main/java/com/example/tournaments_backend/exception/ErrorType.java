package com.example.tournaments_backend.exception;

public enum ErrorType {
    NOT_FOUND,
    FORBIDDEN,
    TOKEN_EXPIRED,
    ALREADY_EXISTS,
    EMAIL_ALREADY_CONFIRMED,
    PASSWORD_ALREADY_RESET,
    INVALID_INPUT,
    PERMISSION_DENIED,
    GAME_INACTIVE
}