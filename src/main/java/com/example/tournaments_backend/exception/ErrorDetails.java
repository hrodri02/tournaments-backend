package com.example.tournaments_backend.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetails {
    private int status;
    private String errorKey;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private List<ValidationErrorDetail> validationErrors;

    public ErrorDetails(HttpStatus status, String errorKey, LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.status = status.value();
        this.errorKey = errorKey;
        validationErrors = List.of();
    }

    public ErrorDetails(HttpStatus status, String errorKey, List<ValidationErrorDetail> validationErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status.value();
        this.errorKey = errorKey; // Use a general key like "VALIDATION_FAILED"
        this.validationErrors = validationErrors;
    }
}
