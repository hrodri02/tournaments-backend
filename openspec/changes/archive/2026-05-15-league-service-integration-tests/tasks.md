## 1. Crear el esqueleto de la clase `LeagueServiceIntegrationTests`

- [x] 1.1 Crear `src/test/java/com/example/tournaments_backend/league/LeagueServiceIntegrationTests.java` extendiendo `AbstractIntegrationTest`, anotada con `@Transactional`
- [x] 1.2 Agregar campos `@Autowired`: `LeagueService leagueService`, `LeagueRepository leagueRepository`, `TeamRepository teamRepository`, `PlayerRepository playerRepository`

## 2. Implementar pruebas de integración para `addLeague`

- [x] 2.1 `addLeague_ShouldPersistLeagueWithGeneratedId_WhenRequestIsValid` — construir un `LeagueRequest`, llamar a `leagueService.addLeague(...)`, verificar que la liga retornada tiene un `id` no nulo y que name/startDate/durationInWeeks coinciden con el request

## 3. Implementar pruebas de integración para `getLeagueById`

- [x] 3.1 `getLeagueById_ShouldReturnLeague_WhenLeagueExists` — guardar una `League` via repositorio, llamar a `leagueService.getLeagueById(id)`, verificar que la liga retornada coincide
- [x] 3.2 `getLeagueById_ShouldThrowServiceException_WhenLeagueDoesNotExist` — llamar a `leagueService.getLeagueById(-1L)` y verificar `ServiceException` con `HttpStatus.NOT_FOUND` y `ClientErrorKey.LEAGUE_NOT_FOUND`

## 4. Implementar prueba de integración para `getLeagues()`

- [x] 4.1 `getLeagues_ShouldReturnAllLeagues_WhenMultipleLeaguesExist` — guardar dos ligas via repositorio, llamar a `leagueService.getLeagues()`, verificar que ambas están presentes en el resultado

## 5. Implementar pruebas de integración para `getLeagues(LeagueStatus)` (consultas nativas)

- [x] 5.1 `getLeagues_ShouldReturnOnlyNotStartedLeagues_WhenStatusIsNotStarted` — guardar una liga NOT_STARTED (`startDate = LocalDate.now().plusWeeks(1)`) y una ENDED, llamar a `leagueService.getLeagues(LeagueStatus.NOT_STARTED)`, verificar que solo se retorna la NOT_STARTED
- [x] 5.2 `getLeagues_ShouldReturnOnlyInProgressLeagues_WhenStatusIsInProgress` — guardar una liga IN_PROGRESS (`startDate = LocalDate.now().minusWeeks(2)`, `durationInWeeks = 8`) y una NOT_STARTED, llamar a `leagueService.getLeagues(LeagueStatus.IN_PROGRESS)`, verificar que solo se retorna la IN_PROGRESS
- [x] 5.3 `getLeagues_ShouldReturnOnlyEndedLeagues_WhenStatusIsEnded` — guardar una liga ENDED (`startDate = LocalDate.now().minusWeeks(10)`, `durationInWeeks = 4`) y una IN_PROGRESS, llamar a `leagueService.getLeagues(LeagueStatus.ENDED)`, verificar que solo se retorna la ENDED

## 6. Implementar pruebas de integración para `addTeamToLeague`

- [x] 6.1 `addTeamToLeague_ShouldPopulateJoinTable_WhenLeagueAndTeamExist` — guardar un `Player` via `playerRepository`, guardar un `Team` (owner = player guardado) via `teamRepository`, guardar una `League` via `leagueRepository`, llamar a `leagueService.addTeamToLeague(leagueId, teamId)`, volver a consultar la liga via repositorio, verificar que `league.getTeams()` contiene al equipo
- [x] 6.2 `addTeamToLeague_ShouldThrowServiceException_WhenLeagueDoesNotExist` — llamar a `leagueService.addTeamToLeague(-1L, -1L)` y verificar `ServiceException` con `HttpStatus.NOT_FOUND` y `ClientErrorKey.LEAGUE_NOT_FOUND`

## 7. Implementar pruebas de integración para `deleteLeagueById`

- [x] 7.1 `deleteLeagueById_ShouldRemoveLeagueFromDb_WhenLeagueExists` — guardar una liga via repositorio, llamar a `leagueService.deleteLeagueById(id)`, verificar que la liga retornada coincide con la guardada y que `leagueRepository.findById(id)` ya no existe
- [x] 7.2 `deleteLeagueById_ShouldThrowServiceException_WhenLeagueDoesNotExist` — llamar a `leagueService.deleteLeagueById(-1L)` y verificar `ServiceException` con `HttpStatus.NOT_FOUND`

## 8. Implementar pruebas de integración para `updateLeague`

- [x] 8.1 `updateLeague_ShouldPersistUpdatedFields_WhenLeagueExists` — guardar una liga via repositorio, construir un `LeagueRequest` actualizado, llamar a `leagueService.updateLeague(id, request)`, volver a consultar con `leagueRepository.findById(id)`, verificar que todos los campos actualizados coinciden con el request
- [x] 8.2 `updateLeague_ShouldThrowServiceException_WhenLeagueDoesNotExist` — llamar a `leagueService.updateLeague(-1L, request)` y verificar `ServiceException` con `HttpStatus.NOT_FOUND`

## 9. Verificar

- [x] 9.1 Ejecutar `./mvnw test -Dtest=LeagueServiceIntegrationTests` y confirmar que todas las pruebas pasan
- [x] 9.2 Ejecutar `./mvnw test` y confirmar que no hay regresiones en las pruebas existentes
