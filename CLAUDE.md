# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (PostgreSQL + MailDev) — required before running the app
docker compose -f devops/local/docker-compose.yml up -d

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AuthServiceTests

# Run a single test method
./mvnw test -Dtest=AuthServiceTests#signUp_ShouldCreatePlayer_WhenRoleIsPlayer

# Build
./mvnw clean package
```

## Architecture

Spring Boot 3.2.4 REST API (Java 23, Maven, PostgreSQL 16) with JWT-based authentication.

**Layered pattern:** Controller → Service → Repository, organized by domain under `src/main/java/com/example/tournaments_backend/`.

**Domain packages:** `app_user`, `auth`, `game`, `game_stat`, `league`, `league_application`, `player`, `team`, `team_invite`, plus `security` and `email`.

### Auth & Security

- RSA key pair (`app.pub` / `app.key` in classpath) signs JWT access tokens via Spring OAuth2 resource server.
- `JwtService` issues short-lived access tokens and long-lived refresh tokens (stored in DB, revocable).
- `AuthService` handles signup, login, email confirmation (via MailDev SMTP on port 1025), and password reset — each flow uses its own token entity (`ConfirmationToken`, `ResetToken`).

### Data Model

```
AppUser (base)  ←  Player (extends, adds position + teams)
Team  ←→  Player (many-to-many)
Team  ←→  League (many-to-many)
League  →  Game (one-to-many)
Game  →  GameStat (one-to-many, tracks GOAL/ASSIST/FAILURE etc.)
TeamInvite: Team → Player invitation workflow (PENDING → ACCEPTED/REJECTED)
Application: Team → League membership workflow (PENDING → APPROVED/REJECTED)
```

### Key Conventions

- `ddl-auto=create-drop` — the DB schema is recreated on every startup; no migration tool.
- Lombok is used heavily; ensure annotation processing is enabled in your IDE.
- `ServiceException` (wraps an HTTP status + error key) is the standard way to signal domain errors; `GlobalExceptionHandler` converts them to structured `ErrorDetails` responses.
- Builder pattern via `@Builder` (and dedicated `*Builder` classes for complex entities like `Team`) is preferred over setters in service code.
- Static factory methods like `TeamDTO.from(Collection<Team>)` are used for mapping to DTOs.

## Local Services

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MailDev (email UI) | http://localhost:1080 |
