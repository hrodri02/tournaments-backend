package com.example.tournaments_mcp.backend;

/**
 * A call to the tournaments API did not succeed.
 *
 * <p>Its message is written for the model that receives it as an error tool
 * result, not for a log reader: it says what the backend refused and what would
 * make the next attempt work.
 */
public class BackendException extends RuntimeException {

    public BackendException(String message) {
        super(message);
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
