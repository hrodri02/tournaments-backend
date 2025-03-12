package com.example.tournaments_backend.registration;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.UserDTO;
import com.example.tournaments_backend.exception.EmailAlreadyConfirmedException;
import com.example.tournaments_backend.exception.ErrorDetails;
import com.example.tournaments_backend.exception.TokenExpiredException;
import com.example.tournaments_backend.exception.TokenNotFoundException;
import com.example.tournaments_backend.exception.UserAlreadyExistException;

import jakarta.validation.Valid;

import java.util.Map;

import static com.example.tournaments_backend.security.SecurityConstants.AUTHORIZATION_HEADER;
import static com.example.tournaments_backend.security.SecurityConstants.TOKEN_PREFIX;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(path="api/v1/auth")
@AllArgsConstructor
public class RegistrationController {
    private RegistrationService registrationService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Valid RegistrationRequest request) throws UserAlreadyExistException {
        registrationService.signUp(request);
        Map<String, String> resBody = Map.of("message", "A confirmation email has been sent.");
        return ResponseEntity.ok().body(resBody);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody @Valid AuthenticationRequest authRequest) {
        AppUser authenticatedUser = registrationService.authenticate(authRequest);

        // if user's account is verified
        if (authenticatedUser.getEnabled()) {
            String jws = registrationService.generateJWS(authenticatedUser);

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

        Map<String, String> resBody = Map.of("message", "Account is not enabled. Please verify your email.");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(resBody);
    }

    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam("token") String token) throws TokenNotFoundException, EmailAlreadyConfirmedException, TokenExpiredException {
        String jws = registrationService.confirmToken(token);

        Map<String, String> resBody = Map.of("message", "Account verified!");
        return ResponseEntity
                .ok()
                .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + jws)
                .body(resBody);
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestParam("email") String email) throws UsernameNotFoundException, EmailAlreadyConfirmedException {
        registrationService.resendEmail(email);
        Map<String, String> resBody = Map.of("message", "A new confirmation email has been sent.");
        return ResponseEntity.ok().body(resBody);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(new Date(), ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<?> handleUserAlreadyExists(UserAlreadyExistException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(new Date(), ex.getMessage()));
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<?> handleTokenNotFound(TokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(new Date(), ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyConfirmedException.class)
    public ResponseEntity<?> handle(EmailAlreadyConfirmedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(new Date(), ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<?> handleTokenExpired(TokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE).body(new ErrorDetails(new Date(), ex.getMessage()));
    }
}
