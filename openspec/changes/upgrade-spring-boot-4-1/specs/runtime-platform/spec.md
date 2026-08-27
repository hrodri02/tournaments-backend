## MODIFIED Requirements

### Requirement: Spring Boot baseline

The application SHALL build and run against a Spring Boot 4.1.x release, declared as the `spring-boot-starter-parent` version in `pom.xml`. The declared patch version SHALL be one that officially supports the JDK used by the container image, so the deployed runtime is never on an unsupported JVM.

#### Scenario: Application starts on the declared baseline
- **WHEN** the application is built and started against the declared Spring Boot parent version
- **THEN** the Spring context initializes successfully and the web server accepts requests

#### Scenario: Container JDK is supported by the declared baseline
- **WHEN** the `Dockerfile` builds and runs the application on a given JDK version
- **THEN** the declared Spring Boot patch version is one that officially supports that JDK

#### Scenario: Java language level is unaffected by the major upgrade
- **WHEN** the Spring Boot baseline moves within the 4.x line
- **THEN** the declared `java.version` is unchanged, because the Boot 4 baseline is Java 17

### Requirement: OpenAPI tooling tracks the Spring Boot baseline

The `springdoc-openapi` version SHALL be one built against the declared Spring Boot baseline — the 3.1.x line for Boot 4.1.x. Because springdoc binds to Spring MVC internals, a mismatch fails at application startup rather than at compile time, so a passing test suite is not sufficient evidence of compatibility.

#### Scenario: Swagger UI serves after a Spring Boot upgrade
- **WHEN** the Spring Boot parent version is changed and the application is started
- **THEN** `/swagger-ui.html` renders and lists the API operations
- **AND** `/v3/api-docs` returns an OpenAPI document without authentication

## ADDED Requirements

### Requirement: Driver-level contracts are re-verified across a driver major version

Where application code depends on a client library's internals — an exception type referenced by name, or the reply shape of a raw protocol command — a change to that library's major version SHALL be verified behaviorally against the capability the code implements, not only by a successful compile and a passing happy-path suite.

#### Scenario: Fail-closed stance survives a Redis driver major upgrade
- **WHEN** the Redis client library's major version changes and the Redis store is then made unreachable
- **THEN** requests receive HTTP `503` with `errorKey` `RATE_LIMIT_UNAVAILABLE`, as the rate-limiting capability requires
- **AND** the limiter does not allow requests through under infrastructure failure

#### Scenario: Raw command reply still deserializes
- **WHEN** the Redis client library's major version changes and the sliding-window Lua script is executed
- **THEN** the reply's positional values are read without a cast failure and the limiter returns a correct decision
