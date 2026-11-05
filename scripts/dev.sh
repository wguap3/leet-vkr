#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v docker >/dev/null || { echo 'Install Docker Desktop with Docker Compose.' >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo 'Docker daemon is unavailable. Start Docker Desktop and retry.' >&2; exit 1; }
docker compose version >/dev/null
if [ ! -f .env ]; then
  command -v openssl >/dev/null || { echo 'openssl is required to generate local secrets.' >&2; exit 1; }
  (umask 077; {
    printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 24)"
    printf 'JWT_SECRET=%s\n' "$(openssl rand -base64 48 | tr -d '\n')"
    printf 'WEB_PORT=3100\nAPI_PORT=8080\nSANDBOX_IMAGE=eclipse-temurin:21-jdk-alpine\n'
  } > .env)
fi
image=$(sed -n 's/^SANDBOX_IMAGE=//p' .env | tail -1)
image="${image:-eclipse-temurin:21-jdk-alpine}"
if ! docker image inspect "$image" >/dev/null 2>&1; then
  docker pull "$image"
fi
docker compose up --build --detach --wait --wait-timeout 300
printf '\nStackr is running. Use the WEB_PORT from .env (default http://localhost:3100).\n'
