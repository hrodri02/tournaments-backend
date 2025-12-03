package com.example.tournaments_backend.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<?> handleAuthenticationException(JwtException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new ErrorDetails(
                    status, 
                    ClientErrorKey.INVALID_TOKEN.name(), 
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
