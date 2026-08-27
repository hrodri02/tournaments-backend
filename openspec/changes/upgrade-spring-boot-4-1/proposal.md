## Why

`upgrade-spring-boot-4-0` crossed the Boot 4 boundary but deliberately stopped short of the newest release, because Boot 4.1 bumps Lettuce from 6.8 to **7.5** — a major version directly underneath the Redis rate limiter. This change takes that bump on its own, so a Redis failure has one plausible cause instead of five.

The rest of the delta is modest: Spring Security 7.0 → 7.1, Hibernate 7.2 → 7.4, Spring Data 2025.1 → 2026.0. Spring Framework stays at 7.0.9.

The rate limiter is the reason this change exists rather than being folded into the previous one. It reaches past Spring Data into the driver in two places: it catches `io.lettuce.core.RedisException` directly to enforce its fail-closed stance, and it reads a raw `List` reply from an `EVAL` of a hand-written Lua script. Both are exactly the kind of contract a driver major version is entitled to change.

## What Changes

- Bump the parent from `4.0.8` to `4.1.1`; bump springdoc from `3.0.3` to `3.1.0` in lockstep.
- Verify the fail-closed path still works under Lettuce 7 — the `rate-limiting` capability requires an unreachable Redis to produce HTTP `503` with `errorKey` `RATE_LIMIT_UNAVAILABLE`, and the code that guarantees it catches a Lettuce-internal exception type by name.
- Verify the Lua script's reply still deserializes — the limiter reads positional values out of a raw `List` and casts the first to `Number`.
- Absorb Spring Security 7.1, Hibernate 7.4, and the Spring Data version train.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `runtime-platform`: the Spring Boot baseline moves to 4.1.x and the OpenAPI tooling to springdoc 3.1.x.
<!-- rate-limiting is NOT modified. Its requirements are unchanged; this change re-verifies them against a new driver major. -->

## Impact

- **Build**: `pom.xml` — parent and springdoc versions only.
- **Production source, at risk not certain**: `SlidingWindowRateLimiter.java` — the `io.lettuce.core.RedisException` catch and the `List` reply handling.
- **Tests, at risk not certain**: `SlidingWindowRateLimiterIntegrationTest.java` — direct `LettuceConnectionFactory` construction.
- **No Java version change.**
