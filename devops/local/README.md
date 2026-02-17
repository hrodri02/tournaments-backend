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

- **PostgreSQL** on `localhost:5432`
- **MailDev SMTP** on `localhost:1025`
- **MailDev UI** on `http://localhost:1080`

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

## Notes

- Database config is aligned with `src/main/resources/application.properties`.
- If ports are occupied, stop conflicting services or adjust port mappings.
