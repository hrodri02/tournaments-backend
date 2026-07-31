# Consistent-hash key is the request URI, not the client IP

## Status
Accepted — 2026-07-18

## Context
Introducing a two-instance backend pool behind nginx requires choosing a key
for the `hash ... consistent` directive. The application is stateless: JWT
access tokens are RSA-signed and validated locally by any instance, refresh
tokens live in Postgres, and rate-limit buckets live in Redis. Which instance
serves a given request is therefore functionally irrelevant — the choice of
key only affects how observable/demonstrable the distribution is locally.

## Decision
Hash on `$request_uri`. A single local client (`curl`) naturally varies the
path across requests, making spread across the pool, per-path stickiness,
and failover all directly demonstrable without any multi-client-IP setup.

Rejected alternative: `$remote_addr` (client affinity). This only
demonstrates spread when many distinct client IPs are involved, and it
solves a problem — session/cache affinity — that this stateless app doesn't
have.

## Consequences
- All requests from one host client, repeated to the same path, always hit
  the same backend instance — this is the intended, demonstrable stickiness,
  not a bug.
- Consistent hashing does not guarantee even distribution across only two
  backends for a small number of distinct paths; the verification script
  curls several distinct paths to make the spread visible.
- If client/session affinity is ever needed in the future (e.g. an in-memory
  per-instance cache), the hash key would need to change to `$remote_addr` or
  a session/user identifier — this decision would need revisiting.
