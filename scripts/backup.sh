#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dest="${1:-backups/stackr-$(date -u +%Y%m%dT%H%M%SZ).dump}"
[ ! -e "$dest" ] || { echo 'Destination already exists; choose a new backup filename.' >&2; exit 1; }
mkdir -p "$(dirname "$dest")"
umask 077
trap 'rm -f "$dest.partial"' EXIT
docker compose exec -T postgres pg_dump -U stackr -d stackr_db --format=custom > "$dest.partial"
docker compose exec -T postgres pg_restore --list < "$dest.partial" >/dev/null
mv "$dest.partial" "$dest"
printf 'Verified PostgreSQL backup: %s\n' "$dest"
