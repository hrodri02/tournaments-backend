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

| Service | Port | Notes |
|---------|------|-------|
| **nginx** (reverse proxy) | `http://localhost` | Public entry point for the API and Swagger UI |
| **PostgreSQL** | `localhost:5432` | Application database |
| **MailDev SMTP** | `localhost:1025` | Local SMTP for email flows |
| **MailDev UI** | `http://localhost:1080` | Browse sent emails |

> **Note:** The backend is no longer published directly on `localhost:8080`. All traffic goes through nginx on port 80. Use `http://localhost` and `http://localhost/swagger-ui.html` for local API access.

## Start services

```bash
docker compose -f devops/local/docker-compose.yml up -d
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

After dependencies are up:

```bash
./mvnw spring-boot:run
```

## Rate limiting

nginx enforces a per-client-IP request rate limit. The defaults are defined in `devops/local/.env`:

| Variable | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT` | `10r/s` | Steady request rate per client IP |
| `RATE_BURST` | `20` | Extra requests allowed above the steady rate before nginx returns `429` |

To relax limits during heavy local testing (e.g. running automated test suites), raise these values in `.env` and restart the nginx container:

```bash
# In devops/local/.env
RATE_LIMIT=100r/s
RATE_BURST=200

docker compose -f devops/local/docker-compose.yml restart nginx
```

## Notes

- Database config is aligned with `src/main/resources/application.properties`.
- If ports are occupied, stop conflicting services or adjust port mappings.
