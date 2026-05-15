## 1. Create Shared Test Infrastructure

- [x] 1.1 Create `AbstractIntegrationTest` abstract base class in `src/test/java/com/example/tournaments_backend/` with a `@Bean @ServiceConnection` `PostgreSQLContainer` field and `@ActiveProfiles("test")` annotation
- [x] 1.2 Verify the container uses the `postgres:16-alpine` image to match the existing CI and local setup

## 2. Update AuthServiceIntegrationTests

- [x] 2.1 Update `AuthServiceIntegrationTests` to extend `AbstractIntegrationTest`
- [x] 2.2 Remove `@ActiveProfiles("test")` from `AuthServiceIntegrationTests` since it will be inherited from the base class

## 3. Update Test Configuration

- [x] 3.1 Remove the `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` lines from `src/main/resources/application-test.properties` (replaced by `@ServiceConnection` dynamic values)

## 4. Update CI Workflow

- [x] 4.1 Remove the `services: postgres` block from `.github/workflows/maven-tests.yml`
- [x] 4.2 Remove the `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` env vars from the `Build and Run Tests` step in `maven-tests.yml`

## 5. Verify

- [x] 5.1 Run `./mvnw test` locally and confirm all tests in `AuthServiceIntegrationTests` pass
- [x] 5.2 Confirm `TournamentsBackendApplicationTests` still passes (check if it needs to extend `AbstractIntegrationTest` too)
