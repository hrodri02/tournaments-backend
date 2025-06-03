package com.example.tournaments_backend.app_user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/api/v1/users")
public class AppUserController {
    private final AppUserService appUserService;

    @Autowired
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser appUser = appUserService.getAppUserByEmail(userDetails.getUsername());
        UserDTO resBody = new UserDTO(
            appUser.getId(), 
            appUser.getFirstName(),
            appUser.getLastName(),
            appUser.getEmail(),
            appUser.getAppUserRole());

        return ResponseEntity.ok(resBody);
    }

    @GetMapping
    public List<AppUser> getPersons() {
        return appUserService.getAppUsers();
    }

    @PostMapping
    public void addUser(@RequestBody AppUser user) {
        appUserService.addUser(user);
    }

    @DeleteMapping(path = "{userId}")
    public void deleteUser(@PathVariable("userId") Long userId) {
        appUserService.deleteUser(userId);
    }

    @PutMapping("{userId}")
    public void updateUser(@PathVariable("userId") Long userId, @RequestBody AppUser updatedAppUser) {
        appUserService.updateUser(userId, updatedAppUser);
    }
}
