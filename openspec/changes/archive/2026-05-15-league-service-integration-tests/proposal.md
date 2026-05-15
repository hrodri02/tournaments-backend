## Por qué

`LeagueService` ya cuenta con pruebas unitarias completas (`LeagueServiceTests`) que mockean la capa de repositorio. Sin embargo, varios de sus métodos dependen de consultas SQL nativas personalizadas (`findInProgressLeagues`, `findEndedLeagues`) y de una tabla de unión many-to-many (`league_team`) cuya corrección solo puede verificarse contra una base de datos real. Las pruebas de integración detectarán errores en las consultas, problemas de mapeo ORM y comportamiento transaccional que las pruebas con mocks no pueden exponer.

## Qué cambia

- Agregar `LeagueServiceIntegrationTests` en `src/test/java/com/example/tournaments_backend/league/` extendiendo `AbstractIntegrationTest`, usando `@Transactional` para rollback automático por prueba.
- Cubrir todos los métodos públicos de `LeagueService` con al menos una prueba de camino feliz y una de camino de error a nivel de integración, enfatizando los casos donde la BD real aporta valor sobre los mocks:
  - `addLeague` — la entidad se persiste y obtiene un ID generado.
  - `getLeagueById` — camino feliz y excepción `NOT_FOUND` contra una BD real vacía.
  - `getLeagues()` — recupera todas las ligas (incluyendo la carga EntityGraph de equipos).
  - `getLeagues(NOT_STARTED / IN_PROGRESS / ENDED)` — las consultas nativas retornan subconjuntos correctos según las fechas.
  - `addTeamToLeague` — la tabla de unión (`league_team`) se puebla; liga o equipo inexistente lanza `NOT_FOUND`.
  - `deleteLeagueById` — el registro se elimina de la BD; ID inexistente lanza `NOT_FOUND`.
  - `updateLeague` — las mutaciones de campos se persisten; ID inexistente lanza `NOT_FOUND`.

## Capacidades

### Nuevas capacidades

- `league-service-integration-test-suite`: Cobertura de pruebas de integración para `LeagueService` respaldada por una base de datos PostgreSQL real gestionada por Testcontainers.

### Capacidades modificadas

## Impacto

- **Nuevo archivo de pruebas**: `src/test/java/com/example/tournaments_backend/league/LeagueServiceIntegrationTests.java`
- **Sin cambios en el código de la aplicación** — adición exclusiva de pruebas.
- Depende de la clase base `AbstractIntegrationTest` y la infraestructura de Testcontainers ya existentes.
