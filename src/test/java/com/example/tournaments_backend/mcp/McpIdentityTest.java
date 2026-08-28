package com.example.tournaments_backend.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;

@ExtendWith(MockitoExtension.class)
public class McpIdentityTest {
    @Mock
    private AppUserService appUserService;

    private AppUser admin() {
        return new AppUser(
                "Admin",
                "User",
                "admin@example.com",
                "password",
                AppUserRole.ADMIN);
    }

    @Test
    void resolve_ShouldBuildAuthenticationNamedByEmail_WhenUserExists() {
        // 1. Arrange
        when(appUserService.getAppUserByEmail("admin@example.com")).thenReturn(admin());
        McpIdentity identity = new McpIdentity(appUserService, "admin@example.com");

        // 2. Act
        identity.resolve();

        // 3. Assert
        // getName() is the whole contract: TeamService reads it and looks the
        // user back up by email.
        assertThat(identity.authentication().getName()).isEqualTo("admin@example.com");
        assertThat(identity.authentication().isAuthenticated()).isTrue();
        assertThat(identity.authentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void resolve_ShouldFailStartup_WhenNoEmailConfigured() {
        // 1. Arrange
        McpIdentity identity = new McpIdentity(appUserService, "");

        // 2. Act & Assert
        // Refusing to start beats answering as nobody.
        assertThatThrownBy(identity::resolve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOURNAMENTS_USER");
    }

    @Test
    void resolve_ShouldFailStartup_WhenUserDoesNotExist() {
        // 1. Arrange
        when(appUserService.getAppUserByEmail("nobody@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found."));
        McpIdentity identity = new McpIdentity(appUserService, "nobody@example.com");

        // 2. Act & Assert
        assertThatThrownBy(identity::resolve)
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void runAs_ShouldInstallIdentityDuringTheCall_AndClearItAfter() {
        // 1. Arrange
        when(appUserService.getAppUserByEmail("admin@example.com")).thenReturn(admin());
        McpIdentity identity = new McpIdentity(appUserService, "admin@example.com");
        identity.resolve();

        // 2. Act
        // A tool method carrying @PreAuthorize reads the context, not arguments,
        // so the context has to be populated for the duration of the call.
        String nameDuringCall = identity.runAs(
                () -> SecurityContextHolder.getContext().getAuthentication().getName());

        // 3. Assert
        assertThat(nameDuringCall).isEqualTo("admin@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void runAs_ShouldClearTheContext_WhenTheActionThrows() {
        // 1. Arrange
        when(appUserService.getAppUserByEmail("admin@example.com")).thenReturn(admin());
        McpIdentity identity = new McpIdentity(appUserService, "admin@example.com");
        identity.resolve();

        // 2. Act & Assert
        // A leaked context would let the next tool call on this thread run as a
        // user it was never given.
        assertThatThrownBy(() -> identity.runAs(() -> {
            throw new IllegalArgumentException("boom");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
