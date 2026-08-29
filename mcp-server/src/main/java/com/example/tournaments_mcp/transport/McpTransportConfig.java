package com.example.tournaments_mcp.transport;

import java.util.Map;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carries the caller's {@code Authorization} header from the HTTP request into
 * the tool invocation.
 *
 * <p>This bean exists only to replace one default. Spring AI's
 * {@code McpServerStatelessWebMvcAutoConfiguration} builds the same transport,
 * but {@code WebMvcStatelessServerTransport.Builder} defaults its context
 * extractor to one that returns {@link McpTransportContext#EMPTY}: every HTTP
 * header is discarded before a tool ever runs. Since the autoconfigured bean is
 * {@code @ConditionalOnMissingBean}, declaring our own here is enough -- the
 * autoconfigured router function still picks it up and the endpoint path still
 * comes from {@code spring.ai.mcp.server.streamable-http.mcp-endpoint}.
 *
 * <p>Only the one header is lifted. A context extractor is a place where the
 * whole request is briefly in reach, and copying all of it would make every
 * client header reachable from tool code by accident.
 *
 * <p>If this bean is ever deleted, nothing fails to start and no test that
 * exercises a tool in isolation fails either -- every tool call simply comes
 * back reporting a missing Authorization header. {@code
 * TokenForwardingIntegrationTest} is the test that notices.
 */
@Configuration
public class McpTransportConfig {

    /** Key under which the caller's Authorization header is stored. */
    public static final String AUTHORIZATION_KEY = "authorization";

    @Bean
    public WebMvcStatelessServerTransport webMvcStatelessServerTransport(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint(serverProperties.getMcpEndpoint())
                .contextExtractor(McpTransportConfig::extractAuthorization)
                .build();
    }

    private static McpTransportContext extractAuthorization(ServerRequest request) {
        String header = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        return (header == null)
                ? McpTransportContext.EMPTY
                : McpTransportContext.create(Map.of(AUTHORIZATION_KEY, header));
    }
}
