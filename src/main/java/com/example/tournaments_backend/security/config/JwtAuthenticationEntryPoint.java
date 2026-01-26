package com.example.tournaments_backend.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ErrorDetails error = new ErrorDetails(
            HttpStatus.UNAUTHORIZED, 
            ClientErrorKey.INVALID_CREDENTIALS.name(), 
            LocalDateTime.now()
        );
        String jsonResponse = objectMapper.writeValueAsString(error);
        response.getWriter().write(jsonResponse);
    }
}