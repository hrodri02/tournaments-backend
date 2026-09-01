package com.example.tournaments_mcp.tools;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns what a tool method returns -- or throws -- into a {@link CallToolResult}.
 *
 * <p>This class exists because of one difference between the SDK and Spring AI's
 * wrapper, and it is the difference most likely to be missed. The SDK's
 * {@code DefaultMcpStatelessServerHandler} maps an exception escaping a tool
 * handler to a JSON-RPC {@code error} object: the call fails at the protocol
 * level, and a client is entitled to treat that as the server malfunctioning.
 * Spring AI's {@code SyncStatelessMcpToolMethodCallback} caught the exception
 * instead and returned a normal result carrying {@code isError: true}, which is
 * what the protocol intends for a tool that ran and failed -- the model reads
 * the message and can act on it.
 *
 * <p>The second behaviour is the one worth having, so every handler routes
 * through {@link #of}. Nothing about the SDK enforces that; forget it in one
 * tool and only that tool misbehaves, which is why
 * {@code ToolCatalogTest} asserts it per tool rather than once.
 */
@Component
public class ToolResults {

    private final JsonMapper jsonMapper;

    public ToolResults(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Runs a tool body and packages the outcome.
     *
     * <p>The value is serialised to JSON and returned as text content, matching
     * what the Spring AI branch produced -- {@code test-http.sh} parses
     * {@code .result.content[0].text} as JSON and would notice any other shape.
     *
     * <p>Only the message survives a failure, not the type or the stack. Both
     * {@code BackendException} and the {@code IllegalStateException} from
     * {@link com.example.tournaments_mcp.transport.CallerToken} carry messages
     * written for whoever ends up reading them, so there is nothing to add.
     */
    public CallToolResult of(Supplier<?> toolBody) {
        try {
            return CallToolResult.builder()
                    .addTextContent(jsonMapper.writeValueAsString(toolBody.get()))
                    .isError(false)
                    .build();
        }
        catch (RuntimeException ex) {
            return CallToolResult.builder()
                    .addTextContent(ex.getMessage())
                    .isError(true)
                    .build();
        }
    }
}
