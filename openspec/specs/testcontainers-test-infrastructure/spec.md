# testcontainers-test-infrastructure Specification

## Purpose

Provide self-contained integration test infrastructure so that tests requiring a real database provision their own PostgreSQL instance via Testcontainers instead of depending on an externally provisioned database. A shared abstract base class encapsulates the container configuration so every current and future integration test inherits it without duplication, and the CI workflow no longer needs to stand up a PostgreSQL service.

## Requirements

### Requirement: Integration tests provision their own PostgreSQL container
Integration tests that require a real database SHALL start a PostgreSQL container via Testcontainers rather than depending on an externally provisioned database. The container SHALL be wired into the Spring datasource using `@ServiceConnection`, so no test declares datasource URL, username, or password properties by hand.

#### Scenario: Integration test runs without external database
- **WHEN** a developer runs `./mvnw test` on a machine with Docker available but no external PostgreSQL running
- **THEN** all integration tests pass because Testcontainers starts its own PostgreSQL container

#### Scenario: Integration test runs in CI without a database service
- **WHEN** the GitHub Actions workflow runs `./mvnw test` without a `services: postgres` block
- **THEN** all integration tests pass because Testcontainers uses the runner's Docker daemon to start a PostgreSQL container

#### Scenario: Datasource properties are not registered manually
- **WHEN** the shared integration test base class is inspected
- **THEN** the container is bound to the datasource by `@ServiceConnection` and no `@DynamicPropertySource` registration of `spring.datasource.*` is present

### Requirement: Shared base class encapsulates container configuration
A shared abstract base class SHALL declare the `PostgreSQLContainer` bean so that all current and future integration test classes can inherit the Testcontainers setup without duplicating configuration. The Testcontainers modules SHALL be declared using the artifact coordinates of the major version in use, which the Spring Boot BOM manages.

#### Scenario: New integration test class reuses container setup
- **WHEN** a developer creates a new `@SpringBootTest` integration test class that extends the base class
- **THEN** the class automatically gets a Testcontainers-managed PostgreSQL datasource with no additional configuration

#### Scenario: Module coordinates resolve under the managed major version
- **WHEN** the Testcontainers major version managed by the Spring Boot BOM changes
- **THEN** the declared module artifactIds are the ones published for that major version and resolve without an explicit version override

### Requirement: CI workflow does not provision an external PostgreSQL service
The GitHub Actions workflow for running tests SHALL NOT include a `services: postgres` block or `SPRING_DATASOURCE_*` environment variable overrides, as the datasource is fully managed by Testcontainers.

#### Scenario: Workflow runs cleanly without postgres service
- **WHEN** a pull request is opened against `main`
- **THEN** the CI workflow runs `./mvnw test` successfully using only the `SPRING_PROFILES_ACTIVE`, `JWT_PRIVATE_KEY`, and `JWT_PUBLIC_KEY` environment variables
