package com.example.tournaments_backend.security;

public class SecurityConstants {
    
    public static final String SECRET = "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 60; //  1 minute 54_000; // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 1_296_000; // 15 days
    public static final String TOKEN_TYPE = "Bearer";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
}