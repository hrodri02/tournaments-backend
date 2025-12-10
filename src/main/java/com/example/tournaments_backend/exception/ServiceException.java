package com.example.tournaments_backend.exception;
import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final ClientErrorKey errorKey;
    private final String resourceName;
    
    public ServiceException(
        HttpStatus status,
        ClientErrorKey errorKey, 
        String resourceName, 
        String message) 
    {
        super(message);
        this.status = status;
        this.errorKey = errorKey;
        this.resourceName = resourceName;
    }
}
