## ADDED Requirements

### Requirement: Integration tests provision their own PostgreSQL container
Integration tests that require a real database SHALL start a PostgreSQL container via Testcontainers rather than depending on an externally provisioned database. The container SHALL be wired into the Spring datasource automatically using `@ServiceConnection`.

#### Scenario: Integration test runs without external database
- **WHEN** a developer runs `./mvnw test` on a machine with Docker available but no external PostgreSQL running
- **THEN** all integration tests pass because Testcontainers starts its own PostgreSQL container

#### Scenario: Integration test runs in CI without a database service
- **WHEN** the GitHub Actions workflow runs `./mvnw test` without a `services: postgres` block
- **THEN** all integration tests pass because Testcontainers uses the runner's Docker daemon to start a PostgreSQL container

### Requirement: Shared base class encapsulates container configuration
A shared abstract base class SHALL declare the `PostgreSQLContainer` bean so that all current and future integration test classes can inherit the Testcontainers setup without duplicating configuration.

#### Scenario: New integration test class reuses container setup
- **WHEN** a developer creates a new `@SpringBootTest` integration test class that extends the base class
- **THEN** the class automatically gets a Testcontainers-managed PostgreSQL datasource with no additional configuration

### Requirement: CI workflow does not provision an external PostgreSQL service
The GitHub Actions workflow for running tests SHALL NOT include a `services: postgres` block or `SPRING_DATASOURCE_*` environment variable overrides, as the datasource is fully managed by Testcontainers.

#### Scenario: Workflow runs cleanly without postgres service
- **WHEN** a pull request is opened against `main`
- **THEN** the CI workflow runs `./mvnw test` successfully using only the `SPRING_PROFILES_ACTIVE`, `JWT_PRIVATE_KEY`, and `JWT_PUBLIC_KEY` environment variables
