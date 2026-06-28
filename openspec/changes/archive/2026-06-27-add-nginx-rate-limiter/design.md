## Context

The local stack (`devops/local/docker-compose.yml`) runs three services: `postgres`, `maildev`, and `backend` (the Spring Boot app, currently publishing `8080:8080` to the host). There is no protection against request floods — clients hit the JVM directly. We want a dedicated reverse-proxy layer to enforce rate limiting before traffic reaches the application, as established in `proposal.md`. Requirements are defined in `specs/api-rate-limiting/spec.md`.

Constraints:
- This is the local-development compose file; the design should stay simple and mirror what a production proxy would do without introducing heavy tooling.
- The app listens on `8080` with no context path.
- Configuration conventions already use `devops/local/.env` for tunables.

## Goals / Non-Goals

**Goals:**
- Run nginx as its own container that is the single published entry point to the API.
- Enforce a per-source-IP request-rate limit with a burst allowance, rejecting excess with `429`.
- Keep rate-limit parameters tunable via config without touching app code.
- Preserve real client context (`Host`, `X-Forwarded-For`, `X-Forwarded-Proto`) to the backend.

**Non-Goals:**
- TLS/HTTPS termination (kept HTTP for local; can be layered later).
- Authentication, WAF, or per-endpoint/per-user rate policies (single global zone for now).
- Production orchestration (Kubernetes, autoscaling) — this targets the local compose stack.
- Distributed/shared rate-limit state across multiple nginx replicas.

## Decisions

**Decision: Use the official `nginx` image with a mounted config file rather than a custom-built image.**
Mount `devops/local/nginx/nginx.conf` (and a server block) read-only into `nginx:alpine`. Rationale: no Dockerfile to maintain, config is versioned and visible, and edits only require a container restart. Alternative considered: building a custom image that bakes in the config — rejected as unnecessary overhead for local dev and slower to iterate on.

**Decision: Use `limit_req_zone` keyed on `$binary_remote_addr` with `limit_req` + `burst` + `nodelay`.**
Define a shared-memory zone keyed by client IP, set a steady `rate` (e.g. `10r/s`), and apply it per location with a `burst` (e.g. `20`) and `nodelay` so bursts are served immediately rather than throttled with latency. Rationale: this is nginx's standard, well-understood rate-limiting primitive and directly satisfies the spec's rate + burst + 429 requirements. Alternative considered: `limit_conn` (connection-based) — rejected because the requirement is about request rate, not concurrent connections; the two can be combined later if needed.

**Decision: Set `limit_req_status 429`.**
nginx defaults to `503` for rejected requests; the spec requires `429 Too Many Requests`, which is the semantically correct status. Override it explicitly.

**Decision: Stop publishing `8080` on `backend`; publish `80` on `nginx` only.**
`backend` keeps `expose`/internal networking so nginx can reach it at `http://backend:8080`, but the host port mapping is removed. Rationale: enforces the spec requirement that the backend is not directly reachable, so the rate limiter cannot be bypassed. Trade-off documented below.

**Decision: Source rate values from `.env` via a templated config.**
Use nginx's `envsubst` (supported by the official image via `/etc/nginx/templates/*.template` → `/etc/nginx/conf.d/`) so `RATE_LIMIT` and `RATE_BURST` come from `devops/local/.env`. Rationale: satisfies the "configurable without code changes" requirement using a built-in mechanism. Alternative considered: hardcoding values in the static config — rejected because it fails the tunability requirement; the template approach is the image's documented pattern.

## Risks / Trade-offs

- **Developers lose direct `localhost:8080` access** → Document the new `http://localhost` entry point in `README.md`; optionally keep `8080` published in a commented-out block for debugging if a developer needs to bypass the proxy.
- **Rate limit too aggressive could block legitimate dev/test traffic (e.g. Swagger, test suites hammering endpoints)** → Choose conservative defaults (e.g. `10r/s`, burst `20`) and make them `.env`-tunable; note in README how to raise them.
- **`X-Forwarded-For` can be spoofed by clients** → Acceptable for local dev; in production this would be paired with `set_real_ip_from`/trusted-proxy config. Noted as future work, out of scope here.
- **`envsubst` templating can accidentally substitute nginx runtime variables like `$binary_remote_addr`** → Constrain substitution to only the intended variables (the official image supports `NGINX_ENVSUBST_FILTER`, or escape nginx vars), and verify the rendered config on startup.
- **Single nginx instance is a new single point of failure** → Acceptable for local; `restart: unless-stopped` mirrors the other services.

## Migration Plan

1. Add `devops/local/nginx/` config (template) and the `nginx` service to compose.
2. Add `RATE_LIMIT` / `RATE_BURST` to `.env`.
3. Remove the `8080:8080` host mapping from `backend`.
4. `docker compose ... up -d` and verify: normal requests succeed via `http://localhost`, rapid bursts return `429`, and `localhost:8080` is refused.
5. Update `README.md`.

Rollback: revert the compose change (re-add `8080:8080`, remove the `nginx` service) and restart — no data or schema impact since nothing application-side changes.

## Open Questions

- Should Swagger UI / actuator-style paths be exempt from rate limiting (separate `location` without `limit_req`), or is a single global policy acceptable for now? (Leaning: single global policy; revisit if it interferes with local testing.)
- Final default values for `RATE_LIMIT` and `RATE_BURST` — confirm during implementation against how the existing test suite exercises the API.
