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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping(path="/api/v1/users")
@Tag(name = "User Management", description = "API endpoints for managing users")
public class AppUserController {
    private final AppUserService appUserService;

    @Autowired
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Operation(summary = "Get current user information", description = "Returns the details of the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user information", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
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

    @Operation(summary = "Get all users", description = "Returns a list of all users in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
    @GetMapping
    public List<AppUser> getPersons() {
        return appUserService.getAppUsers();
    }

    @Operation(summary = "Add a new user", description = "Creates a new user in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
    @PostMapping
    public void addUser(@RequestBody AppUser user) {
        appUserService.addUser(user);
    }

    @Operation(summary = "Delete a user", description = "Deletes a user from the system by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully deleted"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
    @DeleteMapping(path = "{userId}")
    public void deleteUser(@PathVariable("userId") Long userId) {
        appUserService.deleteUser(userId);
    }

    @Operation(summary = "Update a user", description = "Updates an existing user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated")
    })
    @PutMapping("{userId}")
    public void updateUser(@PathVariable("userId") Long userId, @RequestBody AppUser updatedAppUser) {
        appUserService.updateUser(userId, updatedAppUser);
    }
}
