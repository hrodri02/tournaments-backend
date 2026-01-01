package com.example.tournaments_backend.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.UserDTO;
import com.example.tournaments_backend.auth.tokens.TokensDTO;
import com.example.tournaments_backend.auth.tokens.refreshToken.RefreshToken;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.security.SecurityConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.headers.Header;
import jakarta.validation.Valid;

import java.util.Map;

import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(path="api/v1/auth")
@AllArgsConstructor
@Tag(name = "Auth Management", description = "API endpoints for authentication")
public class AuthController {
    private AuthService authService;

    @Operation(summary = "Registers a user", description = "Returns a confirmation message.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully creates a user", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - registration request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Valid RegistrationRequest request) throws ServiceException {
        authService.signUp(request);
        AuthResponse resBody = new AuthResponse("A confirmation email has been sent.");
        return ResponseEntity.ok(resBody);
    }

    @Operation(summary = "Logs in a user", description = "Returns the authenticated user, access token, refresh token, and expiration time of access token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticates a user", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid - authentication request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - account disabled or bad credentials",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDTO> authenticate(@RequestBody @Valid AuthenticationRequest authRequest) throws AuthenticationException {
        AppUser authenticatedUser = authService.authenticate(authRequest);

        if (!authenticatedUser.getEnabled()) {
            throw new DisabledException("Account is not enabled. Please verify your email.");
        }

        // if user's account is verified, generate access token and refresh token
        String accessToken = authService.generateAccessToken(authenticatedUser);
        Long expiresIn = authService.getExpirationTime(accessToken);
        String refreshToken = authService.generateRefreshToken(authenticatedUser);
        Long refreshTokenExpiresIn = authService.getExpirationTime(refreshToken);
        RefreshToken token = new RefreshToken(refreshToken, refreshTokenExpiresIn, authenticatedUser);
        // save refresh token in DB
        authService.saveRefreshToken(token);

        UserDTO userDTO = new UserDTO(authenticatedUser);
        TokensDTO tokensDTO = new TokensDTO(
                accessToken, 
                SecurityConstants.TOKEN_TYPE, 
                refreshToken, 
                expiresIn
            );
        AuthDTO authDTO = new AuthDTO(userDTO, tokensDTO);
        return ResponseEntity.ok(authDTO);
    }

    @Operation(summary = "Enables a user account", description = "Returns the user along with the access and refresh tokens.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully enables a user's account", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthDTO.class)),
            headers = @Header(name = HttpHeaders.AUTHORIZATION, description = "JWT Token", schema = @Schema(type = "string"))
        ),
        @ApiResponse(responseCode = "404", description = "Not found - cofirmation token not found or it expired",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @GetMapping("/confirm")
    public ResponseEntity<AuthDTO> confirm(
        @Parameter(description = "Confirmation token with 15 minute expiration")
        @RequestParam("token") String token) throws ServiceException 
    {
        AppUser user = authService.confirmToken(token);

        String accessToken = authService.generateAccessToken(user);
        Long expiresIn = authService.getExpirationTime(accessToken);
        String refreshTokenString = authService.generateRefreshToken(user);
        Long refreshTokenExpiresIn = authService.getExpirationTime(refreshTokenString);
        
        UserDTO userDTO = new UserDTO(user);
        TokensDTO tokensDTO = new TokensDTO(
                accessToken, 
                SecurityConstants.TOKEN_TYPE, 
                refreshTokenString, 
                expiresIn
            );
        AuthDTO authDTO = new AuthDTO(userDTO, tokensDTO);
        // save refresh token
        RefreshToken refreshToken = new RefreshToken(refreshTokenString, refreshTokenExpiresIn, user);
        authService.saveRefreshToken(refreshToken);
        return ResponseEntity.ok(authDTO);
    }

    @Operation(summary = "Resends confirmation link to user's email", description = "Returns a confirmation message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully resends email to user's account", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Not found - user with email not found or email already confirmed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("/resend")
    public ResponseEntity<AuthResponse> resend(
        @Parameter(description = "User's email")
        @RequestParam("email") String email) throws UsernameNotFoundException, ServiceException 
    {
        authService.resendEmail(email);
        AuthResponse resBody = new AuthResponse("A new confirmation email has been sent.");
        return ResponseEntity.ok(resBody);
    }

    @Operation(summary = "Sends password reset instructions to user's email", description = "Returns a confirmation message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully sends password reset instructions to user's email", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
        )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(
        @Parameter(description = "User's email")
        @RequestParam("email") String email) 
    {
        authService.sendResetPasswordEmail(email);
        AuthResponse resBody = new AuthResponse("We've sent password reset instructions to your email.");
        return ResponseEntity.ok(resBody);
    }

    @GetMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String resetToken, @RequestParam("email") String email) throws ServiceException {
        authService.validateResetToken(resetToken, email);
        Map<String, String> resBody = Map.of("message", "Redirect user to reset password form.");
        return ResponseEntity.ok().body(resBody);
    }

    @Operation(summary = "Resets user's password", description = "Notifies user that their password was reset")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully resets a user's password", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid - reset password request body is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "404", description = "Not found - user/token not found or password was already reset",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest resetPwdRequest) throws ServiceException {
        String token = resetPwdRequest.getToken();
        String email = resetPwdRequest.getEmail();
        authService.validateResetToken(token, email);
        String newPassword = resetPwdRequest.getNewPassword();
        authService.saveUsersNewPassword(token, email, newPassword);
        AuthResponse resBody = new AuthResponse("Password has been successfully reset.");
        return ResponseEntity.ok(resBody);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity
            .status(status)
            .body(new ErrorDetails(
                status,
                ex.getMessage(),
                LocalDateTime.now()
            ));
    }
}
