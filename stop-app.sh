#!/bin/bash

set -e

COMPOSE_FILE="devops/local/docker-compose.yml"

# Stop Spring app (if listening on 8080)
APP_ID=$(lsof -ti:8080 || true)
if [ -n "$APP_ID" ]; then
	kill -9 $APP_ID || true
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
	echo "[stop-app] Stopping Docker Compose services..."
	docker compose -f "$COMPOSE_FILE" down
else
	# Stop local PostgreSQL service if available
	if command -v systemctl >/dev/null 2>&1; then
		sudo systemctl stop postgresql || true
	fi

	# Stop local MailDev (if listening on 1080)
	MAILDEV_ID=$(lsof -ti:1080 || true)
	if [ -n "$MAILDEV_ID" ]; then
		kill -9 $MAILDEV_ID || true
	fi
fi