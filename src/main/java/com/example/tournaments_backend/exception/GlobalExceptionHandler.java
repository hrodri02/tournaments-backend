package com.example.tournaments_backend.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 1. Extract and map FieldError to the new DTO
        List<ValidationErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> ValidationErrorDetail.builder()
                // Get the field name
                .field(error.getField()) 
                // Get the first error code (which is the constraint name, e.g., NotNull, Size)
                .errorKey(error.getCode()) 
                // Get the default message (useful for development logs)
                .message(error.getDefaultMessage()) 
                .build())
            .collect(Collectors.toList());

        // 2. Create the unified response object
        ErrorDetails response = new ErrorDetails(
            HttpStatus.BAD_REQUEST,
            // Use a general key for validation failure
            ClientErrorKey.VALIDATION_FAILED.name(), 
            errorDetails 
        );
        
        // Set the list of errors in the response (if not done in the constructor)
        response.setValidationErrors(errorDetails);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredTokenException(ExpiredJwtException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.TOKEN_EXPIRED.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleMissingTokenException(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.MISSING_TOKEN.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler({SignatureException.class, MalformedJwtException.class, UnsupportedJwtException.class})
    public ResponseEntity<?> handleInvalidTokenException(JwtException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.INVALID_TOKEN.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleAuthenticationException(BadCredentialsException  ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.INVALID_CREDENTIALS.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabledException(DisabledException  ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.DISABLED_ACCOUNT.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLockedException(LockedException  ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.LOCKED_ACCOUNT.name(), 
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidEnumValue(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status,
                    ex.getMessage(),
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorDetails> handleServiceException(ServiceException ex) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status,
                    ex.getErrorKey().name(),
                    LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetails> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status,
                    ex.getMessage(),
                    LocalDateTime.now()
                ));
    }
}
