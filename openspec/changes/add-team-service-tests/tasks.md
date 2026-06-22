## 1. Create `TeamServiceTests.java` (unit tests)

- [x] 1.1 Create `src/test/java/com/example/tournaments_backend/team/TeamServiceTests.java` with `@ExtendWith(MockitoExtension.class)`, `@Mock` fields for `TeamRepository`, `AppUserService`, `PlayerService`, `TeamInviteService`, and `@InjectMocks TeamService`

- [x] 1.2 Add `getTeamById_ShouldReturnTeam_WhenTeamExists`: stub `teamRepository.findById(1L)` to return an `Optional` of a team; assert the result equals the stubbed team

- [x] 1.3 Add `getTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist`: stub `teamRepository.findById(1L)` to return `Optional.empty()`; assert `ServiceException` with `HttpStatus.NOT_FOUND` and `ClientErrorKey.TEAM_NOT_FOUND`

- [x] 1.4 Add `addTeam_ShouldSaveTeamAndReturnDTO_WhenRequestIsValid`: stub `playerService.getPlayerByEmail`, `teamRepository.save`, and `playerService.getAllPlayersByEmail` (empty list) and `teamInviteService.addAll` (empty list); assert returned `TeamDTO` name matches request

- [x] 1.5 Add `deleteTeamById_ShouldReturnDTO_WhenTeamExists`: stub `teamRepository.findById(1L)` with a team that has empty leagues and players sets; assert result DTO id and name; verify `teamRepository.deleteById(1L)` was called

- [x] 1.6 Add `deleteTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist`: stub `findById` to empty; assert `ServiceException` NOT_FOUND + TEAM_NOT_FOUND; verify `deleteById` never called

- [x] 1.7 Add `updateTeam_ShouldUpdateAndReturnTeam_WhenTeamExists`: stub `teamRepository.findById(1L)` with an existing team; stub `teamRepository.save` to return the updated team; assert name and logoUrl updated

- [x] 1.8 Add `updateTeam_ShouldThrowServiceException_WhenTeamDoesNotExist`: stub `findById` to empty; assert `ServiceException` NOT_FOUND + TEAM_NOT_FOUND

- [x] 1.9 Add `addPlayerToTeam_ShouldAddPlayerAndReturnDTO_WhenBothExist`: stub `playerService.getPlayerById` and `teamRepository.findById`; stub `teamRepository.save` to return the team; assert player is present in returned DTO

- [x] 1.10 Add `addPlayerToTeam_ShouldThrowServiceException_WhenTeamDoesNotExist`: stub `playerService.getPlayerById` ok; stub `teamRepository.findById` to empty; assert `ServiceException` NOT_FOUND + TEAM_NOT_FOUND

- [x] 1.11 Add `deletePlayerFromTeam_ShouldRemovePlayerAndReturnDTO_WhenBothExist`: stub player and team; call service; assert player no longer in DTO

- [x] 1.12 Add `deletePlayerFromTeam_ShouldThrowServiceException_WhenTeamDoesNotExist`: stub `playerService.getPlayerById` ok; stub `teamRepository.findById` to empty; assert `ServiceException` NOT_FOUND + TEAM_NOT_FOUND

- [x] 1.13 Add `getTeams_ShouldReturnTeamsPartOfAndInvitedTo_WhenUserHasTeamsAndInvites`: stub `appUserService.getAppUserByEmail`, `teamRepository.findByPlayers_Id` (one team), `teamInviteService.getAllInvitesByPlayerId` (one invite), `teamRepository.findAllById` (one invited team), `teamInviteService.getAllTeamInvites` (empty for both calls to simplify), `playerService.getAllPlayersByIds` (empty); assert `teamsPartOf` size 1 and `teamsInvitedTo` size 1

## 2. Create `TeamServiceIntegrationTests.java` (integration tests)

- [x] 2.1 Create `src/test/java/com/example/tournaments_backend/team/TeamServiceIntegrationTests.java` extending `AbstractIntegrationTest`, annotated `@Transactional`; autowire `TeamService`, `TeamRepository`, `PlayerRepository`, and `TeamInviteRepository`

- [x] 2.2 Add private helper `buildPlayer(String email)` that constructs and saves a minimal `Player` (first name, last name, email, encoded password, `AppUserRole.PLAYER`, `Position.STRIKER`) via `playerRepository.save`

- [x] 2.3 Add `getTeamById_ShouldReturnTeam_WhenTeamExists`: use helper to save owner + team; call `teamService.getTeamById(saved.getId())`; assert result equals saved team

- [x] 2.4 Add `getTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist`: assert `ServiceException` with NOT_FOUND and TEAM_NOT_FOUND when id is `-1L`

- [x] 2.5 Add `addTeam_ShouldPersistTeamWithOwner_WhenRequestIsValid`: save a Player; build a `TeamRequest` with name, `createdAt`, and empty `playersToInvite`; call `teamService.addTeam(request, new UsernamePasswordAuthenticationToken(player.getEmail(), null))`; assert returned DTO name matches; assert `teamRepository.findById(dto.getId())` is present

- [x] 2.6 Add `deleteTeamById_ShouldRemoveTeamFromDb_WhenTeamExists`: save owner + team; call `teamService.deleteTeamById(saved.getId())`; assert `teamRepository.findById(saved.getId())` is empty

- [x] 2.7 Add `deleteTeamById_ShouldThrowServiceException_WhenTeamDoesNotExist`: assert `ServiceException` NOT_FOUND when id is `-1L`

- [x] 2.8 Add `updateTeam_ShouldPersistUpdatedFields_WhenTeamExists`: save owner + team; build update `TeamRequest`; call `teamService.updateTeam(saved.getId(), request)`; re-fetch from repo; assert name and logoUrl match updated values

- [x] 2.9 Add `updateTeam_ShouldThrowServiceException_WhenTeamDoesNotExist`: assert `ServiceException` NOT_FOUND when id is `-1L`

- [x] 2.10 Add `addPlayerToTeam_ShouldPopulateJoinTable_WhenBothExist`: save owner + team + second player; call `teamService.addPlayerToTeam(secondPlayer.getId(), team.getId())`; assert returned DTO players contain the second player's id

- [x] 2.11 Add `deletePlayerFromTeam_ShouldRemoveFromJoinTable_WhenBothExist`: save owner + team, add owner to team players; call `teamService.deletePlayerFromTeam(owner.getId(), team.getId())`; assert returned DTO players do not contain the owner

## 3. Verify

- [x] 3.1 Run `./mvnw test -Dtest=TeamServiceTests` and confirm all unit tests pass

- [x] 3.2 Run `./mvnw test -Dtest=TeamServiceIntegrationTests` and confirm all integration tests pass

- [x] 3.3 Run `./mvnw test` to confirm the full suite still passes with no regressions
