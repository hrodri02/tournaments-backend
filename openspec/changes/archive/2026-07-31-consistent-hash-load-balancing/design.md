## Context

nginx is the only host-facing entry point (`:80`) and currently proxies to a single backend container via a bare `proxy_pass http://backend:8080` — there is no `upstream` block and therefore no explicit load-balancing policy. Postgres, Redis (rate-limit store), and MailDev are shared infrastructure. The application is stateless: JWT access tokens are RSA-signed and validated locally by any instance using the public key, refresh tokens live in Postgres, and rate-limit buckets live in Redis. Consequently, which backend serves a given request is functionally irrelevant.

We want to run two backend instances and distribute traffic with consistent hashing, and to make the behavior observable/assertable locally.

## Goals / Non-Goals

**Goals:**
- Distribute requests across two backend containers using nginx consistent hashing keyed on `$request_uri`.
- Make the serving instance observable via an `X-Instance-Id` response header driven by an `INSTANCE_ID` env var.
- Provide an easy, single-host way to demonstrate spread across containers, per-path stickiness, and graceful failover.
- Preserve the existing rate-limiter security behavior (`X-Forwarded-For = $remote_addr` overwrite) unchanged.

**Non-Goals:**
- Session affinity / sticky client sessions (not needed — app is stateless).
- Production orchestration, autoscaling, or health-check tuning beyond nginx defaults.
- Any change to the rate limiter, auth, or data model.

## Decisions

**Decision: Hash key = `$request_uri` (not `$remote_addr`).**
The app is stateless, so client affinity buys nothing functional. `$request_uri` varies per request from a single host client, making the algorithm trivially demonstrable with plain `curl` (no multi-client-IP setup). Alternative `$remote_addr` (client affinity) was rejected: it only demonstrates spread with many distinct client IPs and solves a problem (in-memory sessions) we don't have.

**Decision: Two explicit compose services (`backend-1`, `backend-2`) over `--scale`.**
nginx's `hash … consistent` builds its ring from a *static* list of `server` entries in the `upstream` block. Two named services give reliable ring membership and distinct `INSTANCE_ID`s. A single scaled service (`--scale backend=2`) was rejected: DNS round-robin doesn't populate the hash ring cleanly in open-source nginx, and replicas would share one `INSTANCE_ID` (indistinguishable). Duplication between the two services is collapsed with a YAML anchor.

**Decision: `X-Instance-Id` via a Spring `OncePerRequestFilter`.**
A single filter reads `INSTANCE_ID` (env, with a default fallback so the app still boots outside compose) and sets the header on every response. Chosen over per-controller logic (leaky) and nginx `add_header $upstream_addr` (only exposes a container IP, not a friendly name and not asserted server-side).

**Decision: Bound each service's memory via `mem_limit` + JVM `MaxRAMPercentage`.**
Running two backend containers instead of one roughly doubles the stack's local memory footprint. Explicit `mem_limit` on every compose service (450m per backend, smaller values for postgres/redis/maildev/nginx) keeps the total footprint predictable on a dev machine. `-XX:MaxRAMPercentage=75.0` makes each JVM size its heap relative to its container's cgroup limit rather than host memory, so the `mem_limit` is actually respected instead of the JVM over-committing.

**Decision (SUPERSEDED): Gate the seed-data `CommandLineRunner` on a designated `INSTANCE_ID`, not just an existence check.**
With two backend instances starting against the same Postgres database, both instances' `CommandLineRunner` beans run on startup. An initial `appUserRepository.count() > 0` guard was tried first, but it's a check-then-act race: `backend-1` and `backend-2` start concurrently with no ordering between them, so both can observe `count() == 0` before either commits its insert, and both proceed to seed — producing duplicate users (confirmed in practice). The fix restricted seeding to a single designated instance (`SEEDER_INSTANCE_ID = "backend-1"`, plus the `default` `INSTANCE_ID` fallback so plain `./mvnw spring-boot:run` still seeds).

Superseded by the decision below. Gating on instance identity addressed only duplicate *inserts*; it left the more damaging problem untouched, because seeding was never the whole story — schema generation was.

**Decision: A one-shot initializer container owns the schema and the seed data; serving instances only validate.**
`application.properties` sets `spring.jpa.hibernate.ddl-auto=create-drop`, so *every* instance ran schema DDL on startup, not just seeding. The second backend to boot dropped and recreated every table, discarding whatever the first had committed — meaning the `INSTANCE_ID` gate could keep seed data unique while still allowing it to vanish entirely, depending on which instance finished DDL last.

The fix moves both responsibilities out of the serving instances into a dedicated `db-init` compose service. It runs the *same image* as the backends (shared via an `x-app-image` anchor) with `SPRING_PROFILES_ACTIVE=seed`, `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, and `SPRING_MAIN_WEB_APPLICATION_TYPE=none`: it boots Spring, ensures the schema, seeds, and exits 0. The backends declare `depends_on: db-init: condition: service_completed_successfully` and run `ddl-auto=validate`, so they cannot issue DDL at all. `AppUserConfig` becomes `@Profile("seed")` and the `INSTANCE_ID` gate is deleted — there is exactly one seeder by construction, enforced by container lifecycle rather than by an identity comparison in application code.

Reusing the app image was chosen over Flyway migrations or a `psql` container running `.sql` files. Those are the better production answer, but the seed data is ~90 players whose passwords come from `BCryptPasswordEncoder.encode(...)`; expressing it as SQL means hardcoding that many bcrypt hashes and hand-maintaining every join table in parallel with the JPA entities. Reusing the image keeps the entities, builders, and encoder as the single definition of the seed data.

**Decision: The initializer uses `ddl-auto=update`, not `create`.**
Compose restarts a dependency whenever a service depending on it is restarted, so `docker compose restart backend-1` re-runs `db-init`. `create` was implemented first and proved actively harmful: restarting one backend made the initializer issue 26 `DROP TABLE` statements while the *other* backend was still serving traffic, silently invalidating the tokens it had issued. `create-drop` is worse still — it registers a shutdown hook that drops the schema the instant the initializer's JVM exits. `update` creates the schema on an empty database and is a no-op on a populated one; paired with the retained `count() > 0` guard in `AppUserConfig`, a re-run touches nothing. The trade-off is that `update` never drops stale columns, so `docker compose down -v` is the clean-slate path and the backends' `validate` is what catches drift.

## Risks / Trade-offs

- **All traffic from one host client shares one `$remote_addr` but varies `$request_uri`** → distribution is visible only because the key is the path; hitting the *same* path repeatedly always lands on the same backend (this is the intended, demonstrable stickiness), not a bug.
- **Uneven distribution across only 2 backends for a small number of distinct paths** → mitigated by the demo script curling several distinct paths; consistent hashing does not guarantee 50/50 for few keys.
- **One backend down** → nginx default `proxy_next_upstream` routes to the healthy one; *consistent* hashing means only the failed backend's share of paths remaps, not the whole keyspace. Demonstrated explicitly in the script.
- **`INSTANCE_ID` unset outside compose** → filter falls back to a default value so local `./mvnw spring-boot:run` and tests still work.
- **Schema creation still runs through Hibernate**, only relocated to one container rather than being expressed as reviewable, versioned migrations → acceptable for local-only dev infrastructure; adopting Flyway is the natural next step if the schema ever needs versioning.
- **`ddl-auto=update` never drops stale columns**, so an entity change can leave schema drift → `docker compose down -v` resets, and the backends' `validate` surfaces drift at startup instead of letting it pass unnoticed.
- **Seeding no longer happens by default when running from source** → `./mvnw spring-boot:run` yields an empty database; `-Dspring-boot.run.profiles=seed` is required, and this is documented in `devops/local/README.md`.
- **`application.properties` still carries `create-drop` and points at `localhost:5432/tournaments`**, which is the Compose Postgres → running `./mvnw spring-boot:run` from source will drop the running stack's schema. Pre-existing behavior, unchanged by this change, but sharper now that the stack's data is expected to persist across backend restarts.

## Migration Plan

Local-only dev infrastructure; no production rollout. Apply by rebuilding the compose stack (`docker compose -f devops/local/docker-compose.yml up -d --build`). Rollback is reverting the nginx config and compose file to the single-`backend` service; the `X-Instance-Id` filter is inert (harmless default) if the env var is absent.
