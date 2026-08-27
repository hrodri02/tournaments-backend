## Why

nginx currently proxies to a single backend container with no load-balancing policy (a bare `proxy_pass http://backend:8080`). We want to run more than one backend instance and distribute traffic across them using **consistent hashing** rather than the default behavior — both to build out horizontal-scaling capability and to demonstrate/verify the consistent-hash algorithm locally.

## What Changes

- Introduce an nginx `upstream` pool that uses `hash $request_uri consistent` (ketama consistent hashing) across **two** backend containers instead of the single implicit upstream.
- Replace the single `backend` compose service with two services (`backend-1`, `backend-2`), each carrying a distinct `INSTANCE_ID`; nginx `depends_on` both.
- Add an `X-Instance-Id` response header, set by each backend from its `INSTANCE_ID` env var via a Spring `OncePerRequestFilter`, so the serving container is observable and assertable in tests.
- Add a demo/verification script (`devops/local/nginx/test-consistent-hash.sh`) that shows per-path stickiness, spread across both containers, and graceful failover when one backend is stopped.
- Preserve the existing `X-Forwarded-For = $remote_addr` overwrite in nginx verbatim (rate-limiter security is orthogonal and must not change).
- Bound per-service memory (`mem_limit`) across the compose stack and set JVM `-XX:MaxRAMPercentage=75.0` so container limits are respected, keeping the two-backend stack's footprint predictable.
- Move database schema and seed-data ownership out of the serving instances into a one-shot `db-init` compose service, which runs the same image with a `seed` profile and no web server, then exits. The backends wait on `service_completed_successfully` and run `ddl-auto=validate`, so they never issue DDL — previously `create-drop` meant whichever instance booted second dropped and recreated every table underneath the first. `AppUserConfig` becomes `@Profile("seed")` and its `INSTANCE_ID`-based seeder gate is deleted.

## Capabilities

### New Capabilities
- `load-balancing`: How nginx distributes requests across multiple backend instances (consistent-hash key, upstream pool membership, failover behavior) and how the serving instance is identified via the `X-Instance-Id` response header.

### Modified Capabilities
<!-- None. Rate-limiting requirements are unchanged; the X-Forwarded-For overwrite is preserved verbatim. -->

## Impact

- **nginx config**: `devops/local/nginx/default.conf` — add `upstream` block, point `location /` at the pool.
- **Compose**: `devops/local/docker-compose.yml` — `backend` → `backend-1` + `backend-2` (YAML anchor to avoid duplication), each with `INSTANCE_ID`; nginx `depends_on` both.
- **Backend code**: new `OncePerRequestFilter` (e.g. under a small `web`/`infra` package) reading `INSTANCE_ID` (env, with default fallback so the app still boots outside compose) and stamping `X-Instance-Id` on every response.
- **Tests**: new JUnit `MockMvc` test asserting the filter sets `X-Instance-Id` from the configured value.
- **Scripts**: new `devops/local/nginx/test-consistent-hash.sh`.
- **Docs**: new `docs/consistent-hashing.md`.
- **Dockerfile**: JVM entrypoint now passes `-XX:MaxRAMPercentage=75.0`.
- **Compose**: `mem_limit` added to `postgres`, `maildev`, `redis`, `backend-1`, `backend-2`, and `nginx`.
- **Backend code**: `AppUserConfig` is annotated `@Profile("seed")`; the `SEEDER_INSTANCE_ID` gate and its `INSTANCE_ID` parameter are removed, and the `count() > 0` guard is retained so a re-run cannot duplicate data.
- **Compose**: new one-shot `db-init` service (`seed` profile, `ddl-auto=update`, `web-application-type=none`, `restart: "no"`); new `x-app-image` anchor so all three app services share one build; `ddl-auto=validate` added to `x-backend-env`; backends `depends_on` `db-init` with `condition: service_completed_successfully`; Postgres healthcheck now interpolates `${POSTGRES_USER}`/`${POSTGRES_DB}` instead of hardcoding them, since three services gate on it.
- **Docs**: `devops/local/README.md` refreshed (it still listed only Postgres and MailDev) with the full service list, the `db-init` lifecycle, and the `-Dspring-boot.run.profiles=seed` flag; new ADR `docs/adr/2026-07-25-one-shot-seeder-container-owns-schema.md`.
- No changes to application data model, auth, or the rate limiter. Both backends remain functionally interchangeable (stateless JWT validated locally; refresh tokens in Postgres; rate-limit buckets in Redis).
