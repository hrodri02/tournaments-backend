package com.example.tournaments_backend.exception;

public class PasswordAlreadyResetException extends Exception {
    public PasswordAlreadyResetException(String message) {
        super(message);
    }
}
