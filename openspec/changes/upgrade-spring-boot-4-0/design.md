## Context

See `proposal.md` — Why for the version table and the sequencing rationale. Requirements are in `specs/runtime-platform/spec.md` and `specs/testcontainers-test-infrastructure/spec.md`; this document covers only how they are satisfied.

The decisions below turn on one fact that is not visible from the dependency list: the size of a major-version bump in the BOM tells you nothing about the size of the source change, and for Jackson the two differ by a factor of five.

## Goals / Non-Goals

**Goals:**
- Cross the Boot 4 boundary with the Redis/Lettuce major bump deliberately excluded.
- Establish whether the Hibernate 7 / Jakarta Persistence 3.2 transition affects the entity model, which no amount of dependency-metadata reading can answer.
- Confirm the JSON wire format is unchanged, since Jackson 3 is a rewrite of the binding layer that this API's responses flow through.

**Non-Goals:**
- Reaching the newest Boot release. 4.1.1 is a separate change.
- Adopting Boot 4 idioms — module-scoped autoconfiguration imports, the new HTTP client abstractions — beyond what compiling requires.
- Any Java version change, and any change to the rate limiter's Lua script or Redis interaction.

## Decisions

**Decision: land on 4.0.8, not 4.1.1.**
Boot 4.1 bumps Lettuce from 6.8 to 7.5. The rate limiter drives Redis through `LettuceConnectionFactory` and an atomic Lua script returning a raw `List`, and it is the one subsystem here with a hand-written protocol-level contract. Taking that major bump in the same change as Jackson 3, Hibernate 7, Testcontainers 2, and JUnit 6 means a Redis failure has five plausible causes. Deferring it costs one extra changeset and buys an unambiguous bisect.

**Decision: Jackson 3 is a two-file change, not a ten-file change.**
`jackson-bom:3.1.5` keeps `jackson-annotations` under the groupId `com.fasterxml.jackson.core` with its own independent version property, while core and databind move to `tools.jackson.*`. The annotations package is therefore *unchanged*, and every `@JsonBackReference`, `@JsonManagedReference`, and `@JsonIgnore` in the entity model stays exactly as written — `GameStat`, `ConfirmationToken`, `RefreshToken`, `League`, `Game`, `Team`, `AppUser`, `Player`. Only `ObjectMapper` relocates, in `RateLimitFilter.java:14` and `RateLimitFilterTest.java:17`.

This is recorded because the obvious inference — "a Jackson major bump moves every Jackson import" — is wrong, and acting on it means touching eight entity files and reviewing a diff that should not exist.

**Decision: fix the `@ServiceConnection` drift here rather than filing it separately.**
`openspec/specs/testcontainers-test-infrastructure/spec.md` already requires `@ServiceConnection`, but `AbstractIntegrationTest.java:15-24` starts the container in a static block and wires it through three `@DynamicPropertySource` registrations. Testcontainers 2.0 forces a visit to that file regardless, and the manual property wiring is precisely the surface a Testcontainers major bump is most likely to disturb. Adopting `@ServiceConnection` deletes that surface instead of migrating it, and closes a drift the previous change deliberately deferred.

**Decision: keep `spring-boot-autoconfigure-classic` in reserve, unused.**
Boot 4 splits autoconfiguration into per-technology modules and ships `spring-boot-autoconfigure-classic` to preserve the old `org.springframework.boot.autoconfigure.*` arrangement. Adopting it preemptively would mask whichever imports actually need attention. It is the fallback if the restructuring bites somewhere unanticipated, not the plan.

## Risks / Trade-offs

- **Hibernate 6.6 → 7.2 with Jakarta Persistence 3.2 is the principal unknown.** It lands on `AppUser` → `Player` inheritance plus two bidirectional many-to-manys, and dependency metadata cannot predict it. `ddl-auto=create-drop` converts mapping problems into startup failures in the integration tests, which is the fastest available signal. Budget for this being the bulk of the work.
- **Jackson 3 can change API responses without failing a single test.** The existing tests assert on domain objects and status codes, not serialized payloads, so a changed default — date representation, null handling, property inclusion — would pass CI and reach clients. This needs a before/after comparison of real response bodies, which is a verification gap rather than a code change.
- **`ObjectMapper` is no longer mutable after construction.** `RateLimitFilterTest.java:39` builds its mapper as `new ObjectMapper().registerModule(new JavaTimeModule())`; Jackson 3 constructs mappers through `JsonMapper.builder()`. The replacement is shorter, because `java.time` support no longer needs registering — but it is a rewrite of that line, not an import swap.
- **Jackson 3 throws unchecked exceptions.** `RateLimitFilter.java:95` calls `writeValue` inside the error-response path. If the method's `throws` clause narrows, the compiler will say so; the risk is the reverse case, where a now-unchecked `JacksonException` escapes a path that previously could not throw past the filter.
- **Testcontainers 2.0 is an API-cleanup release, not just a rename.** The coordinate changes are mechanical, but `PostgreSQLContainer`, `GenericContainer`, `withExposedPorts`, and `getMappedPort` all need checking against 2.x rather than assuming source compatibility.
- **Servlet 6.1 under Tomcat 11** is additive for the request/response and filter APIs the two filters use, so no change is expected — but this is reasoning from the spec's additivity, not from a green build.

## Migration Plan

Build and test change only. Verification is `./mvnw test`, then a full runtime pass: Swagger UI, an authenticated request cycle, `test-limit.sh` for the rate limiter, and a JSON response-body comparison against the 3.5.16 baseline.

Rollback is reverting to the `upgrade-spring-boot-3-5` baseline, which is why that change lands on `main` separately.

## Open Questions

- ~~How to capture the JSON baseline for comparison: recording a handful of real response bodies from the running 3.5.16 build is the cheap option; adding serialization assertions to the suite is the durable one and would close the gap permanently.~~ **Resolved: both.** The captures were needed to verify *this* upgrade — assertions written after the fact can only encode what the new build already does, so they cannot tell you whether it changed. They were kept out of the repo (see `tasks.md` 1.2). The durable assertions were then added as well, in `JsonSerializationContractTests`, so the next upgrade starts from a suite that fails on a wire-format change instead of a manual diff that someone has to remember to run.
- ~~Whether Hibernate 7 findings warrant their own follow-up change if the entity model needs more than mechanical adjustment.~~ **Resolved: no follow-up needed.** Hibernate 6.6.53 → 7.2.24 with Jakarta Persistence 3.2 required zero entity changes. The `AppUser` → `Player` inheritance and both bidirectional many-to-manys mapped unchanged, and `ddl-auto=create-drop` built the schema in the integration contexts without a mapping error. The risk section budgeted for this being the bulk of the work; it was none of it.
