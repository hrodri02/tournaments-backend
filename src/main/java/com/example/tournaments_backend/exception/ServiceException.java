package com.example.tournaments_backend.exception;

public class ServiceException extends RuntimeException {
    private final ErrorType errorType;
    private final String resourceName;
    
    public ServiceException(ErrorType errorType, String resourceName, String message) {
        super(message);
        this.errorType = errorType;
        this.resourceName = resourceName;
    }
}
