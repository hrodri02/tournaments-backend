## Context

See `proposal.md` — Why for the rationale behind 3.5 as a waypoint. No capability spec changes, so this document covers only how the upgrade is carried out.

Two facts the decisions below turn on: `Dockerfile:1,8` builds and runs on Temurin 25 while `pom.xml:30` declares Java 23, and the build carries four hand-pinned dependency versions (`pom.xml:57`, `:78`, `:87`, `:92`) that the Boot BOM would otherwise manage.

## Goals / Non-Goals

**Goals:**
- Reach a green test suite on 3.5.16 with no production source changes.
- Eliminate every deprecation warning that represents a Boot 4 removal, so the next change starts from a clean compile.
- Reduce the number of hand-pinned versions, so the remaining two Boot upgrades have fewer independent variables.

**Non-Goals:**
- Any Java version change. The JDK stays at 23 in `pom.xml` and both CI workflows.
- Adopting `@ServiceConnection` in the Testcontainers base class, or any other modernization that isn't required to compile and pass on 3.5.16.
- Resolving the `application.properties` / Compose `create-drop` hazard noted in the load-balancing change.

## Decisions

**Decision: 3.5.16, not 3.5.0.**
Java 25 support arrived partway through the 3.5 line. `Dockerfile` builds and runs on `eclipse-temurin:25.0.2`, so pinning an early 3.5 patch would put the container on a JVM that release does not officially support — an inconsistency that only shows up in the deployed image, not in local `./mvnw test` on any JDK.

**Decision: springdoc moves in lockstep with Boot, at every step.**
springdoc 2.2.0 targets Boot 3.1 / Framework 6.0 and binds to Spring MVC handler-mapping internals that moved in Framework 6.2. It fails at **application startup**, not at compile time, so the suite can go green and the app still won't boot — the failure mode that makes this worth stating rather than leaving to whoever reads the dependency list. springdoc 2.9.0's own parent is `spring-boot-starter-parent:3.5.16`, so version-matching is exact. Treat this as a standing constraint: 3.0.3 pairs with Boot 4.0, 3.1.0 with Boot 4.1.

**Decision: remove the `spring-security-test` version rather than bump it.**
`pom.xml:54-59` pins 6.2.2 against a BOM-managed runtime of 6.2.3 — the drift already exists and nobody noticed. Bumping the pin to 6.5.11 reproduces the same defect one version later. Deleting the `<version>` element makes the class of bug impossible for the remaining two upgrades.

**Decision: do the `@MockitoBean` swap here, not in the Boot 4 change.**
3.5 is the only release where both APIs exist — the bean-override annotations are Spring Framework 6.2 APIs, so this swap is only possible once the 3.5 parent is in place, not before it. The new annotations carry an `enforceOverride` attribute governing whether an existing bean definition is required — a semantic difference, not a rename. Swapping while the suite can still go green isolates any fallout from the Jackson, Hibernate, and Testcontainers breakage arriving in the next change. `AuthServiceIntegrationTests.java:52,61` are the only two usages in the repository.

**Decision: test the `byte-buddy` pin rather than delete it on principle.**
`pom.xml:89-93` pins 1.18.3 over the BOM's 1.17.8, and surefire passes `-XX:+EnableDynamicAgentLoading -Xshare:off` (`pom.xml:124`). Read together, those say the pin exists so Mockito's inline mock maker works on a modern JDK — a deliberate fix, not drift. The task is to drop it, run the suite, and restore it if `MockMaker` errors appear; the surefire arguments stay either way.

## Risks / Trade-offs

- **Hibernate 6.4.4 → 6.6.53 is the only change here that can fail for a non-obvious reason.** Three minors of tightened mapping validation land on `AppUser` → `Player` inheritance plus two bidirectional many-to-manys (`Team ↔ Player`, `Team ↔ League`). `ddl-auto=create-drop` works in our favour: drift surfaces as a loud startup failure in the integration tests rather than as silent runtime misbehavior.
- **A green suite does not clear Swagger.** Spring Security 6.3+ tightened `requestMatchers` ambiguity handling when more than one servlet is registered, and springdoc registers resources. The `permitAll` rules for `/v3/api-docs/**` and `/swagger-ui/**` (`SecurityConfig.java:76-84`) need a manual browser check — no automated test covers them.
- **Deduplicating Lombok couples the annotation processor to the BOM.** The `annotationProcessorPaths` entry (`pom.xml:153-158`) requires an explicit version, so collapsing the two pins means `${lombok.version}` and a move from 1.18.38 to the managed 1.18.46. Lombok versions interact with the JDK's compiler internals, so this is a real change rather than tidying.
- **Fixing `<source>23.0.2</source>` (`pom.xml:151-152`) is cleanup that could surface a genuine difference.** It is not a valid release number, yet the build compiles today on JDK 25 — meaning something is normalizing it. Replacing it with `java.version` is correct but changes what javac actually receives.

## Migration Plan

Local build change only; no runtime rollout. Verification is `./mvnw test`, then booting the app against the Compose stack and loading Swagger UI. Rollback is reverting `pom.xml` and one test file.

Land this on `main` green before starting `upgrade-spring-boot-4-0`. The value of this change is the known-good baseline; merging it into the Boot 4 work forfeits that.

## Open Questions

- `openspec/specs/testcontainers-test-infrastructure/spec.md` requires the container to be wired with `@ServiceConnection`, but `AbstractIntegrationTest.java:15-24` uses a static block plus `@DynamicPropertySource`. The spec and the code have drifted. Deferred to `upgrade-spring-boot-4-0`, which has to touch that file anyway for the Testcontainers 2.0 migration — but it is a pre-existing inconsistency, not one this upgrade introduces.
