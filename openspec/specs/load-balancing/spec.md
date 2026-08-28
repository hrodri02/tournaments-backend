# load-balancing Specification

## Purpose

Distribute API traffic across multiple interchangeable backend instances behind the nginx reverse proxy using consistent hashing (ketama) keyed on the request URI, so the application can scale horizontally while the routing behavior stays observable and verifiable locally. The serving instance is identifiable from a response header, failover to a healthy instance is graceful, and the rate limiter's client identification is unaffected. Because the instances share one database, schema and seed-data ownership belongs to a single initializer that runs to completion before any instance starts, and each service's memory footprint is bounded so a multi-instance stack stays predictable on a developer machine.

## Requirements

### Requirement: Consistent-hash request distribution across backend instances

nginx SHALL distribute incoming requests across the pool of backend instances using consistent hashing (ketama) keyed on the request URI. Requests for the same URI path SHALL be routed to the same backend instance as long as pool membership is unchanged.

#### Scenario: Same path is routed to the same instance
- **WHEN** a client sends multiple requests to the same URI path (e.g. `/api/leagues`)
- **THEN** every request for that path is handled by the same backend instance
- **AND** the `X-Instance-Id` response header is identical across those requests

#### Scenario: Different paths spread across the pool
- **WHEN** a client sends requests to several distinct URI paths
- **THEN** the requests are distributed across more than one backend instance (as reflected by differing `X-Instance-Id` values)

### Requirement: Graceful failover when an instance is unavailable

nginx SHALL continue serving requests using the remaining healthy backend instance(s) when one instance becomes unavailable. Because distribution uses consistent hashing, only the paths previously mapped to the unavailable instance SHALL be remapped; paths mapped to healthy instances SHALL be unaffected.

#### Scenario: One backend instance is stopped
- **WHEN** one backend instance is stopped and a client continues sending requests
- **THEN** requests still receive successful responses served by a remaining healthy instance
- **AND** the `X-Instance-Id` header reflects a healthy instance

### Requirement: Serving instance is identified via response header

Every response SHALL include an `X-Instance-Id` header whose value identifies the backend instance that served the request. The value SHALL be derived from the instance's `INSTANCE_ID` environment variable, falling back to a default value when the variable is not set so the application still functions outside the multi-container setup.

#### Scenario: Header reflects the configured instance id
- **WHEN** a backend instance configured with `INSTANCE_ID=backend-1` serves a request
- **THEN** the response includes the header `X-Instance-Id: backend-1`

#### Scenario: Default value when INSTANCE_ID is unset
- **WHEN** the application serves a request and `INSTANCE_ID` is not set in the environment
- **THEN** the response still includes an `X-Instance-Id` header with the default fallback value

### Requirement: Rate-limiter client identification is preserved

The introduction of the upstream pool SHALL NOT change how the client IP is forwarded to backends. nginx SHALL continue to overwrite the `X-Forwarded-For` header with the real connecting peer address (`$remote_addr`), discarding any client-supplied value, so the rate limiter keeps identifying clients correctly.

#### Scenario: X-Forwarded-For is overwritten with the peer address
- **WHEN** a client sends a request through nginx with a spoofed `X-Forwarded-For` header
- **THEN** the backend receives `X-Forwarded-For` set to the real connecting peer address, not the spoofed value

### Requirement: Database schema and seed data are owned by a single initializer

When multiple backend instances share one database, no serving instance SHALL create, alter, or drop the schema, and application seed data SHALL be created only once. Schema and seed ownership SHALL belong to a dedicated initializer that runs to completion before any serving instance starts. Serving instances SHALL validate the schema they find rather than generating it, so that no instance can modify the schema underneath another.

#### Scenario: Serving instances issue no DDL
- **WHEN** `backend-1` and `backend-2` start against the same database
- **THEN** neither instance issues any `CREATE TABLE` or `DROP TABLE` statement
- **AND** each instance validates the existing schema and fails fast if it does not match the entity model

#### Scenario: Seed data exists exactly once after both instances start
- **WHEN** `backend-1` and `backend-2` have both started against the same database
- **THEN** exactly one set of seed users exists
- **AND** the row count is unchanged from the moment the initializer completed

#### Scenario: Serving instances wait for initialization to complete
- **WHEN** the stack is started from an empty database
- **THEN** the initializer creates the schema, seeds, and terminates successfully
- **AND** no serving instance starts until it has terminated successfully

#### Scenario: Re-running the initializer leaves an existing database untouched
- **WHEN** the initializer runs again against a database that already holds schema and seed data
- **THEN** it drops no tables and inserts no additional rows
- **AND** it terminates successfully, leaving existing data and identifiers unchanged

#### Scenario: Seeding is inert outside the initializer
- **WHEN** the application starts without the seeding profile active
- **THEN** no seed data is written, regardless of whether the database is empty

### Requirement: Per-service memory limits bound the local stack footprint

Each compose service SHALL declare a memory limit sized for running two backend instances alongside shared infrastructure, and each backend JVM SHALL size its heap relative to its container's memory limit rather than host memory.

#### Scenario: Backend container respects its memory limit
- **WHEN** a backend container starts with a configured `mem_limit`
- **THEN** the JVM sizes its heap using `-XX:MaxRAMPercentage=75.0` relative to the container's memory limit
