package com.example.tournaments_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRepository;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationToken;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationTokenRepository;
import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.player.Position;

@SpringBootTest
@Transactional // Rolls back DB changes after each test
public class AuthServiceIntegrationTests {
    @Autowired
    private AuthService authService;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @SpyBean // This wraps the real bean so we can force a failure
    private AppUserService appUserService;

    @Autowired
    private AppUserRepository userRepository; // Use your actual repository

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private EmailSender emailSender; // Mocked so we don't send real emails

    @Test
    void signUp_ShouldSavePlayerAndSendEmail_WhenRequestIsValid() {
        // 1. Arrange
        RegistrationRequest request = new RegistrationRequest(
            "Jane",
            "Doe",
            "jane@example.com",
            "password123",
            "password123",
            AppUserRole.PLAYER,
            Position.STRIKER 
        );

        // 2. Act
        String returnedToken = authService.signUp(request);

        // 3. Assert - Database State
        Optional<AppUser> savedUserOpt = userRepository.findAppUserByEmail("jane@example.com");
        assertTrue(savedUserOpt.isPresent(), "User should be saved in the database");
        
        AppUser savedUser = savedUserOpt.get();
        assertEquals("Jane", savedUser.getFirstName());
        assertTrue(savedUser instanceof Player, "Should create a Player instance for PLAYER role");
        assertEquals(Position.STRIKER, ((Player) savedUser).getPosition());

        // 4. Assert - Email Interaction
        verify(emailSender, times(1)).send(
            eq("Confirm email"),
            eq("jane@example.com"),
            contains(returnedToken) // Verify the token returned is the one in the email link
        );
    }

    @Test
    void signUp_ShouldSaveAdminAndSendEmail_WhenRequestIsValid() {
        // 1. Arrange
        RegistrationRequest request = new RegistrationRequest(
            "Jane",
            "Doe",
            "jane@example.com",
            "password123",
            "password123",
            AppUserRole.ADMIN,
            null
        );

        // 2. Act
        String returnedToken = authService.signUp(request);

        // 3. Assert - Database State
        Optional<AppUser> savedUserOpt = userRepository.findAppUserByEmail("jane@example.com");
        assertTrue(savedUserOpt.isPresent(), "User should be saved in the database");
        
        AppUser savedUser = savedUserOpt.get();
        assertEquals("Jane", savedUser.getFirstName());
        assertTrue(savedUser instanceof AppUser, "Should create a AppUser instance for ADMIN role");

        // 4. Assert - Email Interaction
        verify(emailSender, times(1)).send(
            eq("Confirm email"),
            eq("jane@example.com"),
            contains(returnedToken) // Verify the token returned is the one in the email link
        );
    }

    @Test
    void signUp_ShouldThrowException_WhenEmailAlreadyExists() {
        // 1. Arrange: Put an existing user in the DB
        String sharedEmail = "duplicate@example.com";
        AppUser existingUser = new AppUser(
            "First", 
            "User", 
            sharedEmail, 
            "password", 
            AppUserRole.USER
        );
        userRepository.save(existingUser);
        long orignalUserCount = userRepository.count();

        // 2. Create a request with that same email
        RegistrationRequest duplicateRequest = new RegistrationRequest(
            "Second", 
            "User", 
            sharedEmail, 
            "newpassword", 
            "newpassword", 
            AppUserRole.PLAYER, 
            Position.STRIKER
        );

        // 3. Act & Assert
        // Depending on your implementation, this might throw a ServiceException 
        // or a DataIntegrityViolationException from the DB.
        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.signUp(duplicateRequest);
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(ClientErrorKey.USER_ALREADY_EXISTS, ex.getErrorKey());
        assertEquals("User already exists.", ex.getMessage());

        // 4. Verify: Ensure the second user was NOT saved (count stays the same)
        long userCount = userRepository.count();
        assertEquals(orignalUserCount, userCount, "Database should still only have the original number of users.");
    }

    @Test
    void authenticate_ShouldReturnUser_WhenRequestIsValid() {
        AppUser user = new AppUser(
            "Jane", 
            "Doe", 
            "jdoe@example.com", 
            passwordEncoder.encode("password"), 
            AppUserRole.USER
        );
        user.setEnabled(true);
        userRepository.save(user);

        AuthenticationRequest request = new AuthenticationRequest(
            "jdoe@example.com", 
            "password"
        );

        AppUser result = authService.authenticate(request);

        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void authenticate_ShouldThrowException_WhenCredentialsAreInvalid() {
        AuthenticationRequest request = new AuthenticationRequest(
            "admin@example.com",
            "admin12"
        );

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
            authService.authenticate(request)
        );

        assertEquals("Bad credentials", ex.getMessage());
    }   

    @Test
    void confirmToken_ShouldReturnAppUser_WhenTokenIsValid() {
        AppUser user = new AppUser(
            "Heriberto", 
            "Rodriguez",
            "hrodriguez@example.com",
            passwordEncoder.encode("securespass"),
            AppUserRole.PLAYER
        );
        userRepository.save(user);
        
        LocalDateTime now = LocalDateTime.now();
        String mockToken = "mockToken";
        ConfirmationToken token = new ConfirmationToken(
            mockToken, 
            now,
            now.plusMinutes(15), 
        user);
        confirmationTokenRepository.save(token);

        AppUser result = authService.confirmToken(mockToken);
        
        
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());

        AppUser userInDb = userRepository.findAppUserByEmail(user.getEmail()).get();
        assertEquals(true, userInDb.isEnabled());

        Optional<ConfirmationToken> tokenInDB = confirmationTokenRepository.findByToken(mockToken);
        if (tokenInDB.isPresent()) {
            assertNotNull(tokenInDB.get().getConfirmedAt());
        }
    }

    @Test
    void confirmToken_ShouldReturnRollback_WhenEnablingUserAccountFails() {
        AppUser user = new AppUser(
            "Heriberto", 
            "Rodriguez",
            "hrodriguez@example.com",
            passwordEncoder.encode("securespass"),
            AppUserRole.PLAYER
        );
        userRepository.save(user);
        
        LocalDateTime now = LocalDateTime.now();
        String mockToken = "mockToken";
        ConfirmationToken token = new ConfirmationToken(
            mockToken, 
            now,
            now.plusMinutes(15), 
        user);
        confirmationTokenRepository.save(token);

        // FORCE COMMIT the setup so it survives the future rollback
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 2. Start a NEW transaction for the "Act" part
        TestTransaction.start();
        
        doThrow(new RuntimeException("Database Connection Failed"))
            .when(appUserService)
            .enableAppUser(eq("hrodriguez@example.com"));

        assertThrows(RuntimeException.class, () -> 
            authService.confirmToken(mockToken)
        );

        TestTransaction.flagForRollback();
        TestTransaction.end();
        
        // Start a new transaction to read the clean state
        TestTransaction.start();

        ConfirmationToken tokenInDB = confirmationTokenRepository.findByToken(mockToken).get();
        assertNull(tokenInDB.getConfirmedAt());
    }

    @Test
    void resendEmail_ShouldSendEmail_WhenUsersAccountIsNotEnabled() {
        String email = "uniqueEmail1@example.com";
        AppUser user = new AppUser(
            "Heriberto",
            "Rodriguez",
            email,
            "securepass",
            AppUserRole.PLAYER
        );
        userRepository.save(user);

        authService.resendEmail(email);

        List<ConfirmationToken> tokens = confirmationTokenRepository
            .findByAppUserAndConfirmedAtIsNull(user);

        assertEquals(1, tokens.size());

        verify(emailSender)
            .send(
                eq("Confirm email"),
                eq(email),
                anyString()
            );
    }

    @Test
    void resendEmail_ShouldThrowException_WhenUsersEmailIsAlreadyConfirmed() {
        String email = "resend-test@example.com";
        AppUser user = new AppUser(
            "Heriberto",
            "Rodriguez",
            email,
            "securepass",
            AppUserRole.PLAYER
        );
        user.setEnabled(true);
        userRepository.save(user);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            authService.resendEmail(email);
        }); 

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ClientErrorKey.EMAIL_ALREADY_CONFIRMED, ex.getErrorKey());
        assertEquals("Email is already confirmed.", ex.getMessage());

        List<ConfirmationToken> tokens = confirmationTokenRepository
            .findByAppUserAndConfirmedAtIsNull(user);

        assertEquals(0, tokens.size());

        verify(emailSender, never())
            .send(
                anyString(),
                anyString(),
                anyString()
            );      
    }
}
