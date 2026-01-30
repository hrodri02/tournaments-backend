package com.example.tournaments_backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationToken;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationTokenService;
import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.Position;
import com.example.tournaments_backend.security.JwtService;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
    @Mock
    private JwtService jwtService;
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

    @Test
    void confirmToken_ShouldReturnUser_WhenTokenIsValid() {
        String mockToken = "generated-uuid-token";
        LocalDateTime createdAt = LocalDateTime.now();
        AppUser user = new AppUser(
            "John", 
            "Doe", 
            "jdoe@example.com", 
            "securepass1",
            AppUserRole.PLAYER
        );
        ConfirmationToken confirmationToken = new ConfirmationToken(
            mockToken, 
            createdAt, 
            createdAt.plusMinutes(15), 
            user
        );

        when(confirmationTokenService.getToken(mockToken)).thenReturn(Optional.of(confirmationToken));

        AppUser result = authService.confirmToken(mockToken);

        // assert the state
        assertThat(result).isEqualTo(user);
        
        // verify the behavior
        verify(confirmationTokenService).setConfirmedAt(mockToken);
        verify(appUserService).enableAppUser(user.getEmail());
    }

    @Test
    void confirmToken_ShouldReturnException_WhenTokenNotFound() {
        String mockToken = "generated-uuid-token";

        when(confirmationTokenService.getToken(mockToken))
            .thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.confirmToken(mockToken);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(ClientErrorKey.CONFIRMATION_TOKEN_NOT_FOUND, ex.getErrorKey());
        assertEquals("Token not found.", ex.getMessage());

        verify(confirmationTokenService, never()).setConfirmedAt(any());
        verify(appUserService, never()).enableAppUser(any());
    }

    @Test
    void confirmToken_ShouldReturnException_WhenTokenAlreadyConfirmed() {
        String mockToken = "generated-uuid-token";

        AppUser user = new AppUser(
            "John",
            "Doe",
            "jdoe@example.com",
            "securepass1",
            AppUserRole.PLAYER
        );
        LocalDateTime now = LocalDateTime.now();
        ConfirmationToken confirmationToken = new ConfirmationToken(
            mockToken, 
            now,
            now.plusMinutes(15), 
            user
        );
        confirmationToken.setConfirmedAt(now.plusMinutes(5));
        when(confirmationTokenService.getToken(mockToken))
            .thenReturn(Optional.of(confirmationToken));

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.confirmToken(mockToken);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ClientErrorKey.EMAIL_ALREADY_CONFIRMED, ex.getErrorKey());
        assertEquals("Email is already confirmed.", ex.getMessage());

        verify(confirmationTokenService, never()).setConfirmedAt(any());
        verify(appUserService, never()).enableAppUser(any());
    }

    @Test
    void confirmToken_ShouldReturnException_WhenTokenExpired() {
        String mockToken = "generated-uuid-token";

        AppUser user = new AppUser(
            "John",
            "Doe",
            "jdoe@example.com",
            "securepass1",
            AppUserRole.PLAYER
        );
        LocalDateTime past = LocalDateTime.now().minusMinutes(20);
        ConfirmationToken confirmationToken = new ConfirmationToken(
            mockToken, 
            past,
            past.plusMinutes(15), 
            user
        );

        when(confirmationTokenService.getToken(mockToken))
            .thenReturn(Optional.of(confirmationToken));

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.confirmToken(mockToken);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ClientErrorKey.TOKEN_EXPIRED, ex.getErrorKey());
        assertEquals("Token expired. Please request a new confirmation email.", ex.getMessage());

        verify(confirmationTokenService, never()).setConfirmedAt(any());
        verify(appUserService, never()).enableAppUser(any());
    }

    @Test
    void generateAccessToken_ShouldReturnAccessToken_WhenUsernameAndEmailAreValid() {
        String mockToken = "generated-uuid-token";
        AppUser user = new AppUser(
            "John", 
            "Doe", 
            "jdoe@example.com",
            "securepass1",
            AppUserRole.PLAYER);        

        when(jwtService.createAccessToken(user))
            .thenReturn(mockToken);
        
        String result = authService.generateAccessToken(user);
        assertEquals(mockToken, result);
    }

    @Test
    void sendResetPasswordEmail_ShouldSendEmail_WithCorrectLinkAndUser() {
        String email = "jdoe@example.com";
        AppUser user = new AppUser(
            "John",
            "Doe",
            "jdoe@example.com",
            "securepass1",
            AppUserRole.PLAYER
        );
        when(appUserService.getAppUserByEmail(email))
            .thenReturn(user);
        
        String mockToken = "mockToken";
        when(appUserService.generateNewResetTokenForUser(user))
            .thenReturn(mockToken);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        authService.sendResetPasswordEmail(email);

        // Verify interactions were precise
        verify(appUserService).getAppUserByEmail(email);
        verify(appUserService).generateNewResetTokenForUser(user);
        verify(emailSender).send(eq("Reset password"), eq(email), bodyCaptor.capture());

        String capturedBody = bodyCaptor.getValue();
        // Ensure the link is constructed correctly
        assertTrue(capturedBody.contains("token=" + mockToken));
        assertTrue(capturedBody.contains("email=" + email));
        
        // Ensure the greeting is personalized
        assertTrue(capturedBody.contains("John"));
    }
    
    @Test
    void sendResetPasswordEmail_ShouldReturnException_WhenUsernameNotFound() {
        String email = "jdoe@example.com";
        String errMsg = "User with username " + email + " not found.";
        when(appUserService.getAppUserByEmail(email))
            .thenThrow(new UsernameNotFoundException(errMsg));

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () -> {
            authService.sendResetPasswordEmail(email);
        });

        assertEquals(errMsg, ex.getMessage());

        verify(appUserService, never()).generateNewResetTokenForUser(any());
        verify(emailSender, never()).send(anyString(), eq(email), anyString());
    }
}