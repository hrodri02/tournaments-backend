package com.example.tournaments_backend.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.UserDTO;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.ServiceException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;

import static com.example.tournaments_backend.security.SecurityConstants.AUTHORIZATION_HEADER;
import static com.example.tournaments_backend.security.SecurityConstants.TOKEN_PREFIX;

import java.time.LocalDateTime;

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
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrationConfirmationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - registration request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @PostMapping("/signup")
    public ResponseEntity<RegistrationConfirmationResponse> signUp(@RequestBody @Valid RegistrationRequest request) throws ServiceException {
        authService.signUp(request);
        RegistrationConfirmationResponse resBody = new RegistrationConfirmationResponse("A confirmation email has been sent.");
        return ResponseEntity.ok(resBody);
    }

    @Operation(summary = "Logs in a user", description = "Returns the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticates a user", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid - authentication request is not valid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - account disabled or bad credentials",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<UserDTO> authenticate(@RequestBody @Valid AuthenticationRequest authRequest) throws AuthenticationException {
        AppUser authenticatedUser = authService.authenticate(authRequest);

        if (!authenticatedUser.getEnabled()) {
            throw new DisabledException("Account is not enabled. Please verify your email.");
        }

        // if user's account is verified
        String jws = authService.generateJWS(authenticatedUser);

        UserDTO resBody = new UserDTO(
            authenticatedUser.getId(), 
            authenticatedUser.getFirstName(),
            authenticatedUser.getLastName(),
            authenticatedUser.getEmail(),
            authenticatedUser.getAppUserRole());
        
        // send user json to client with auth token
        return ResponseEntity
                .ok()
                .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + jws)
                .body(resBody);
    }

    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam("token") String token) throws ServiceException {
        String jws = authService.confirmToken(token);

        Map<String, String> resBody = Map.of("message", "Account verified!");
        return ResponseEntity
                .ok()
                .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + jws)
                .body(resBody);
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestParam("email") String email) throws UsernameNotFoundException, ServiceException {
        authService.resendEmail(email);
        Map<String, String> resBody = Map.of("message", "A new confirmation email has been sent.");
        return ResponseEntity.ok().body(resBody);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        authService.sendResetPasswordEmail(email);
        Map<String, String> resBody = Map.of("message", "We've sent password reset instructions to your email.");
        return ResponseEntity.ok().body(resBody);
    }

    @GetMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String resetToken, @RequestParam("email") String email) throws ServiceException {
        authService.validateResetToken(resetToken, email);
        Map<String, String> resBody = Map.of("message", "Redirect user to reset password form.");
        return ResponseEntity.ok().body(resBody);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest resetPwdRequest) throws ServiceException {
        String token = resetPwdRequest.getToken();
        String email = resetPwdRequest.getEmail();
        authService.validateResetToken(token, email);
        String newPassword = resetPwdRequest.getNewPassword();
        authService.saveUsersNewPassword(token, email, newPassword);
        Map<String, String> resBody = Map.of("message", "Password has been successfully reset.");
        return ResponseEntity.ok().body(resBody);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(LocalDateTime.now(), ex.getMessage()));
    }
}
