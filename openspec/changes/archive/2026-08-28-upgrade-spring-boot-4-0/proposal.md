## Why

`upgrade-spring-boot-3-5` reached a green baseline on Boot 3.5.16 with a clean deprecation report. This change crosses the actual breaking boundary into Boot 4, where five dependencies bump a major version at once:

| | 3.5.16 | 4.0.8 |
|---|---|---|
| Spring Framework | 6.2.19 | 7.0.9 |
| Spring Security | 6.5.11 | 7.0.7 |
| Hibernate | 6.6.53 | 7.2.24 |
| Jackson | 2.21.4 | **3.1.5** |
| Tomcat | 10.1.55 | **11.0.24** (Servlet 6.1) |
| JUnit | 5.12.2 | **6.0.3** |
| Testcontainers | 1.21.4 | **2.0.5** |

We land on 4.0.8 rather than going straight to 4.1.1 so that Lettuce's 6.8 → 7.5 major bump — which sits underneath the Redis rate limiter — arrives in its own change rather than overlapping with these.

Despite that table, the verified source impact is narrow: two files change for Jackson, one test class for the mock-override APIs (already done in the previous change), and the Testcontainers coordinates. Spring Security, the servlet filters, and every JPA annotation were checked and need no edits.

## What Changes

- Bump the parent from `3.5.16` to `4.0.8`; bump springdoc from `2.9.0` to `3.0.3` in lockstep.
- **Jackson 3**: change the `ObjectMapper` import in two files from `com.fasterxml.jackson.databind` to `tools.jackson.databind`, and drop `JavaTimeModule` — `jackson-datatype-jsr310` does not exist at 3.x because `java.time` support moved into databind core. The Jackson *annotations* on all eight entity classes are unaffected.
- **Testcontainers 2.0**: every module artifactId gained a `testcontainers-` prefix, so `junit-jupiter` → `testcontainers-junit-jupiter` and `postgresql` → `testcontainers-postgresql`. The old coordinates do not exist at 2.0.5.
- Wire the PostgreSQL container with `@ServiceConnection`, bringing `AbstractIntegrationTest` in line with the requirement its own capability spec already states.
- Verify no JSON wire-format regression in API responses — Jackson 3 changes defaults, and this is a REST API.
- Absorb Hibernate 7 / Jakarta Persistence 3.2, Servlet 6.1, JUnit 6, and Spring Security 7.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `runtime-platform`: the Spring Boot baseline moves to 4.0.x and the OpenAPI tooling to springdoc 3.0.x. Adds a requirement covering the JSON binding library, since Jackson 3 splits databind and annotations across two package roots.
- `testcontainers-test-infrastructure`: container wiring becomes `@ServiceConnection` in fact rather than only on paper, and the Testcontainers module coordinates change.

## Impact

- **Build**: `pom.xml` — parent, springdoc, two Testcontainers artifactIds.
- **Production source**: `RateLimitFilter.java` — `ObjectMapper` import; possibly the `throws` clause, since Jackson 3 throws unchecked `JacksonException` where 2.x threw checked `JsonProcessingException`.
- **Tests**: `RateLimitFilterTest.java` — `ObjectMapper` import, `JavaTimeModule` removal, mapper construction. `AbstractIntegrationTest.java` — `@ServiceConnection` wiring.
- **Unchanged, verified**: `SecurityConfig` (`AuthenticationConfiguration` and `@EnableWebSecurity` both still ship in `spring-security-config` 7.x); both servlet filters' `jakarta.servlet` imports; `FilterRegistrationBean`'s package; all eight entities' Jackson annotations; every JUnit import.
- **No Java version change.** Boot 4's baseline is Java 17; `java.version` stays at 23.
