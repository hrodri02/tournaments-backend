package com.example.tournaments_mcp.transport;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * The MCP endpoint: a servlet, a server, and the one line that carries the
 * caller's {@code Authorization} header into a tool invocation.
 *
 * <p>All three beans are written out here because nothing autoconfigures them.
 * That is the whole difference between this branch and the Spring AI one: there
 * {@code McpServerStatelessWebMvcAutoConfiguration} built the equivalent of all
 * three, and this class existed only to override the transport's default
 * context extractor, which discards every header.
 *
 * <p>The transport chosen is the stateless one. Each JSON-RPC call is a
 * self-contained POST, which is what lets the caller's Authorization header be
 * the unit of identity; a session-based transport would establish identity once
 * at initialize and reuse it, which is the stdio model with extra steps. On the
 * Spring AI branch that choice was {@code spring.ai.mcp.server.protocol=
 * STATELESS} in a properties file. Here it is which class gets constructed --
 * less configurable, but there is no longer a property that can silently
 * disagree with the code.
 *
 * <p>Only the one header is lifted. A context extractor is a place where the
 * whole request is briefly in reach, and copying all of it would make every
 * client header reachable from tool code by accident.
 */
@Configuration
public class McpTransportConfig {

    /** Key under which the caller's Authorization header is stored. */
    public static final String AUTHORIZATION_KEY = "authorization";

    /**
     * The SDK's view of Jackson, over the mapper Spring Boot already configured.
     *
     * <p>Boot 4.1 ships Jackson 3, so the bean being wrapped is
     * {@code tools.jackson.databind.JsonMapper}. The records in {@code backend}
     * still carry {@code com.fasterxml.jackson.annotation} annotations, which
     * Jackson 3 continues to read -- that package did not move.
     */
    @Bean
    public McpJsonMapper mcpJsonMapper(JsonMapper jsonMapper) {
        return new JacksonMcpJsonMapper(jsonMapper);
    }

    /**
     * The servlet that speaks JSON-RPC over HTTP.
     *
     * <p>{@code messageEndpoint} is not routing -- {@link ServletRegistrationBean}
     * below does that. The transport compares it against the request URI and
     * answers 404 when they differ, so the two must be given the same path.
     */
    @Bean
    public HttpServletStatelessServerTransport mcpTransport(
            McpJsonMapper mcpJsonMapper,
            @Value("${tournaments.mcp.endpoint}") String endpoint) {
        return HttpServletStatelessServerTransport.builder()
                .jsonMapper(mcpJsonMapper)
                .messageEndpoint(endpoint)
                .contextExtractor(McpTransportConfig::extractAuthorization)
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStatelessServerTransport> mcpServlet(
            HttpServletStatelessServerTransport transport,
            @Value("${tournaments.mcp.endpoint}") String endpoint) {
        ServletRegistrationBean<HttpServletStatelessServerTransport> registration =
                new ServletRegistrationBean<>(transport, endpoint);
        registration.setName("mcpServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    /**
     * Binds the tools to the transport.
     *
     * <p>Tool argument validation is left at the SDK's default of on: a call
     * whose arguments do not match the schema declared in {@code ToolCatalog}
     * comes back as an error result before any tool code runs.
     *
     * <p>{@code immediateExecution} is left at its default of off, so a tool
     * body runs on a bounded-elastic thread rather than the servlet thread.
     * That is safe here only because the caller's token reaches the tool as a
     * value -- see {@link CallerToken}. It is the assumption the in-process
     * HTTP variant could not make, since its principal lived in a ThreadLocal.
     */
    @Bean
    public McpStatelessSyncServer mcpServer(
            HttpServletStatelessServerTransport transport,
            List<SyncToolSpecification> tools,
            @Value("${tournaments.mcp.name}") String name,
            @Value("${tournaments.mcp.version}") String version) {
        return McpServer.sync(transport)
                .serverInfo(name, version)
                .capabilities(ServerCapabilities.builder()
                        // The argument is listChanged, not "has tools". The set
                        // is fixed at startup, so no client ever needs telling
                        // that it changed.
                        .tools(false)
                        .build())
                .tools(tools)
                .build();
    }

    private static McpTransportContext extractAuthorization(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return (header == null)
                ? McpTransportContext.EMPTY
                : McpTransportContext.create(Map.of(AUTHORIZATION_KEY, header));
    }
}
