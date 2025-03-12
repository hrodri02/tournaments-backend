package com.example.tournaments_backend.email;

public interface EmailSender {
    void send(String to, String email);
}
