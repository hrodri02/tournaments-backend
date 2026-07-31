# Local development dependencies

This folder contains local infrastructure for backend development.

## Requirements

- **Docker is required** (Docker Engine + Docker Compose plugin).

You can verify your installation with:

```bash
docker --version
docker compose version
```

## Install Docker

### Ubuntu / Debian (recommended)

Use Docker's official installation guide:

- [Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)

After installation, allow running Docker without `sudo` (recommended):

```bash
sudo usermod -aG docker "$USER"
newgrp docker
```

### macOS

Install **Docker Desktop**:

- [Docker Desktop for Mac](https://docs.docker.com/desktop/setup/install/mac-install/)

### Windows

Install **Docker Desktop**:

- [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)

> Note: On Windows, enable WSL2 integration for best compatibility.

## Services

- **nginx** on `http://localhost` — the only host-facing entry point for the API
- **PostgreSQL** on `localhost:5432`
- **MailDev SMTP** on `localhost:1025`
- **MailDev UI** on `http://localhost:1080`
- **Redis** — rate-limiter store, internal network only
- **backend-1 / backend-2** — two Spring Boot instances, internal network only;
  reach them through nginx, which load-balances with consistent hashing
- **db-init** — one-shot seeder that creates the schema and mock data, then exits

## Start services

```bash
docker compose -f devops/local/docker-compose.yml up -d
```

Startup order is enforced by the compose file: `db-init` waits for Postgres, seeds,
and exits 0; only then do the backends start. Verify the seeder succeeded with:

```bash
docker compose -f devops/local/docker-compose.yml logs db-init
docker compose -f devops/local/docker-compose.yml ps -a db-init   # expect Exited (0)
```

`db-init` is idempotent: restarting a backend re-triggers it, but it finds the schema
and data already present and exits without touching them. Use `down -v` for a clean
slate:

```bash
docker compose -f devops/local/docker-compose.yml down -v
```

## Stop services

```bash
docker compose -f devops/local/docker-compose.yml down
```

## View logs

```bash
docker compose -f devops/local/docker-compose.yml logs -f
```

## Run backend app

The full Compose stack above already runs two backend instances behind nginx. To run a
single instance from source instead, start only the dependencies and then:

```bash
./mvnw spring-boot:run
```

That gives you an empty database. To populate it with the same mock data the `db-init`
container creates, activate the `seed` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

## Notes

- Database config is aligned with `src/main/resources/application.properties`.
- Under Compose, `SPRING_JPA_HIBERNATE_DDL_AUTO` is overridden per service: `update` for
  `db-init`, `validate` for the backends. Only `db-init` may change the schema — two
  serving instances issuing DDL against one database would drop each other's tables.
- If ports are occupied, stop conflicting services or adjust port mappings.
