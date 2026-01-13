package com.example.tournaments_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRepository;
import com.example.tournaments_backend.app_user.AppUserRole;
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
}
