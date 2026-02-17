#!/bin/bash

set -e

COMPOSE_FILE="devops/local/docker-compose.yml"

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    echo "[start-app] Starting PostgreSQL + MailDev with Docker Compose..."
    docker compose -f "$COMPOSE_FILE" up -d
else
    echo "[start-app] Docker Compose not available. Falling back to local services..."

    # Start maildev locally if available
    if command -v maildev >/dev/null 2>&1; then
        maildev &
    else
        echo "[start-app] maildev not found in PATH. Install it or use Docker Compose."
    fi

    # Start PostgreSQL service locally if not running
    if command -v systemctl >/dev/null 2>&1 && ! systemctl is-active --quiet postgresql; then
        sudo systemctl start postgresql
    fi
fi

echo "[start-app] Starting tournaments backend app..."
java -jar target/tournaments-backend-0.0.1-SNAPSHOT.jar &