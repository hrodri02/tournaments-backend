package com.example.tournaments_backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationTokenService;
import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.Position;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {
    @Mock
    private AppUserService appUserService;
    @Mock
    private EmailSender emailSender;
    // Mock other dependencies even if not used in this specific method
    @Mock
    private AuthenticationManager authenticationManager;
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
    void signUp_ShouldThrowException_WhenUsernameAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest(
            "John", 
            "Doe", 
            "jdoe@exampl.com", 
            "securepass1", 
            "securepass2", 
            AppUserRole.PLAYER, 
            Position.STRIKER);
        
        when(appUserService.signUp(any()))
            .thenThrow(new ServiceException(
                HttpStatus.CONFLICT, 
                ClientErrorKey.USER_ALREADY_EXISTS, 
                "App user",
                "User already exists."));

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.signUp(request);
        });

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorKey()).isEqualTo(ClientErrorKey.USER_ALREADY_EXISTS);

        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    void authenticate_ShouldReturnUser_WhenCredentialsAreValid() {
        AuthenticationRequest request = new AuthenticationRequest(
            "jdoe@example.com", 
            "securepass1"
        );

        AppUser user = new AppUser(
            "John",
            "Doe",
            "jdoe@example.com",
            "securepass1",
            AppUserRole.PLAYER
        );

        when(appUserService.getAppUserByEmail(request.getEmail())).thenReturn(user);

        AppUser result = authService.authenticate(request);
        assertThat(result.getEmail()).isEqualTo("jdoe@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));        
    }

    @Test
    void authenticate_ShouldThrowException_WhenCredentialsAreInvalid() {
        // 1. Arrange
        AuthenticationRequest request = new AuthenticationRequest("wrong@example.com", "badpass");
        
        // STUB: Force the manager to throw an exception
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // 2. Act & Assert
        // We expect the exception to bubble up and stop the method
        assertThrows(BadCredentialsException.class, () -> {
            authService.authenticate(request);
        });

        // VERIFY: Ensure appUserService was NEVER called because the manager failed
        verify(appUserService, never()).getAppUserByEmail(any());
    }    
}