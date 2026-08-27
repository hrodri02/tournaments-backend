## Why

`TeamService` has no test coverage. All other services in the project (`AuthService`, `LeagueService`) have both unit and integration tests. Adding tests for `TeamService` closes this gap, catches regressions, and brings the team domain in line with the project's testing standards.

## What Changes

- Create `src/test/java/com/example/tournaments_backend/team/TeamServiceTests.java` — Mockito-based unit tests covering all 7 public methods and the private `getTeamDTOsWithInvites` helper (exercised via `getTeams`).
- Create `src/test/java/com/example/tournaments_backend/team/TeamServiceIntegrationTests.java` — Testcontainers-backed integration tests verifying persistence behavior for the methods that write to or read from the database.

## Capabilities

### New Capabilities

- `team-service-unit-tests`: Full Mockito-based unit test coverage for `TeamService`, following the same pattern as `LeagueServiceTests`.
- `team-service-integration-tests`: Testcontainers integration tests for `TeamService`, following the same pattern as `LeagueServiceIntegrationTests` and reusing `AbstractIntegrationTest`.

### Modified Capabilities

_None — no application code is changed._

## Impact

- **New test files only**: `TeamServiceTests.java` and `TeamServiceIntegrationTests.java` in a new `team/` package under `src/test/`.
- **No application code changes.**
- **No new dependencies**: Mockito, JUnit 5, AssertJ, Testcontainers, and `spring-boot-testcontainers` are already declared in `pom.xml`.
