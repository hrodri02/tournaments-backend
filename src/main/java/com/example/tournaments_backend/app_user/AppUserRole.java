package com.example.tournaments_backend.app_user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum AppUserRole {
    USER,
    ADMIN,
    PLAYER;

    public GrantedAuthority asGrantedAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + this.name());
    }
}
