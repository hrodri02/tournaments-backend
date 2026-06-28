## Requirements

### Requirement: Reverse proxy as single entry point

The system SHALL route all external HTTP traffic to the application through an nginx reverse proxy. The application container SHALL NOT publish its HTTP port directly to the host; it SHALL be reachable only on the internal container network via nginx.

#### Scenario: Request reaches the app through nginx

- **WHEN** a client sends an HTTP request to the published nginx port
- **THEN** nginx forwards the request to the backend application on the internal network
- **AND** the backend's response is returned to the client unchanged

#### Scenario: Backend is not directly reachable from the host

- **WHEN** a client attempts to connect to the backend's application port directly on the host
- **THEN** the connection is refused because the port is not published to the host

### Requirement: Per-client request rate limiting

The system SHALL limit the rate of requests accepted from a single client, identified by source IP address, using a configurable requests-per-second rate. Requests within the configured rate SHALL be proxied to the backend normally.

#### Scenario: Requests within the limit are served

- **WHEN** a client sends requests at or below the configured rate
- **THEN** nginx proxies each request to the backend
- **AND** the client receives the backend's normal response

#### Scenario: Independent clients are limited independently

- **WHEN** two clients with different source IP addresses each send requests
- **THEN** each client's rate is tracked separately
- **AND** one client exceeding its limit does not affect the other client

### Requirement: Burst handling and rejection of excess requests

The system SHALL allow a configurable short burst of requests above the steady rate before rejecting traffic. Requests that exceed the rate plus the allowed burst SHALL be rejected with HTTP status `429 Too Many Requests` rather than being forwarded to the backend.

#### Scenario: Burst above steady rate is absorbed

- **WHEN** a client briefly sends more requests than the steady rate but within the configured burst allowance
- **THEN** nginx queues or accepts the burst requests up to the burst limit
- **AND** forwards them to the backend

#### Scenario: Excess traffic is rejected with 429

- **WHEN** a client sends requests exceeding the configured rate plus burst allowance
- **THEN** nginx rejects the excess requests with HTTP status 429
- **AND** the rejected requests are not forwarded to the backend

### Requirement: Preservation of client context

The system SHALL forward the original client information to the backend so the application can observe the real request origin. nginx SHALL set the `Host`, `X-Forwarded-For`, and `X-Forwarded-Proto` headers on proxied requests.

#### Scenario: Client headers are forwarded

- **WHEN** nginx proxies a request to the backend
- **THEN** the request includes the original `Host` header
- **AND** the request includes an `X-Forwarded-For` header containing the client IP
- **AND** the request includes an `X-Forwarded-Proto` header reflecting the original scheme

### Requirement: Configurable rate-limit values

The system SHALL expose the rate-limiting parameters (steady request rate and burst size) as configuration that can be tuned without changing application code.

#### Scenario: Operator tunes the rate limit

- **WHEN** an operator changes the configured rate or burst value and restarts the proxy
- **THEN** nginx enforces the new limits
- **AND** no application code or rebuild is required
