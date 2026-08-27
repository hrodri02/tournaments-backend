## Context

`AuthServiceIntegrationTests` is a `@SpringBootTest` integration test class that tests the full Spring context against a real PostgreSQL database. Currently, this database is provisioned externally — either by running `docker compose -f devops/local/docker-compose.yml up -d` locally or by the `services: postgres` block in the GitHub Actions workflow (`maven-tests.yml`). Datasource credentials are injected via `SPRING_DATASOURCE_*` environment variables with fallback defaults in `application-test.properties`.

The Testcontainers dependencies (`spring-boot-testcontainers` and `org.testcontainers:postgresql`) are already declared in `pom.xml`.

## Goals / Non-Goals

**Goals:**
- Make `AuthServiceIntegrationTests` fully self-contained: no external DB required to run locally or in CI.
- Establish a reusable Testcontainers pattern for all current and future integration test classes.
- Simplify the CI workflow by removing the redundant `services: postgres` block.

**Non-Goals:**
- Migrating unit tests (`AuthServiceTests`, `LeagueServiceTests`) — those don't hit the DB.
- Changing any application (non-test) code.
- Introducing a shared test application context across all test classes (each class gets its own container for isolation, unless profiling reveals startup time is prohibitive).

## Decisions

### Decision 1: Use `@ServiceConnection` with `@Bean` in a shared base class

**Chosen**: Create an abstract base class `AbstractIntegrationTest` (or a `@TestConfiguration` class annotated with `@ServiceConnection`) that declares the `PostgreSQLContainer` bean. `AuthServiceIntegrationTests` extends this class.

**Why**: Spring Boot 3.2+ `@ServiceConnection` automatically wires the container's dynamic URL, username, and password into the Spring datasource — no manual `@DynamicPropertySource` required. A shared base class avoids copy-pasting the container setup in every integration test class.

**Alternative considered**: `@DynamicPropertySource` static method on each test class. Works but is more verbose and requires every class to duplicate the setup. Rejected in favor of the cleaner `@ServiceConnection` approach available in Spring Boot 3.2.

**Alternative considered**: `@TestcontainersConfiguration` + `@ImportTestcontainers` (Spring Boot 3.2 style). Equivalent to the base class approach but uses class-level annotations instead of inheritance. Either is acceptable; base class chosen for simplicity given the small number of integration test classes.

### Decision 2: Container lifecycle — one container per test class (default)

**Chosen**: Let Spring manage the container lifecycle. With `@SpringBootTest` and a `@Bean`-declared container, Spring starts the container when the test application context starts and stops it when the context closes. Each test class that loads a new context gets its own container.

**Why**: Currently there is only one integration test class. Container reuse (`Testcontainers.reuse(true)`) adds complexity and can mask test isolation issues. The default is simpler and safer.

**Alternative considered**: Static container field with `@Container` + `withReuse(true)` for faster local runs. Can be revisited if startup time becomes a concern.

### Decision 3: Update CI workflow

**Chosen**: Remove the `services: postgres` block and the `SPRING_DATASOURCE_*` env vars from `maven-tests.yml`. Keep `SPRING_PROFILES_ACTIVE: test` and the JWT key secrets. Docker is pre-installed on `ubuntu-latest` runners, so Testcontainers works without additional setup.

**Why**: The service block becomes dead weight once Testcontainers manages its own container. Removing it reduces CI configuration and eliminates the risk of a port conflict between the service container and the Testcontainers container.

### Decision 4: Remove datasource defaults from `application-test.properties`

**Chosen**: Remove (or replace with a placeholder) the `spring.datasource.*` lines in `application-test.properties`. With `@ServiceConnection`, these values are overridden at runtime anyway, but leaving stale defaults is misleading.

## Risks / Trade-offs

- **Slower first-run locally**: Testcontainers pulls the `postgres:16-alpine` image on first use. After that, Docker caches it. → Mitigation: document this in the PR description.
- **Docker required locally**: Developers without Docker Desktop (or Podman/Colima) cannot run integration tests. → Mitigation: already a requirement since `docker compose` is used for local dev; no regression.
- **CI image pull latency**: The `ubuntu-latest` runner may need to pull the image. → Mitigation: GitHub Actions caches Docker layers between runs on the same runner; acceptable overhead.
- **Test isolation**: `@Transactional` rollback already handles per-test isolation within a class; the container itself is not reset between tests. → No change from current behavior (the workflow DB was also shared across the test run).

## Migration Plan

1. Create `AbstractIntegrationTest` base class with the `PostgreSQLContainer` `@Bean` and `@ServiceConnection`.
2. Update `AuthServiceIntegrationTests` to extend `AbstractIntegrationTest` and remove `@ActiveProfiles("test")` override if the base class handles it.
3. Remove `spring.datasource.*` lines from `application-test.properties`.
4. Update `maven-tests.yml` to remove the `services: postgres` block and associated env vars.
5. Run `./mvnw test` locally to verify all tests pass.
6. Push and confirm the CI workflow passes without the external Postgres service.

Rollback: revert commits in reverse order; all changes are additive or config-only.

## Open Questions

- Should `@ActiveProfiles("test")` live on the base class or stay on each test class? (Low stakes — base class is cleaner but either works.)
- Will `TournamentsBackendApplicationTests` (which also loads a Spring context) need to extend the same base class? Depends on whether it requires a DB.
