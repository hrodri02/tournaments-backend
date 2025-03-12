package com.example.tournaments_backend.app_user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.exception.UserAlreadyExistException;
import com.example.tournaments_backend.registration.token.ConfirmationToken;
import com.example.tournaments_backend.registration.token.ConfirmationTokenService;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AppUserService implements UserDetailsService {
    private final static String USER_NOT_FOUND_MSG = 
        "user with email %s not found";
    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;

    public List<AppUser> getAppUsers() {
        return appUserRepository.findAll();
    }

    public AppUser getAppUserByEmail(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository
                            .findAppUserByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        System.out.println("person service " + appUser);
        return appUser;
    }

    public void addUser(AppUser appUser) {
        Optional<AppUser> appUserOptional = appUserRepository.findAppUserByEmail(appUser.getEmail());
        if (appUserOptional.isPresent()) {
            throw new IllegalStateException("email taken");
        }
        appUserRepository.save(appUser);
    }

    public void deleteUser(Long userId) {
        boolean exists = appUserRepository.existsById(userId);
        if (!exists) {
            throw new IllegalStateException(
                "User with " + userId + " does not exists");
        }
        appUserRepository.deleteById(userId);
    }

    public void updateUser(Long userId, AppUser updatedAppUser) {
        Optional<AppUser> optionalAppUser = appUserRepository.findById(userId);
        if (!optionalAppUser.isPresent()) {
            throw new IllegalStateException(
                "person with " + userId + " does not exists");
        }
        AppUser updatePerson = optionalAppUser.get();
        updatePerson.setLastName(updatedAppUser.getLastName());
        appUserRepository.save(updatePerson);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository
            .findAppUserByEmail(email)
            .orElseThrow(() -> 
                new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }

    public String signUp(AppUser appUser) throws UserAlreadyExistException {
        boolean userExists = appUserRepository
            .findAppUserByEmail(appUser.getEmail())
            .isPresent();
        
        if (userExists) {
            throw new UserAlreadyExistException("User already exists");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);
        appUserRepository.save(appUser);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, 
                                                                    LocalDateTime.now(), 
                                                                    LocalDateTime.now().plusMinutes(15),
                                                                    appUser);
        confirmationTokenService.saveConfirmationToken(confirmationToken);

        return token;
    }

    @Transactional
    public String generateNewTokenForUser(AppUser appUser) {
        confirmationTokenService.invalidateUnconfirmedTokens(appUser);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, 
                                                                    LocalDateTime.now(), 
                                                                    LocalDateTime.now().plusMinutes(15),
                                                                    appUser);
        confirmationTokenService.saveConfirmationToken(confirmationToken);

        return token;
    }

    public void enableAppUser(String email) {
        AppUser user = appUserRepository
                .findAppUserByEmail(email)
                .orElseThrow(() -> 
                    new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
        user.setEnabled(true);
        appUserRepository.save(user);
    }
}
