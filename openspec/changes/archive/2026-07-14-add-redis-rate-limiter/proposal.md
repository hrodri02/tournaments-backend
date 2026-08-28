## Why

The API currently accepts unlimited requests from any client, leaving every endpoint — including the unauthenticated `auth` routes — exposed to brute-force, scraping, and accidental request floods. A per-IP rate limit backed by a shared store lets us cap abuse consistently, even across multiple application instances.

## What Changes

- Add a servlet-level rate-limiting middleware (`OncePerRequestFilter`) applied to **all** endpoints, enforcing one global per-IP request limit.
- Use the **sliding window counter** algorithm with **Redis** as the central data store, so the limit is shared across all app instances. The increment-and-decide step runs as an atomic Redis Lua script.
- Identify clients by IP: first `X-Forwarded-For` entry, falling back to `request.getRemoteAddr()`.
- Put an `nginx` reverse proxy in front of `backend` as the only host-exposed entry point; it **overwrites** `X-Forwarded-For` with the real client IP (`proxy_set_header X-Forwarded-For $remote_addr;`) so the first-XFF-entry rule can be trusted and clients cannot spoof their identity to dodge the limit.
- Make the limit configurable via `ratelimit.*` properties (`enabled`, `requests`, `window-seconds`), defaulting to **10 requests / 1 second**.
- **Fail closed**: if Redis is unreachable, reject the request with `503` (`RATE_LIMIT_UNAVAILABLE`). When a client exceeds its limit, respond with `429` (`RATE_LIMIT_EXCEEDED`).
- On `429`, return the existing `ErrorDetails` JSON body plus `Retry-After` and `X-RateLimit-Limit` / `X-RateLimit-Remaining` / `X-RateLimit-Reset` headers. The filter writes this JSON directly (via Jackson `ObjectMapper`) since it runs before `@RestControllerAdvice`.
- Add the `spring-boot-starter-data-redis` dependency, a `redis:7-alpine` container, and an `nginx` reverse-proxy container to the local docker-compose stack.

## Capabilities

### New Capabilities
- `rate-limiting`: Per-IP request throttling middleware using a Redis-backed sliding window counter, including client identification, limit configuration, throttled/unavailable responses, and rate-limit headers.

### Modified Capabilities
<!-- None: no existing spec's requirements change. -->

## Impact

- **New code** (`com.example.tournaments_backend`): `RateLimitProperties`, `ClientIpResolver`, `SlidingWindowRateLimiter`, `RateLimitFilter`, `RateLimitConfig`, `RateLimitUnavailableException`, and a classpath `sliding_window.lua` script.
- **Modified code**: `SecurityConfig` (register filter via `addFilterBefore`), `ClientErrorKey` (add `RATE_LIMIT_EXCEEDED`, `RATE_LIMIT_UNAVAILABLE`).
- **Dependencies**: adds `spring-boot-starter-data-redis` (Lettuce) to `pom.xml`.
- **Config**: `application.properties` gains `spring.data.redis.host/port` (default `localhost:6379`) and `ratelimit.*` defaults.
- **Infra**: `devops/local/docker-compose.yml` gains a `redis` service with a healthcheck; `backend` gets `depends_on: redis` and `SPRING_DATA_REDIS_HOST=redis`. It also gains an `nginx` reverse-proxy service (with a mounted config) as the host-facing entry point that forwards to `backend` and overwrites `X-Forwarded-For`; the `backend` port is no longer published to the host (internal network only).
- **Tests**: unit tests for `ClientIpResolver`, `SlidingWindowRateLimiter`, `RateLimitFilter`; a Testcontainers Redis integration test exercising the real Lua sliding-window behavior.
