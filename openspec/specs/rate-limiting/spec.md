# rate-limiting Specification

## Purpose

Protect the API from abuse and traffic spikes by enforcing a global per-client-IP request limit on all HTTP endpoints. The limiter uses a Redis-backed sliding window counter so that limits are shared across application instances, identifies clients by IP address behind an `nginx` reverse proxy, is configurable and toggleable, and fails closed when its data store is unreachable.

## Requirements

### Requirement: Per-IP request throttling middleware

The system SHALL enforce a global per-client-IP request limit on all HTTP endpoints using a servlet filter that runs before authentication, so that both authenticated and `permitAll` routes are covered.

#### Scenario: Request within the limit is allowed
- **WHEN** a client IP has made fewer requests than the configured limit within the current sliding window
- **THEN** the request proceeds down the filter chain to the target endpoint
- **AND** the response includes `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` headers

#### Scenario: Request exceeding the limit is throttled
- **WHEN** a client IP's estimated request count within the sliding window exceeds the configured limit
- **THEN** the system responds with HTTP `429` and does not invoke the target endpoint
- **AND** the response body is an `ErrorDetails` JSON with `errorKey` `RATE_LIMIT_EXCEEDED`
- **AND** the response includes a `Retry-After` header and the `X-RateLimit-*` headers

### Requirement: Sliding window counter over Redis

The system SHALL compute the request count using the sliding window counter algorithm with Redis as the shared data store, combining the current and previous fixed-window counters weighted by elapsed time. The increment-and-decide operation MUST be atomic.

#### Scenario: Counting blends current and previous windows
- **WHEN** the limiter evaluates a request
- **THEN** it computes `estimate = current_window_count + previous_window_count * (1 - elapsed_fraction_of_current_window)`
- **AND** it throttles the request when `estimate` is greater than the configured limit

#### Scenario: Counters expire automatically
- **WHEN** a window counter key is created in Redis
- **THEN** it is stored under key `ratelimit:{ip}:{windowNumber}` with a TTL of twice the window length

#### Scenario: Limit resets after the window passes
- **WHEN** a client was throttled and then no requests arrive for longer than the window
- **THEN** subsequent requests from that IP are allowed again

### Requirement: Client identification by IP address

The system SHALL identify clients by IP address, using the first entry of the `X-Forwarded-For` header when present, and falling back to the request's remote address otherwise. This first-entry rule is trustworthy because the deployment places an `nginx` reverse proxy in front of the application that overwrites `X-Forwarded-For` with the real connecting client IP, and the application is not reachable except through that proxy.

#### Scenario: X-Forwarded-For present
- **WHEN** a request carries an `X-Forwarded-For` header with one or more IPs
- **THEN** the limiter uses the first IP in that header as the client identifier

#### Scenario: X-Forwarded-For absent
- **WHEN** a request has no `X-Forwarded-For` header
- **THEN** the limiter uses `request.getRemoteAddr()` as the client identifier

#### Scenario: Client-supplied X-Forwarded-For cannot spoof identity
- **WHEN** a client sends its own `X-Forwarded-For` header through the `nginx` proxy
- **THEN** nginx overwrites that header with the real connecting client IP before the request reaches the application
- **AND** the limiter therefore attributes the request to the real client, not the spoofed value

### Requirement: Configurable limit and toggle

The system SHALL expose the rate limit through `ratelimit.*` configuration properties, defaulting to 10 requests per 1-second window, and SHALL allow the limiter to be disabled.

#### Scenario: Defaults applied
- **WHEN** no `ratelimit` properties are overridden
- **THEN** the limit is 10 requests and the window is 1 second

#### Scenario: Limiter disabled
- **WHEN** `ratelimit.enabled` is `false`
- **THEN** all requests pass through without any Redis interaction or rate-limit headers

### Requirement: Fail closed when Redis is unavailable

The system SHALL reject requests when the Redis store cannot be reached, distinguishing this infrastructure failure from a client exceeding its limit.

#### Scenario: Redis unreachable
- **WHEN** the limiter's Redis operation raises a connection or Redis error
- **THEN** the system responds with HTTP `503`
- **AND** the response body is an `ErrorDetails` JSON with `errorKey` `RATE_LIMIT_UNAVAILABLE`
