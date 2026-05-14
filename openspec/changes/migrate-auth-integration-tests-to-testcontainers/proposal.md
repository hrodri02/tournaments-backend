## Why

The `AuthServiceIntegrationTests` currently relies on an externally provisioned PostgreSQL database (configured via environment variables), which creates a hard dependency on infrastructure setup — both locally and in CI. Testcontainers eliminates this by spinning up a real, isolated PostgreSQL container per test run, making tests fully self-contained and portable.

## What Changes

- Add a `@TestConfiguration` class (or shared base class / `@DynamicPropertySource`) that starts a Testcontainers PostgreSQL container and wires its URL/credentials into the Spring context, replacing the externally-supplied `SPRING_DATASOURCE_*` env vars.
- Annotate `AuthServiceIntegrationTests` (and any future integration test class) to use the Testcontainers-backed datasource rather than a pre-existing database.
- Update `application-test.properties` to remove the fallback to `localhost:5432/testdb` — the datasource will always come from the container.
- Simplify the GitHub Actions workflow (`maven-tests.yml`) by removing the `services: postgres` block and the redundant `SPRING_DATASOURCE_*` env var overrides, since Testcontainers manages its own container on the runner's Docker daemon.

## Capabilities

### New Capabilities

- `testcontainers-test-infrastructure`: Pattern and configuration for running Spring Boot integration tests against a real Testcontainers-managed PostgreSQL database, usable across all integration test classes in the project.

### Modified Capabilities

## Impact

- **Test files**: `AuthServiceIntegrationTests.java` — new annotation / base class usage.
- **Test config**: `application-test.properties` — datasource defaults removed or replaced with Testcontainers dynamic values.
- **CI workflow**: `.github/workflows/maven-tests.yml` — `services: postgres` block and associated env vars can be removed; Docker is already available on `ubuntu-latest` runners.
- **Dependencies**: `spring-boot-testcontainers` and `org.testcontainers:postgresql` are already present in `pom.xml`.
- **No application code changes** — this is purely a test infrastructure migration.
