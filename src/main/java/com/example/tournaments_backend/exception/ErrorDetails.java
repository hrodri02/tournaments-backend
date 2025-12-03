package com.example.tournaments_backend.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetails {
    private HttpStatus status;
    private String errorKey;
    private LocalDateTime timestamp;
}
