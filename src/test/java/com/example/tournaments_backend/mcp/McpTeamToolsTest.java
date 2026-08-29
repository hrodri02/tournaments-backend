package com.example.tournaments_backend.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.team.GetTeamsResponse;
import com.example.tournaments_backend.team.TeamService;

@ExtendWith(MockitoExtension.class)
public class McpTeamToolsTest {
    @Mock
    private TeamService teamService;
    @InjectMocks
    private McpTeamTools mcpTeamTools;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Installs a principal the way the JWT filter chain does before a request
     * reaches a tool, so the tool is exercised against a populated context
     * rather than against an argument it was handed.
     */
    private void authenticateAs(String email) {
        AppUser appUser = new AppUser(
                "Ada",
                "Lovelace",
                email,
                "password",
                AppUserRole.PLAYER);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                appUser,
                null,
                appUser.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @Test
    void listMyTeams_ShouldCallTheServiceAsTheAuthenticatedUser() {
        // 1. Arrange
        authenticateAs("ada@example.com");
        when(teamService.getTeams(any(Authentication.class)))
                .thenReturn(new GetTeamsResponse(List.of(), List.of()));

        // 2. Act
        mcpTeamTools.listMyTeams();

        // 3. Assert
        // The caller's identity has to reach the service, otherwise the tool is
        // not user-scoped at all and would answer the same for everyone.
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(teamService).getTeams(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("ada@example.com");
    }

    @Test
    void listMyTeams_ShouldReturnTheServiceResponseUnchanged() {
        // 1. Arrange
        authenticateAs("ada@example.com");
        GetTeamsResponse expected = new GetTeamsResponse(List.of(), List.of());
        when(teamService.getTeams(any(Authentication.class))).thenReturn(expected);

        // 2. Act
        GetTeamsResponse result = mcpTeamTools.listMyTeams();

        // 3. Assert
        // Pins the decision not to trim the payload the way list_leagues does:
        // this answer is user-scoped and bounded, and the invites are its substance.
        assertThat(result).isSameAs(expected);
    }

    @Test
    void listMyTeams_ShouldFail_WhenNoAuthenticationIsPresent() {
        // 1. Arrange
        SecurityContextHolder.clearContext();

        // 2. Act & 3. Assert
        // Unreachable through the filter chain, which answers 401 first. It can
        // only fire if the tool stops running on the request thread, so the
        // message has to point at that rather than at the caller.
        assertThatThrownBy(() -> mcpTeamTools.listMyTeams())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("request thread");
        verifyNoInteractions(teamService);
    }
}
