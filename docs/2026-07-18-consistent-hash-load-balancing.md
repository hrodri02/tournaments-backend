# Consistent-Hash Load Balancing

Runs two backend containers behind nginx and distributes requests between
them with consistent hashing (ketama), so the app can scale horizontally and
the load-balancing behavior is directly observable and verifiable locally.

## What it does

- nginx distributes requests across an `upstream backend_pool` of two backend
  containers (`backend-1`, `backend-2`) using `hash $request_uri consistent`.
- Requests for the **same URI path** always land on the **same instance**
  (stickiness); requests for **different paths** spread across the pool.
- Every response carries an `X-Instance-Id` header identifying which
  container served it, driven by that container's `INSTANCE_ID` env var
  (falling back to a default outside the multi-container setup).
- If one instance goes down, nginx automatically retries the remaining
  healthy instance; only the paths that hashed to the failed instance remap —
  the rest of the keyspace is unaffected.
- The existing rate-limiter security behavior (`X-Forwarded-For` overwritten
  with `$remote_addr`) is unchanged.
- A one-shot `db-init` container owns the database schema and the mock seed
  data. It runs before either backend starts and exits; the backends run
  `ddl-auto=validate`, so neither can modify the schema underneath the other.

## How a request flows

```
client ──► nginx (upstream backend_pool, hash $request_uri consistent)
              │
              ├──► backend-1 (INSTANCE_ID=backend-1)
              └──► backend-2 (INSTANCE_ID=backend-2)

response ◄── X-Instance-Id: <backend-1|backend-2>
```

## How the stack starts

```
postgres (healthy) ─┐
redis    (healthy) ─┴──► db-init ──► exits 0 ──► backend-1 ──► nginx
                         seed profile,          backend-2
                         ddl-auto=update        ddl-auto=validate
```

The backends declare `depends_on: db-init: condition:
service_completed_successfully`, so they never observe a half-built schema and
never race each other to create one.

## Key design decisions

- **Hash key is `$request_uri`, not `$remote_addr`.** The app is stateless
  (JWT validated locally, refresh tokens in Postgres, rate limits in Redis),
  so client affinity buys nothing functional — and hashing on the path makes
  the spread trivially demonstrable from a single client with `curl`.
- **Two explicit compose services instead of `--scale`.** nginx's
  `hash ... consistent` builds its ring from a static `server` list; a scaled
  service doesn't populate the ring cleanly and all replicas would share one
  `INSTANCE_ID`. `backend-1`/`backend-2` are collapsed with YAML anchors
  (`x-backend-common`, `x-backend-env`) to avoid duplicating config.
- **`X-Instance-Id` via a dedicated `OncePerRequestFilter`.** Registered in
  the Spring Security chain *before* the rate limiter, so the header is
  present on every response — including rate-limited (`429`) and
  auth-rejected ones.
- **A one-shot container owns the schema and seed data.** Two instances
  sharing one database meant two instances running `ddl-auto=create-drop`:
  whichever booted second dropped and recreated every table underneath the
  first. Gating the seeder on an `INSTANCE_ID` was tried first and only
  addressed duplicate *inserts*, not the DDL. `db-init` runs the same image
  with `SPRING_PROFILES_ACTIVE=seed`, `ddl-auto=update`, and no web server;
  `AppUserConfig` is `@Profile("seed")`. Exactly one process seeds, enforced
  by container lifecycle rather than by an identity check in app code.
- **`db-init` uses `ddl-auto=update`, not `create`.** Compose re-runs a
  dependency when a service depending on it restarts, so
  `docker compose restart backend-1` re-runs the seeder. `update` plus the
  retained `count() > 0` guard makes that a no-op; `create` instead issued 26
  `DROP TABLE` statements while the other backend was serving traffic.

See `docs/adr/` for the full rationale and rejected alternatives behind each
decision.

## Components touched

- `src/main/java/com/example/tournaments_backend/instance/InstanceIdFilter.java`
  — sets the `X-Instance-Id` response header.
- `src/main/java/com/example/tournaments_backend/instance/InstanceIdConfig.java`
  — reads `INSTANCE_ID` and wires the filter as a bean.
- `src/main/java/com/example/tournaments_backend/security/config/SecurityConfig.java`
  — registers the filter ahead of the rate limiter.
- `src/main/java/com/example/tournaments_backend/app_user/AppUserConfig.java`
  — seeding runner, now `@Profile("seed")` so only `db-init` runs it.
- `devops/local/nginx/default.conf` — `upstream backend_pool` block.
- `devops/local/docker-compose.yml` — `backend-1`/`backend-2` services, the
  one-shot `db-init` service, the shared `x-app-image` anchor, per-service
  `mem_limit`, and a Postgres healthcheck that follows `.env`.
- `Dockerfile` — JVM entrypoint passes `-XX:MaxRAMPercentage=75.0` so each
  heap is sized from the container limit, not host memory.
- `devops/local/nginx/test-consistent-hash.sh` — verification script.

## How to verify

```bash
docker compose -f devops/local/docker-compose.yml up -d --build
devops/local/nginx/test-consistent-hash.sh
```

The script checks three things against `http://localhost`:

1. Requests to several distinct paths spread across more than one instance.
2. Repeated requests to the same path always return the same
   `X-Instance-Id`.
3. Stopping one backend container (`docker compose stop backend-1`) still
   serves `200`s via the remaining instance, then restarts it.

To check schema/seed ownership:

```bash
# db-init ran to completion, created the schema, and seeded once
docker compose -f devops/local/docker-compose.yml logs db-init
docker compose -f devops/local/docker-compose.yml ps -a db-init   # Exited (0)

# neither serving instance issued DDL
docker compose -f devops/local/docker-compose.yml logs backend-1 backend-2 \
  | grep -cE "create table|drop table"                            # expect 0
```

`docker compose down -v` is the clean-slate path — `update` never drops stale
columns, so it is how you reset after an entity change.

## Running from source

The Compose stack seeds itself. Running a single instance with
`./mvnw spring-boot:run` gives an empty database; add the profile to populate
it with the same mock data:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```
