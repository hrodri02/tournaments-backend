## Context

`TeamService` (7 public methods) depends on four collaborators: `TeamRepository`, `AppUserService`, `PlayerService`, and `TeamInviteService`. The most complex method is `getTeams`, which builds two lists of `TeamDTO` via the private `getTeamDTOsWithInvites` helper — it fetches teams the user belongs to, fetches pending invites, groups them by team ID, and assembles DTOs with invitee lists.

Unit tests mock all four collaborators (no Spring context, no DB). Integration tests extend `AbstractIntegrationTest` (Testcontainers PostgreSQL, full Spring context, `@Transactional` rollback per test), saving real entities via repositories to set up state before calling the service.

Both test files live under `src/test/java/com/example/tournaments_backend/team/`, mirroring the layout used by `auth/` and `league/`.

## Goals / Non-Goals

**Goals:**
- Cover every public method with at least one happy-path and one error-path test.
- Cover `getTeamDTOsWithInvites` indirectly through `getTeams` unit tests.
- Integration tests verify actual DB persistence / relationship cleanup.

**Non-Goals:**
- Testing `TeamController` (controller-layer tests are out of scope).
- Testing `TeamInviteService` or `PlayerService` methods directly.

## Decisions

### Decision 1: Unit test structure mirrors `LeagueServiceTests`

Use `@ExtendWith(MockitoExtension.class)`, `@Mock` for all four dependencies, and `@InjectMocks` for `TeamService`. Follow the Arrange / Act / Assert comment structure already present in the project.

### Decision 2: `getTeams` unit test uses a mock `Authentication`

`getTeams` accepts a `org.springframework.security.core.Authentication` parameter. In unit tests, pass a `UsernamePasswordAuthenticationToken` (or a plain Mockito mock of `Authentication`) so `authentication.getName()` returns a known email. This avoids any Spring Security context setup.

### Decision 3: Integration tests create a real `Player` as team owner

`Team` has a non-nullable `owner` FK. Every integration test that saves a `Team` must first persist a `Player` via `PlayerRepository`. A helper method `buildPlayer(String email)` in the test class creates a minimal `Player` to reduce duplication.

### Decision 4: `addTeam` integration test passes a `UsernamePasswordAuthenticationToken`

`addTeam` calls `playerService.getPlayerByEmail(authentication.getName())`. In the integration test, save a real `Player`, then pass `new UsernamePasswordAuthenticationToken(player.getEmail(), null)` as the `Authentication` argument — no mocking needed.

### Decision 5: `getTeams` integration test is included

Although `getTeams` is complex, it can be tested in integration by saving a `Player`, a `Team` (with the player as owner and member), and a `TeamInvite` (PENDING), then calling `getTeams` with a matching `Authentication`. The response is asserted for the correct `teamsPartOf` and `teamsInvitedTo` sizes.

### Decision 6: Error paths reuse the same `-1L` id convention as `LeagueServiceIntegrationTests`

Passing an id of `-1L` is guaranteed to not match any auto-generated sequence value, keeping error-path tests simple and readable.

## Risks / Trade-offs

- **`getTeams` integration test complexity**: the method touches four collaborators and builds nested DTOs. If the test becomes brittle, it can be simplified to assert only on sizes rather than deep equality.
- **Sequence state between integration tests**: `@Transactional` rollback resets data but not sequences. IDs grow monotonically within a test run; tests must not hard-code expected IDs.
