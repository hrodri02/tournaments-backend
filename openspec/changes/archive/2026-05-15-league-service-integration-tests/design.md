## Contexto

`LeagueService` opera sobre PostgreSQL a través de `LeagueRepository` (que contiene dos consultas SQL nativas) y `TeamService`. El proyecto ya cuenta con una clase base `AbstractIntegrationTest` que configura un contenedor PostgreSQL 16 gestionado por Testcontainers via `@ServiceConnection`, y `AuthServiceIntegrationTests` demuestra el patrón a seguir.

`addTeamToLeague` es el único método que accede a un segundo agregado (`Team`), que a su vez requiere un `Player` como propietario. Las pruebas de este método deben persistir un fixture mínimo de `Player` + `Team` antes de invocar el servicio.

## Objetivos / No objetivos

**Objetivos:**
- Proveer cobertura a nivel de integración para cada método público de `LeagueService`.
- Validar las dos consultas SQL nativas (`findInProgressLeagues`, `findEndedLeagues`) contra aritmética de fechas real en PostgreSQL.
- Verificar que la tabla de unión many-to-many `league_team` se puebla correctamente al llamar `addTeamToLeague`.
- Mantener cada prueba aislada mediante rollback con `@Transactional`.

**No objetivos:**
- Reemplazar las pruebas unitarias existentes — siguen siendo valiosas para retroalimentación rápida.
- Probar la capa HTTP de `LeagueController`.
- Probar cascadas hacia `Game` o `GameStat`.

## Decisiones

### Decisión 1: Extender `AbstractIntegrationTest` y anotar la clase con `@Transactional`

**Elegida**: `LeagueServiceIntegrationTests extends AbstractIntegrationTest` + `@Transactional` a nivel de clase, siguiendo el mismo patrón de `AuthServiceIntegrationTests`.

**Por qué**: Cada prueba hace rollback automáticamente, dejando la BD limpia para la siguiente sin necesidad de `@AfterEach` manual. `AbstractIntegrationTest` ya gestiona el ciclo de vida del contenedor.

### Decisión 2: Usar `@Autowired LeagueRepository` y `@Autowired TeamRepository` para la preparación de pruebas, no la capa de servicio

**Elegida**: Inyectar repositorios directamente para configurar y verificar el estado de la BD; invocar el servicio bajo prueba solo en la fase de acción.

**Por qué**: Los métodos de servicio pueden agregar lógica adicional (p. ej. lanzar excepciones) que no debe formar parte del paso de Arrange. Los saves directos al repositorio son más simples y explícitos.

### Decisión 3: Fixture para `addTeamToLeague` — `Player` + `Team` mínimos guardados via repositorios

**Elegida**: Guardar un `Player` (subclase de `AppUser`) y un `Team` con ese player como owner directamente a través de `PlayerRepository` y `TeamRepository` antes de llamar `addTeamToLeague`.

**Por qué**: `Team` tiene una FK `owner` no nula. Es el fixture más simple que satisface la restricción sin involucrar `AuthService` ni `TeamService`.

**Alternativa considerada**: Usar `TeamService.createTeam(...)` — descartada porque involucra lógica adicional (invitaciones, autenticación) irrelevante para el fixture.

### Decisión 4: Las pruebas de consultas nativas usan offsets de fecha relativos a `LocalDate.now()`

**Elegida**: Crear ligas con `startDate = LocalDate.now().minusWeeks(2)` y `durationInWeeks = 8` para IN_PROGRESS; `startDate = LocalDate.now().plusWeeks(1)` para NOT_STARTED; `startDate = LocalDate.now().minusWeeks(10)` y `durationInWeeks = 4` para ENDED.

**Por qué**: Las consultas comparan `start_date` y `start_date + duration_in_weeks * interval` contra la fecha actual. Usar offsets relativos a `LocalDate.now()` garantiza que las pruebas no queden ligadas a una fecha de calendario específica.

## Riesgos / Compromisos

- **Tiempo de arranque de pruebas**: cada clase de prueba que carga un nuevo contexto Spring levanta un nuevo contenedor Testcontainers. Es aceptable dado que hay pocas clases de pruebas de integración.
- **`@Transactional` y carga perezosa**: `getLeagues()` usa un `@EntityGraph`, por lo que no se esperan excepciones de lazy-load. Si surgieran, la solución es invocar el método dentro de la misma transacción, lo que `@Transactional` en la clase de prueba ya garantiza.
- **Sincronización bidireccional en `addTeamToLeague`**: `League.addTeam(team)` llama a `team.getLeagues().add(this)`. Dentro de la misma transacción el estado en memoria se actualiza; la tabla de unión se escribe en la BD al hacer commit (o flush explícito). La prueba verifica esto mediante una re-consulta con `findById` después de llamar al servicio.

## Plan de migración

1. Crear `LeagueServiceIntegrationTests.java` extendiendo `AbstractIntegrationTest`.
2. Inyectar `LeagueService`, `LeagueRepository`, `TeamRepository` y `PlayerRepository`.
3. Implementar los métodos de prueba en el orden de los métodos del servicio: `addLeague`, `getLeagueById`, `getLeagues`, `getLeagues(status)`, `addTeamToLeague`, `deleteLeagueById`, `updateLeague`.
4. Ejecutar `./mvnw test -Dtest=LeagueServiceIntegrationTests` localmente y confirmar que todas las pruebas pasan.

## Preguntas abiertas

- Ninguna — el patrón de `AbstractIntegrationTest` está establecido y la API del servicio es estable.
