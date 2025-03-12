package com.example.tournaments_backend.exception;

public class EmailAlreadyConfirmedException extends Exception {
    public EmailAlreadyConfirmedException(String message) {
        super(message);
    }
}

