#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ "$#" -eq 2 ] || { echo 'Usage: bash scripts/restore-copy.sh backup.dump stackr_restore_NAME' >&2; exit 1; }
source_file="$1"; restore_db="$2"
[[ "$restore_db" =~ ^stackr_restore_[a-z0-9_]+$ ]] || { echo 'Restore database must be named stackr_restore_NAME (lowercase).' >&2; exit 1; }
[ -f "$source_file" ] || { echo 'Backup file not found.' >&2; exit 1; }
docker compose exec -T postgres createdb -U stackr "$restore_db"
docker compose exec -T postgres pg_restore -U stackr --dbname "$restore_db" --no-owner --exit-on-error < "$source_file"
printf 'Restored into separate database: %s. Primary stackr_db was preserved.\n' "$restore_db"
