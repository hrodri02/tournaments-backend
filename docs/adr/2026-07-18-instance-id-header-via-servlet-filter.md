# Serving instance identified via a servlet filter response header

## Status
Accepted — 2026-07-18

## Context
To make the consistent-hash load balancing observable and assertable, the
serving backend instance needs to be identifiable from the HTTP response.
Several places to implement this were possible: per-controller logic, an
nginx-level header derived from the upstream address, or a single
application-level filter.

## Decision
A single `InstanceIdFilter` (`OncePerRequestFilter`) reads the instance's
`INSTANCE_ID` environment variable (via `InstanceIdConfig`, with a default
fallback so the app still boots outside the multi-container compose setup)
and sets it as the `X-Instance-Id` header on every response. It is
registered in the Spring Security filter chain *before* the existing
`RateLimitFilter`, so the header is present even on rate-limited (`429`) and
auth-rejected responses.

Rejected alternatives:
- **Per-controller logic** — would need to be duplicated (or centralized
  through an interceptor) across every endpoint; leaky and easy to miss on
  new controllers.
- **nginx `add_header $upstream_addr`** — only exposes a container IP:port,
  not a friendly, server-configured name, and isn't asserted or controlled
  from the application side.

## Consequences
- Every response, regardless of route or auth outcome, is taggable with the
  serving instance, which is what the load-balancing test scenarios (spread,
  stickiness, failover) assert against.
- `INSTANCE_ID` unset outside compose (e.g. local `./mvnw spring-boot:run` or
  plain unit tests) falls back to `InstanceIdFilter.DEFAULT_INSTANCE_ID`, so
  the app and its tests keep working unchanged.
- The filter runs unconditionally on every request; if a future need arises
  to disable it (e.g. to hide instance topology in a non-local environment),
  it would need an enable/disable flag analogous to `RateLimitProperties`.
