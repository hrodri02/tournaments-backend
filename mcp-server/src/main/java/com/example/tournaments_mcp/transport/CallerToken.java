package com.example.tournaments_mcp.transport;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * The bearer token of whoever made the MCP call, on its way to the backend.
 *
 * <p>This process never decodes or validates it. It cannot: it has no copy of
 * the RSA public key the backend signs with, and giving it one would make two
 * services responsible for the same decision. The token is carried through and
 * the backend's filter chain rules on it, so a tool is exactly as authorized as
 * the human whose token it is -- no more, and no less.
 *
 * <p>Reaching the tool as a value rather than a {@code ThreadLocal} is the
 * quiet advantage this variant has over the in-process HTTP server, where the
 * principal lived in {@code SecurityContextHolder} and stayed visible only
 * while the tool body happened to run on the request thread.
 *
 * @param headerValue the verbatim {@code Authorization} header, scheme included
 */
public record CallerToken(String headerValue) {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extracts the caller's token from the transport context populated by
     * {@link McpTransportConfig}.
     *
     * @throws IllegalStateException when the client sent no usable credential.
     *     The MCP SDK turns a thrown exception into an error tool result, so the
     *     message here is what the model and the user actually read; it names
     *     the fix rather than the symptom.
     */
    public static CallerToken from(McpTransportContext context) {
        Object header = context.get(McpTransportConfig.AUTHORIZATION_KEY);
        if (header == null) {
            throw new IllegalStateException(
                    "This request carried no Authorization header. The tournaments MCP "
                    + "server acts as the calling user and has no identity of its own, so "
                    + "the client must send one: configure the server with a header of "
                    + "\"Authorization: Bearer <jwt>\", using a token from "
                    + "POST /api/v1/auth/login.");
        }

        String value = header.toString();
        if (!value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                || value.substring(BEARER_PREFIX.length()).isBlank()) {
            throw new IllegalStateException(
                    "The Authorization header is not a bearer token. The tournaments API "
                    + "accepts \"Bearer <jwt>\" only.");
        }

        return new CallerToken(value);
    }
}
