## Context

The Spring Boot API (3.2.4, Java 23) has a single stateless `SecurityFilterChain` using an OAuth2 resource server for JWT auth. There is no request throttling today, so any client can hit any endpoint without bound. The app is containerized (docker-compose with `postgres`, `maildev`, `backend`) and now sits behind an `nginx` reverse proxy that is the only host-facing entry point, so `request.getRemoteAddr()` sees the proxy rather than the real client — the client IP must come from a forwarded header that the proxy controls. Domain errors are surfaced as `ErrorDetails` JSON via a `@RestControllerAdvice` (`GlobalExceptionHandler`), but that advice only runs for exceptions raised inside the DispatcherServlet — not for servlet filters.

## Goals / Non-Goals

**Goals:**
- Enforce one global per-IP request limit across all endpoints via servlet middleware.
- Use a sliding window counter backed by Redis so limits are shared across instances.
- Make the limit and window configurable, with a runtime enable/disable toggle.
- Return standards-friendly throttling responses (`429` + `Retry-After` + `X-RateLimit-*`) reusing the existing `ErrorDetails` shape.
- Fail closed when Redis is unavailable.
- Keep the logic unit-testable, with an integration test proving the real Redis Lua behavior.

**Non-Goals:**
- Per-endpoint or per-user/tiered limits (single global per-IP rule only).
- Distributed-clock correctness (single app clock is used for window math).
- In-app trusted-proxy validation (validating the connecting peer against a known-proxy allowlist, or parsing an untrusted multi-hop XFF chain) — spoofing is instead handled at the edge by having `nginx` overwrite `X-Forwarded-For`, so the app trusts its first entry.
- A production docker-compose profile (only the local dev stack is touched).

## Decisions

### Sliding window counter with an atomic Redis Lua script
The increment and the allow/deny decision run as one Lua script so there is no read-decide-write race under concurrency. The script `INCR`s the current window counter, `GET`s the previous window counter, computes `estimate = current + previous * weight`, and returns `{allowed, estimate}`. Keys are `ratelimit:{ip}:{windowNumber}` with `TTL = 2 * windowSeconds` so old windows self-expire.
- *Alternative — Java-side INCR then GET:* simpler to read but not atomic; two concurrent requests can both pass at the boundary. Rejected.
- *Alternative — bucket4j / Redisson:* bucket4j is token-bucket (not the requested algorithm) and adds heavy dependencies. Rejected.

### Window math in Java with an injectable `Clock`
`SlidingWindowRateLimiter` uses `System`/`Clock` time to compute `windowNumber` and the previous-window `weight = 1 - (elapsedInWindow / windowSize)`, passing them to the script. Injecting `java.time.Clock` makes the window/weight math deterministic in unit tests. The single app clock is authoritative (not Redis `TIME`), keeping the script portable and the math testable.

### `nginx` reverse proxy overwrites `X-Forwarded-For`
An `nginx` service sits in front of `backend` and is the only container whose port is published to the host; `backend` is reachable only over the internal compose network. nginx sets the forwarded header with `proxy_set_header X-Forwarded-For $remote_addr;`, which **overwrites** (not appends) any `X-Forwarded-For` the client sent with the real connecting peer IP. This makes the app's "first XFF entry" rule trustworthy — a client cannot forge the header to scatter its requests across Redis buckets and bypass the limit, because the value is discarded and rewritten at the edge before the request reaches the app.
- *Alternative — append with `$proxy_add_x_forwarded_for`:* preserves the upstream client chain for logging, but leaves the attacker-controlled value as the *first* entry, forcing the app to read the rightmost entry instead. Rejected — nothing here needs the upstream chain, and overwrite keeps the app-side rule simple.

### Register the filter inside the existing SecurityFilterChain
`http.addFilterBefore(rateLimitFilter, ...)` places the limiter before the auth filters, so it covers every route including the `permitAll` `auth` endpoints and rejects floods before auth work happens. An `OncePerRequestFilter` guarantees the check runs exactly once per client request even when the servlet container re-dispatches it internally (FORWARD to an error page, or ASYNC re-dispatch for `DeferredResult`/`Callable` handlers) — a plain `Filter` would re-run on each dispatch and, because the Lua script `INCR`s on every pass, consume multiple slots for one request and throttle clients early.
- *Alternative — `FilterRegistrationBean`:* ordering relative to the security chain is harder to reason about. Rejected.

### Filter writes the error body directly
Because the filter runs before `@RestControllerAdvice`, it serializes an `ErrorDetails` object to the response with the injected Jackson `ObjectMapper` and sets status/headers itself, rather than throwing and hoping the advice catches it.

### Fail closed on Redis failure
A `RedisConnectionFailureException` (or any `RedisException`) from the script call is translated to `RateLimitUnavailableException`, which the filter maps to `503` (`RATE_LIMIT_UNAVAILABLE`). This matches the agreed "fail closed" stance and keeps the semantics distinct from a client hitting its limit (`429`).

### Response headers
On every checked response: `X-RateLimit-Limit`, `X-RateLimit-Remaining` (`max(0, limit - estimate)`), `X-RateLimit-Reset` (seconds until the current window ends). On `429`, additionally `Retry-After` (seconds until the window rolls over, min 1).

## Risks / Trade-offs

- **Rejected requests still consume a slot** (the script `INCR`s before deciding) → acceptable and mildly beneficial against abuse; recovery is within one window (1s default).
- **XFF spoofing** is neutralized by `nginx` overwriting `X-Forwarded-For` with the real peer IP → the residual risk is a client reaching `backend` directly and bypassing nginx, mitigated by publishing only the nginx port to the host and leaving `backend` unpublished (internal network only).
- **Fail-closed means a Redis outage becomes an API outage** → mitigated by the `ratelimit.enabled=false` kill-switch and Redis' healthcheck + `depends_on` in compose.
- **Sliding window counter is an approximation** (weights the previous window linearly) → intentional; it smooths fixed-window edges at far lower cost than a true per-request log.
- **Filter runs before auth for all requests** → the Redis round-trip is on the hot path; mitigated by a single atomic script call and Lettuce connection pooling. *Revisit during implementation:* Lettuce is thread-safe and multiplexes commands over a single shared connection by default, so explicit `commons-pool2` pooling may be redundant — confirm whether we enable pooling deliberately or rely on the shared-connection model before treating "connection pooling" as the mitigation.

## Migration Plan

Additive change — no schema or data migration. Deploy requires a reachable Redis and now routes all client traffic through the new `nginx` reverse proxy (both added to the local compose stack); the `backend` port is no longer published to the host, so clients reach the app via nginx only. Rollback: set `ratelimit.enabled=false` (filter passes through) or revert the branch; no persisted state to clean up (keys are ephemeral with TTL).
