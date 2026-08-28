package com.example.tournaments_backend.mcp;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserService;

import jakarta.annotation.PostConstruct;

/**
 * The identity every tool in the stdio server acts as.
 *
 * <p>Over HTTP the filter chain builds an {@link Authentication} from a bearer
 * token. A stdio server has no request and therefore no principal, so the
 * identity is supplied once at launch by the client that spawns the process
 * ({@code TOURNAMENTS_USER}) and resolved here. One process serves one user,
 * which is exactly the shape of stdio: the client spawns a process per session.
 *
 * <p>Trust is process-level. Whoever can launch this JVM already holds the
 * datasource credentials from the same client configuration, so requiring a
 * password here would protect nothing.
 *
 * <p>Restricted to the {@code mcp} profile deliberately. If this bean existed in
 * the web application it would pin every HTTP request to a single user.
 */
@Component
@Profile("mcp")
public class McpIdentity {

    private final AppUserService appUserService;
    private final String configuredEmail;

    private Authentication authentication;

    public McpIdentity(
            AppUserService appUserService,
            @Value("${mcp.identity.email:}") String configuredEmail) {
        this.appUserService = appUserService;
        this.configuredEmail = configuredEmail;
    }

    /**
     * Resolves the configured identity at startup so a bad configuration fails
     * the boot rather than every tool call. Over stdio a failed start is
     * reported by the client immediately, whereas a null principal would surface
     * later as unexplained per-tool errors.
     */
    @PostConstruct
    void resolve() {
        if (!StringUtils.hasText(configuredEmail)) {
            throw new IllegalStateException(
                    "No MCP identity configured. Set TOURNAMENTS_USER to the email of the "
                    + "account this server should act as, for example "
                    + "TOURNAMENTS_USER=admin@example.com.");
        }

        // Throws UsernameNotFoundException when the address does not exist.
        AppUser appUser = appUserService.getAppUserByEmail(configuredEmail);
        this.authentication = UsernamePasswordAuthenticationToken.authenticated(
                appUser, null, appUser.getAuthorities());
    }

    /**
     * The resolved principal. Its {@code getName()} is the configured email,
     * which is all the service layer reads.
     */
    public Authentication authentication() {
        return authentication;
    }

    /**
     * Runs one tool invocation with the identity installed in the security
     * context, then clears it.
     *
     * <p>Installed per call rather than once at startup because
     * {@link SecurityContextHolder} is thread-local by default and tool calls are
     * served on different threads from the one that constructed this bean.
     * Populating the context, rather than only passing the {@link Authentication}
     * as an argument, is also what allows a tool method to carry its own
     * {@code @PreAuthorize} -- method security reads the context, not arguments.
     */
    public <T> T runAs(Supplier<T> action) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
