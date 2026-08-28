## Why

The application runs Spring Boot 3.2.4. The goal is Spring Boot 4.x, and Boot 4 has **removed** APIs this codebase uses — `spring-boot-test:4.1.1` contains no `MockBean`, `SpyBean`, or mockito classes at all. Jumping 3.2 → 4.x directly surfaces those removals as bare `cannot find symbol` errors with no indication of the replacement.

Boot 3.5 is the only release where every Boot 4 removal still exists as deprecated-but-working code. Landing there first turns the Boot 4 migration from guesswork into a compiler-generated worklist, and gives us a known-green baseline on `main` before five major-version dependency bumps land at once.

This is the first of three sequential changes: **3.5.16 → 4.0.8 → 4.1.1**.

## What Changes

- Bump the parent from `3.2.4` to `3.5.16` (latest 3.5 patch). Spring Framework 6.1 → 6.2, Spring Security 6.2 → 6.5, Hibernate 6.4 → 6.6, Jackson 2.15 → 2.21, Tomcat 10.1.19 → 10.1.55.
- Bump `springdoc-openapi-starter-webmvc-ui` from `2.2.0` to `2.9.0`. Required, not optional — see design.
- Remove the hardcoded `spring-security-test` version so the BOM manages it.
- Replace `@MockBean` / `@SpyBean` with `@MockitoBean` / `@MockitoSpyBean` in the one test class that uses them.
- Remove the `testcontainers.version` property override — Boot 3.5.16 manages exactly the version currently pinned (`1.21.4`).
- Re-evaluate the `byte-buddy` pin and the duplicated Lombok version now that the BOM moves underneath them.
- Replace the invalid `<source>23.0.2</source>` / `<target>23.0.2</target>` compiler settings with the `java.version` property.
- **Java stays at 23.** Boot 4's baseline is Java 17, so the JDK is not on the critical path for any step of this upgrade.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
<!-- None. This is a behavior-preserving platform upgrade: no endpoint, auth flow, rate-limiter, or data-model behavior changes. -->

## Impact

- **Build**: `pom.xml` — parent version; springdoc version; `spring-security-test` `<version>` removed; `testcontainers.version` property removed; `byte-buddy` and Lombok pins revisited; compiler `<source>`/`<target>` replaced.
- **Tests**: `src/test/java/com/example/tournaments_backend/auth/AuthServiceIntegrationTests.java` — two imports and two field annotations.
- **No production source changes expected.** `SecurityConfig`, both servlet filters, all JPA entities, and both `application*.properties` files were checked against the 3.5 baseline and need no edits.
- **No CI or Docker changes.** `java-version: '23'` in both workflows and the Temurin 25 images in `Dockerfile` are unaffected.
