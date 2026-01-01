package com.example.tournaments_backend.auth;

import com.example.tournaments_backend.app_user.UserDTO;
import com.example.tournaments_backend.auth.tokens.TokensDTO;

public record AuthDTO (
    UserDTO user,
    TokensDTO tokens
) {}
