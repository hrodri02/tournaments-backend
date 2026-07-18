# Two explicit backend compose services instead of `--scale`

## Status
Accepted — 2026-07-18

## Context
Running multiple backend instances behind nginx's `hash ... consistent`
upstream requires nginx to know about a stable set of upstream servers. Two
ways to get multiple containers with Docker Compose were considered: a
single `backend` service scaled with `--scale backend=2`, or two distinct
named services.

## Decision
Replace the single `backend` service with two explicit services,
`backend-1` and `backend-2`, sharing configuration via YAML anchors
(`x-backend-common` for build/expose/depends_on, `x-backend-env` for the
common environment variables), each given a distinct `INSTANCE_ID`.

Rejected alternative: `docker compose up --scale backend=2`. nginx's
`hash ... consistent` builds its ring from a *static* list of `server`
entries in the `upstream` block; open-source nginx does not resolve a scaled
service's DNS round-robin into ring members cleanly. A scaled service's
replicas would also share one `INSTANCE_ID`, making them indistinguishable
via the `X-Instance-Id` header.

## Consequences
- The upstream pool membership (`backend-1:8080`, `backend-2:8080` in
  `devops/local/nginx/default.conf`) must be kept in sync by hand with the
  compose service names if the pool size ever changes.
- Each instance is individually addressable and stoppable
  (`docker compose stop backend-1`), which the failover verification script
  relies on directly.
- Scaling beyond two instances means adding more named services (and
  `upstream` entries) rather than a single `--scale` flag — acceptable for
  this local-only demonstration; a production setup would need a different
  mechanism (e.g. dynamic upstream resolution) to scale without manual edits.
