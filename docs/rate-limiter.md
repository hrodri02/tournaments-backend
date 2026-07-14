# Rate Limiter

Per-IP request throttling for every HTTP endpoint, backed by a Redis-shared
sliding window counter. Protects the API — including the unauthenticated `auth`
routes — from brute-force, scraping, and accidental request floods, consistently
across multiple app instances.

## What it does

- Enforces **one global per-IP limit** on all routes (default **10 requests / 1 s**).
- Runs as a servlet filter **before authentication**, so floods are rejected
  before any auth work and `permitAll` routes are covered too.
- Counts requests with a **sliding window counter** over Redis; the
  increment-and-decide step is a single atomic Lua script.
- **Fails closed**: if Redis is unreachable the request is rejected with `503`,
  distinct from a client hitting its limit (`429`).
- Returns standards-friendly headers and reuses the existing `ErrorDetails` JSON.

## How a request flows

```
client ──► nginx ──► RateLimitFilter ──► auth filters ──► controller
              │            │
   overwrites │            │ atomic Lua script on Redis:
   X-Forwarded-For         │  INCR current window, GET previous window,
   with the real IP        │  estimate = current + previous * weight
                           ▼
              allowed? proceed : 429   |   Redis down? 503
```

## Responses

| Situation | Status | `errorKey` | Extra headers |
|---|---|---|---|
| Within limit | (passes through) | — | `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` |
| Over limit | `429` | `RATE_LIMIT_EXCEEDED` | above + `Retry-After` |
| Redis unavailable | `503` | `RATE_LIMIT_UNAVAILABLE` | — |

`X-RateLimit-Remaining` = `max(0, limit - estimate)`; `X-RateLimit-Reset` /
`Retry-After` = seconds until the current window rolls over (min 1).

## Design decisions

- **Atomic Lua script** — increment and allow/deny run as one script so there is
  no read-decide-write race under concurrency, and it's a single Redis round-trip
  on the hot path (`src/main/resources/sliding_window.lua`).
- **Window math in Java with an injectable `Clock`** — the app clock is
  authoritative (not Redis `TIME`), which keeps the script portable and the
  window/weight math deterministic in unit tests.
- **`OncePerRequestFilter`** — guarantees the check (and its `INCR`) runs exactly
  once per request even across FORWARD/ASYNC re-dispatches.
- **Filter writes the error body directly** — it runs before
  `@RestControllerAdvice`, so it serializes `ErrorDetails` with the injected
  Jackson `ObjectMapper` instead of throwing.
- **Registered inside the security chain** via `addFilterBefore(...)` (a disabled
  `FilterRegistrationBean` stops Spring Boot from also auto-registering it at the
  servlet level, which would run it twice).
- **Client IP = first `X-Forwarded-For` entry, else `getRemoteAddr()`** — trusted
  because an **nginx reverse proxy overwrites `X-Forwarded-For` with the real
  connecting IP** (`proxy_set_header X-Forwarded-For $remote_addr;`) and the
  backend is reachable only through nginx. A client cannot spoof the header to
  scatter its requests across buckets.

## Configuration (`ratelimit.*`)

| Property | Default | Meaning |
|---|---|---|
| `ratelimit.enabled` | `true` | When `false`, the filter passes everything through (kill-switch). |
| `ratelimit.requests` | `10` | Max requests per IP per window. |
| `ratelimit.window-seconds` | `1` | Sliding window length. |

Redis connection: `spring.data.redis.host` / `spring.data.redis.port`
(defaults `localhost:6379`; overridden to `redis` in compose).

## Files

**Production (`com.example.tournaments_backend.ratelimit`)**
- `RateLimitProperties` — binds `ratelimit.*`.
- `ClientIpResolver` — resolves the client IP.
- `SlidingWindowRateLimiter` — window math + atomic script call; returns a `Decision`.
- `Decision` — result carrier (allowed, limit, remaining, retryAfterSeconds, resetSeconds).
- `RateLimitFilter` — `OncePerRequestFilter`; sets headers / writes `ErrorDetails`.
- `RateLimitConfig` — `Clock` + `RedisScript` beans, disabled `FilterRegistrationBean`.
- `RateLimitUnavailableException` — signals the fail-closed 503 path.
- `src/main/resources/sliding_window.lua` — the atomic counter script.

**Modified**
- `exception/ClientErrorKey` — added `RATE_LIMIT_EXCEEDED`, `RATE_LIMIT_UNAVAILABLE`.
- `security/config/SecurityConfig` — `addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)`.
- `pom.xml` — added `spring-boot-starter-data-redis`.
- `application.properties` — Redis + `ratelimit.*` defaults.
- `devops/local/docker-compose.yml` + `devops/local/nginx/default.conf` — Redis, nginx, backend no longer host-published.

## How to verify

**Automated**

```bash
./mvnw test
```

Unit tests cover the IP resolver, the limiter (fixed `Clock`, mocked Redis), and
the filter. `SlidingWindowRateLimiterIntegrationTest` exercises the real Lua
behavior against a Redis Testcontainer, including the fail-closed path.

**Manually against the stack**

```bash
docker compose -f devops/local/docker-compose.yml up -d --build

# 10 pass, then 429 (all traffic goes through nginx on :8080)
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui/index.html
done

# Spoofing is ignored — nginx overwrites X-Forwarded-For, so these still throttle
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "X-Forwarded-For: 9.9.9.$i" http://localhost:8080/swagger-ui/index.html
done
```
