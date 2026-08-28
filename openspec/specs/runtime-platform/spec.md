# runtime-platform Specification

## Purpose

Keep the framework baseline and the build's version governance coherent, so that upgrading Spring Boot is a single deliberate edit rather than a hunt through scattered pins. The Spring Boot parent version is the one place a managed version is expressed; the declared patch release is one that officially supports the JDK the container image runs; tooling that binds to framework internals — notably springdoc, which fails at application startup rather than at compile time — tracks that baseline; the JSON binding layer's package roots are recorded so that a Jackson major bump is known to land on mapper construction rather than on domain annotations; and the Java language level is declared once so the build cannot claim a level the parent does not know about.

## Requirements

### Requirement: Spring Boot baseline

The application SHALL build and run against a Spring Boot 4.0.x release, declared as the `spring-boot-starter-parent` version in `pom.xml`. The declared patch version SHALL be one that officially supports the JDK used by the container image, so the deployed runtime is never on an unsupported JVM.

#### Scenario: Application starts on the declared baseline
- **WHEN** the application is built and started against the declared Spring Boot parent version
- **THEN** the Spring context initializes successfully and the web server accepts requests

#### Scenario: Container JDK is supported by the declared baseline
- **WHEN** the `Dockerfile` builds and runs the application on a given JDK version
- **THEN** the declared Spring Boot patch version is one that officially supports that JDK

#### Scenario: Java language level is unaffected by the major upgrade
- **WHEN** the Spring Boot baseline moves to 4.0.x
- **THEN** the declared `java.version` is unchanged, because the Boot 4 baseline is Java 17

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

The `springdoc-openapi` version SHALL be one built against the declared Spring Boot baseline — the 3.0.x line for Boot 4.0.x. Because springdoc binds to Spring MVC internals, a mismatch fails at application startup rather than at compile time, so a passing test suite is not sufficient evidence of compatibility.

#### Scenario: Swagger UI serves after a Spring Boot upgrade
- **WHEN** the Spring Boot parent version is changed and the application is started
- **THEN** `/swagger-ui.html` renders and lists the API operations
- **AND** `/v3/api-docs` returns an OpenAPI document without authentication

### Requirement: JSON binding library baseline

The application SHALL use the Jackson 3.x binding layer, whose databind and core packages are rooted at `tools.jackson` while the annotation package remains `com.fasterxml.jackson.annotation`. Mapping annotations on domain classes SHALL therefore be unaffected by the Jackson major version, and only code constructing or invoking a mapper SHALL reference the `tools.jackson` root.

#### Scenario: Domain annotations are independent of the Jackson major version
- **WHEN** the Jackson major version changes
- **THEN** `@JsonIgnore`, `@JsonManagedReference`, and `@JsonBackReference` on entity classes remain valid without an import change

#### Scenario: API response format is preserved across the binding upgrade
- **WHEN** an API endpoint is called before and after the Jackson major upgrade with equivalent data
- **THEN** the serialized response bodies are equivalent in field names, nesting, null handling, and date representation

#### Scenario: Mappers are constructed through the builder
- **WHEN** application or test code needs a configured `ObjectMapper`
- **THEN** it is obtained through the Jackson 3 builder API rather than by mutating a constructed mapper

### Requirement: Java compilation target is declared once

The Java language level SHALL be expressed solely through the `java.version` property, which drives `maven.compiler.release`. The compiler plugin SHALL NOT declare `source` or `target` independently, so the build cannot claim a language level the parent does not know about.

#### Scenario: Build produces bytecode at the declared level
- **WHEN** the project is compiled on a newer JDK than the declared `java.version`
- **THEN** the produced bytecode targets the declared `java.version` level
