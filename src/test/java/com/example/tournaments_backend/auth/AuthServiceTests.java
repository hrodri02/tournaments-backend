package com.example.tournaments_backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationTokenService;
import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.Position;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {
    @Mock
    private AppUserService appUserService;
    @Mock
    private EmailSender emailSender;
    // Mock other dependencies even if not used in this specific method
    @Mock
    private ConfirmationTokenService confirmationTokenService;
    // ... add mocks for the rest of the constructor fields

    @InjectMocks
    private AuthService authService;

    @Test
    void signUp_ShouldCreatePlayer_WhenRoleIsPlayer() {
        // 1. Arrange
        RegistrationRequest request = new RegistrationRequest(
                "John", 
                "Doe", 
                "john@example.com", 
                "password123", 
                "password123", 
                AppUserRole.PLAYER, 
                Position.STRIKER
        );
        String mockToken = "generated-uuid-token";
        
        // Tell the mock service to return our token when any AppUser signs up
        when(appUserService.signUp(any(AppUser.class))).thenReturn(mockToken);

        // 2. Act
        String result = authService.signUp(request);

        // 3. Assert
        assertThat(result).isEqualTo(mockToken);

        // Verify that appUserService.signUp was called with a PLAYER object
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserService).signUp(userCaptor.capture());
        assertThat(userCaptor.getValue()).isInstanceOf(Player.class);
        
        // Verify email was sent
        verify(emailSender).send(
            eq("Confirm email"), 
            eq("john@example.com"), 
            any(String.class)
        );
    }

    @Test
    void signUp_ShouldCreateAppUser_WhenRoleIsAdmin() {
        // 1. Arrange
        RegistrationRequest request = new RegistrationRequest(
                "John", 
                "Doe", 
                "john@example.com", 
                "password123", 
                "password123", 
                AppUserRole.ADMIN, 
                null
        );
        String mockToken = "generated-uuid-token";
        
        // Tell the mock service to return our token when any AppUser signs up
        when(appUserService.signUp(any(AppUser.class))).thenReturn(mockToken);

        // 2. Act
        String result = authService.signUp(request);

        // 3. Assert
        assertThat(result).isEqualTo(mockToken);

        // Verify that appUserService.signUp was called with a PLAYER object
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserService).signUp(userCaptor.capture());
        assertThat(userCaptor.getValue()).isInstanceOf(AppUser.class);
        
        // Verify email was sent
        verify(emailSender).send(
            eq("Confirm email"), 
            eq("john@example.com"), 
            any(String.class)
        );
    }

    @Test
    void authenticate_ShouldReturnUser_WhenCredentialsAreValid() {
        AuthenticationRequest request = new AuthenticationRequest(
            "admin@example.com", 
            "admin123"
        );

        authService.authenticate(request);
        
    }    
}