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
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private HttpStatus status;
    private String errorKey;
    private LocalDateTime timestamp;
    private List<ValidationErrorDetail> validationErrors;

    public ErrorDetails(HttpStatus status, String errorKey, LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorKey = errorKey;
        validationErrors = List.of();
    }

    public ErrorDetails(HttpStatus status, String errorKey, List<ValidationErrorDetail> validationErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.errorKey = errorKey; // Use a general key like "VALIDATION_FAILED"
        this.validationErrors = validationErrors;
    }
}
