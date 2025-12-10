package com.example.tournaments_backend.exception;

import lombok.Builder;
import lombok.Getter;

/**
 * A detail object used within the global ErrorDetails 
 * to describe a specific field validation failure.
 */
@Getter
@Builder
public class ValidationErrorDetail {
    /** The name of the field that failed validation (e.g., "email"). */
    private final String field;
    
    /** * The validation constraint code (e.g., "NotNull", "Size", "Email").
     * This acts as the error key for frontend translation.
     */
    private final String errorKey;
    
    /** A developer-friendly default message (optional, but useful for debugging). */
    private final String message;
}
