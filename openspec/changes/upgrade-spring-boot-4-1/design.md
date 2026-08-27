## Context

See `proposal.md` — Why for why the Lettuce bump is isolated into its own change. Requirements are in `specs/runtime-platform/spec.md`; the `rate-limiting` capability is unchanged and is re-verified rather than modified.

## Goals / Non-Goals

**Goals:**
- Reach the current Boot 4 release with the Lettuce major bump attributable to a single changeset.
- Prove the rate limiter's fail-closed stance survives, rather than assuming a green suite implies it.

**Non-Goals:**
- Replacing the hand-written Lua script or the raw `List` reply handling with a typed abstraction. That is a worthwhile change and a separate one; conflating it with a driver upgrade would destroy this change's diagnostic value.
- Moving off the direct `io.lettuce.core` dependency in application code, for the same reason.

## Decisions

**Decision: verify fail-closed behaviorally, not by reading the catch clause.**
`SlidingWindowRateLimiter.java:61` catches `DataAccessException | io.lettuce.core.RedisException` and converts either into `RateLimitUnavailableException`, which is what produces the `503` / `RATE_LIMIT_UNAVAILABLE` response the `rate-limiting` capability requires. A Lettuce major can break this in two ways, and only one of them is loud:

- The type moves or is renamed → compile error. Self-announcing, harmless.
- A connection failure begins surfacing as some *other* exception that neither arm catches → it propagates past the limiter, the `503` contract is silently replaced by whatever the filter chain does with an unexpected exception, and **the limiter has failed open under infrastructure failure**. Nothing in a compile or a happy-path test reveals this.

`SlidingWindowRateLimiterIntegrationTest` already exercises the unreachable-Redis path against a real container, so the guard exists. The decision is to treat that specific test as a release gate for this change, not merely as part of the suite.

**Decision: treat the `EVAL` reply shape as a contract to re-check.**
The limiter reads positional values from a raw `List` and casts index 0 to `Number` (`SlidingWindowRateLimiter.java:70`). The `Number` cast already absorbs a `Long`/`Integer` swap, which is the likely variation; a reply arriving as `String` or a nested structure would throw `ClassCastException` at runtime inside the filter. The Lua script itself is unchanged, so any difference here originates in the driver's reply decoding.

## Risks / Trade-offs

- **A silent fail-open is the worst outcome of this change**, and it is invisible to the compiler, to the happy-path tests, and to a Swagger smoke check. The integration test covering an unreachable Redis is the only thing standing in front of it.
- **Spring Data 2025.1 → 2026.0 moves the whole repository layer**, not just Redis. Nothing in the JPA repositories reaches past the abstraction, so no impact is expected — but the version train crossing a year boundary is a larger jump than the Boot minor suggests.
- **Hibernate 7.2 → 7.4 lands on an entity model that the previous change may already have had to adjust.** If Boot 4.0 required mapping changes, this bump revisits the same surface with less warning, since a patch-level Hibernate expectation is easy to under-budget.
- **`LettuceConnectionFactory` is constructed directly in a test** (`SlidingWindowRateLimiterIntegrationTest.java:61`) rather than obtained from Spring, so a constructor or lifecycle change in Spring Data Redis surfaces there first and as a test-only failure — misleading, because it indicates a driver change that production wiring may also feel.

## Migration Plan

Build change only. Verification is the full suite, then `test-limit.sh` against the Compose stack, then an explicit fail-closed check: stop the Redis container, confirm a request returns `503` with `errorKey` `RATE_LIMIT_UNAVAILABLE`, restart it, confirm normal throttling resumes.

Rollback is reverting to the `upgrade-spring-boot-4-0` baseline.

## Open Questions

- Whether the direct `io.lettuce.core.RedisException` catch should be replaced with a Spring Data abstraction once this upgrade lands. It would make the fail-closed path independent of the driver, at the cost of catching a broader class of failure than intended. Worth a follow-up change; explicitly out of scope here.
