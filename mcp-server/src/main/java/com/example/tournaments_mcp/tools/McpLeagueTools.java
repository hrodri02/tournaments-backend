package com.example.tournaments_mcp.tools;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.tournaments_mcp.backend.LeagueDetail;
import com.example.tournaments_mcp.backend.LeagueStatus;
import com.example.tournaments_mcp.backend.LeagueSummary;
import com.example.tournaments_mcp.backend.TournamentsBackendClient;
import com.example.tournaments_mcp.transport.CallerToken;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * Read-only league tools.
 *
 * <p>Plain methods: what makes them tools is {@code ToolCatalog}, which pairs
 * each one with the schema a client sees and the handler that unpacks a call
 * into these arguments. On the Spring AI branch the pairing was implicit --
 * {@code @McpTool} on the method, and a schema reflected out of the signature.
 *
 * <p>The {@link McpTransportContext} parameter comes first by convention and is
 * deliberately absent from the tool's input schema: the caller's credential
 * arrives beside the model's arguments without ever being something the model
 * can set.
 */
@Component
public class McpLeagueTools {

    private final TournamentsBackendClient backend;

    public McpLeagueTools(TournamentsBackendClient backend) {
        this.backend = backend;
    }

    public List<LeagueSummary> listLeagues(McpTransportContext context, LeagueStatus status) {
        return backend.leagues(CallerToken.from(context), status);
    }

    public LeagueDetail getLeague(McpTransportContext context, Long leagueId) {
        return backend.league(CallerToken.from(context), leagueId);
    }
}
