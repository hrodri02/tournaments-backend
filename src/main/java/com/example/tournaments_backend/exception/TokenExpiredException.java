package com.example.tournaments_backend.exception;

public class TokenExpiredException extends Exception {
    public TokenExpiredException(String message) {
        super(message);
    }
}
