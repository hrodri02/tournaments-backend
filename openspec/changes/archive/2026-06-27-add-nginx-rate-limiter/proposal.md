## Why

The backend currently exposes the Spring Boot app directly on port 8080 with no protection against request floods or abusive clients. A single misbehaving or malicious client can exhaust application resources (DB connections, threads) and degrade service for everyone. Adding an nginx reverse proxy in front of the app gives us a dedicated, battle-tested layer for rate limiting before traffic ever reaches the JVM.

## What Changes

- Add a new `nginx` service to `devops/local/docker-compose.yml` that acts as a reverse proxy in front of the `backend` service.
- nginx becomes the single public entry point (published on port `80`); the `backend` service stops publishing port `8080` to the host and is reachable only on the internal compose network.
- Add an nginx configuration that defines a rate-limiting zone (`limit_req_zone`) and applies a per-client request limit with a burst allowance to all proxied API traffic.
- nginx forwards client information (`X-Forwarded-For`, `X-Forwarded-Proto`, `Host`) to the backend so the app sees real client context.
- Update `devops/local/README.md` and the `.env` conventions to document the new entry point and any tunable rate-limit values.

## Capabilities

### New Capabilities
- `api-rate-limiting`: Defines the reverse-proxy rate-limiting behavior in front of the application — request-rate limits per client, burst handling, the rejection response for clients that exceed limits, and pass-through of legitimate traffic to the backend.

### Modified Capabilities
<!-- No existing specs in openspec/specs/; nothing to modify. -->

## Impact

- **Infrastructure**: `devops/local/docker-compose.yml` (new `nginx` service, removed host port mapping on `backend`), new `devops/local/nginx/nginx.conf` (or equivalent), `devops/local/.env` (rate-limit tunables), `devops/local/README.md`.
- **Access pattern**: Local API access moves from `http://localhost:8080` to `http://localhost:80`. Swagger UI and all endpoints are reached through nginx.
- **Dependencies**: Adds the official `nginx` image to the local stack. No application code or Java dependencies change.
- **Developers**: Anyone running the stack locally must rebuild/restart compose and use the new port.
