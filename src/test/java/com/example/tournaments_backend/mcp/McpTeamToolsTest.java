package com.example.tournaments_backend.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.team.GetTeamsResponse;
import com.example.tournaments_backend.team.TeamService;

@ExtendWith(MockitoExtension.class)
public class McpTeamToolsTest {
    @Mock
    private TeamService teamService;
    @Mock
    private AppUserService appUserService;

    private McpIdentity identityFor(String email) {
        AppUser appUser = new AppUser(
                "Some",
                "Body",
                email,
                "password",
                AppUserRole.PLAYER);
        when(appUserService.getAppUserByEmail(email)).thenReturn(appUser);
        McpIdentity identity = new McpIdentity(appUserService, email);
        identity.resolve();
        return identity;
    }

    @Test
    void listMyTeams_ShouldCallTheServiceAsTheConfiguredUser() {
        // 1. Arrange
        McpIdentity identity = identityFor("player@example.com");
        McpTeamTools tools = new McpTeamTools(teamService, identity);
        when(teamService.getTeams(any(Authentication.class)))
                .thenReturn(new GetTeamsResponse(List.of(), List.of()));

        // 2. Act
        tools.listMyTeams();

        // 3. Assert
        // The identity has to reach the service, otherwise the tool is not
        // user-scoped at all and would answer the same for everyone.
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(teamService).getTeams(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("player@example.com");
    }

    @Test
    void listMyTeams_ShouldReturnTheServiceResponseUnchanged() {
        // 1. Arrange
        McpIdentity identity = identityFor("player@example.com");
        McpTeamTools tools = new McpTeamTools(teamService, identity);
        GetTeamsResponse expected = new GetTeamsResponse(List.of(), List.of());
        when(teamService.getTeams(any(Authentication.class))).thenReturn(expected);

        // 2. Act
        GetTeamsResponse result = tools.listMyTeams();

        // 3. Assert
        assertThat(result).isSameAs(expected);
    }
}
