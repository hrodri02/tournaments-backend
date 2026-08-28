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

- ~~Whether the direct `io.lettuce.core.RedisException` catch should be replaced with a Spring Data abstraction once this upgrade lands. It would make the fail-closed path independent of the driver, at the cost of catching a broader class of failure than intended. Worth a follow-up change; explicitly out of scope here.~~ **Resolved: yes, in a follow-up, and this change produced the evidence.** A control probe deleted the `io.lettuce.core.RedisException` arm and `failsClosed_whenRedisUnreachable` still passed — Spring Data Redis 4.1.1 translates the Lettuce 7 connection failure into a `DataAccessException` before the limiter sees it, so the driver catch never fires on that path. The stated cost is also smaller than it looked: `DataAccessException` is already the arm doing the work. Two preconditions before removing it, neither met yet — widen the evidence past connection-refused to the timeout path, and cover that path in the suite first. Sequence: add the timeout-path test, re-run the probe against it, then remove the arm.

## Findings carried forward

The upgrade itself was inert: `pom.xml` alone, no source change, 119 tests unchanged, JSON wire format identical against both the 3.5.16 and 4.0.8 captures. Everything below came out of the verification rather than the diff, and is recorded here because `tasks.md` is not tracked in git.

**The fail-closed contract was broken by latency, not logic — and the runtime check is what caught it.** With Redis stopped, the client received `504 Gateway Time-out` from nginx instead of the required `503`. Measured against a backend directly, bypassing nginx, the application returned `{"errorKey":"RATE_LIMIT_UNAVAILABLE","status":503,…}` in **60.2 seconds**. Lettuce defaults to a 60s command timeout and nginx to a 60s `proxy_read_timeout`, so nginx gave up at the same instant. The limiter had already failed closed; it could not say so before nginx stopped listening. Fixed by bounding the wait — `spring.data.redis.timeout=1s` and `connect-timeout=1s` — which brings the correct 503 back in 1.11s. Raising nginx's timeout would have "fixed" it too, and wrongly: it makes every client wait a minute to be told the limiter is down.

**The designated release-gate test cannot see that failure.** `failsClosed_whenRedisUnreachable` points at `localhost:63999`, where the connection is *refused* in milliseconds. A stopped container *hangs* until the command timeout instead. This document called that test "the only guard against a fail-open regression", and it does guard the refusal path — but the path a real outage produces was never covered, which is exactly why the suite was green while the runtime check failed. A test exercising a hanging Redis is the gap to close, and it gates the Open Question resolved above.

**Attribution for the 60s behaviour is open.** 60s is Lettuce's default command timeout in both 6.x and 7.x, and neither the application nor nginx configured a timeout before this change, so it was very likely pre-existing rather than caused by Lettuce 7. That was never empirically confirmed against 4.0.8, and no part of this record claims it was.

**Scope note.** `application.properties` is a third file, beyond the pom-only Impact this change declared. It was added on an explicit decision to fix the contract rather than record the gap, and is a candidate to split out — the same shape as the maildev fix in `upgrade-spring-boot-4-0`.
