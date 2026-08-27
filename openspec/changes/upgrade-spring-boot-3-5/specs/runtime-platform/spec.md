## ADDED Requirements

### Requirement: Spring Boot baseline

The application SHALL build and run against a Spring Boot 3.5.x release, declared as the `spring-boot-starter-parent` version in `pom.xml`. The declared patch version SHALL be one that officially supports the JDK used by the container image, so the deployed runtime is never on an unsupported JVM.

#### Scenario: Application starts on the declared baseline
- **WHEN** the application is built and started against the declared Spring Boot parent version
- **THEN** the Spring context initializes successfully and the web server accepts requests

#### Scenario: Container JDK is supported by the declared baseline
- **WHEN** the `Dockerfile` builds and runs the application on a given JDK version
- **THEN** the declared Spring Boot patch version is one that officially supports that JDK

### Requirement: Single source of truth for managed dependency versions

Any dependency whose version is managed by the Spring Boot BOM SHALL NOT declare its own `<version>` in `pom.xml`. The parent version SHALL be the only place a Spring-managed version is expressed, so a framework upgrade cannot leave part of the dependency graph behind.

#### Scenario: A BOM-managed dependency carries no explicit version
- **WHEN** a dependency managed by the Spring Boot BOM is declared in `pom.xml`
- **THEN** it has no `<version>` element and resolves to the version the parent manages

#### Scenario: Test support matches the runtime it exercises
- **WHEN** the Spring Boot parent version is changed
- **THEN** `spring-security-test` and every other BOM-managed test dependency resolve to the versions matching the new runtime, with no remnants of the previous line in the dependency tree

#### Scenario: A deliberate override is justified in place
- **WHEN** a dependency must pin a version above what the BOM manages, because of a JDK or tooling incompatibility
- **THEN** the pin is accompanied by a comment in `pom.xml` recording why it is required

### Requirement: OpenAPI tooling tracks the Spring Boot baseline

The `springdoc-openapi` version SHALL be one built against the declared Spring Boot baseline. Because springdoc binds to Spring MVC internals, a mismatch fails at application startup rather than at compile time, so a passing test suite is not sufficient evidence of compatibility.

#### Scenario: Swagger UI serves after a Spring Boot upgrade
- **WHEN** the Spring Boot parent version is changed and the application is started
- **THEN** `/swagger-ui.html` renders and lists the API operations
- **AND** `/v3/api-docs` returns an OpenAPI document without authentication

### Requirement: Java compilation target is declared once

The Java language level SHALL be expressed solely through the `java.version` property, which drives `maven.compiler.release`. The compiler plugin SHALL NOT declare `source` or `target` independently, so the build cannot claim a language level the parent does not know about.

#### Scenario: Build produces bytecode at the declared level
- **WHEN** the project is compiled on a newer JDK than the declared `java.version`
- **THEN** the produced bytecode targets the declared `java.version` level
