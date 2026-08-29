package com.example.tournaments_mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * An MCP server for the tournaments API, running in its own process.
 *
 * <p>Both hops are HTTP: the MCP client speaks streamable HTTP to this process,
 * and this process speaks REST to the backend. It holds no datasource, no JPA
 * mappings, no mail configuration and no JWT keys -- everything it knows about
 * tournaments it learns from {@code /api/v1}, as any other API client would.
 *
 * <p>That is what separates this variant from the two in-process ones. There,
 * the MCP tools called {@code LeagueService} and {@code TeamService} directly
 * and inherited the application's database access; here the backend stays the
 * sole authority on data and on who is allowed to see it.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
