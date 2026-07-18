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

## How a request flows

```
client ──► nginx (upstream backend_pool, hash $request_uri consistent)
              │
              ├──► backend-1 (INSTANCE_ID=backend-1)
              └──► backend-2 (INSTANCE_ID=backend-2)

response ◄── X-Instance-Id: <backend-1|backend-2>
```

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

See `docs/adr/` for the full rationale and rejected alternatives behind each
decision.

## Components touched

- `src/main/java/com/example/tournaments_backend/instance/InstanceIdFilter.java`
  — sets the `X-Instance-Id` response header.
- `src/main/java/com/example/tournaments_backend/instance/InstanceIdConfig.java`
  — reads `INSTANCE_ID` and wires the filter as a bean.
- `src/main/java/com/example/tournaments_backend/security/config/SecurityConfig.java`
  — registers the filter ahead of the rate limiter.
- `devops/local/nginx/default.conf` — `upstream backend_pool` block.
- `devops/local/docker-compose.yml` — `backend-1`/`backend-2` services.
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
