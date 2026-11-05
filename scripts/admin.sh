#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ "$#" -eq 1 ] || { echo 'Usage: bash scripts/admin.sh USERNAME (an existing local account)' >&2; exit 1; }
result=$(docker compose exec -T postgres psql -U stackr -d stackr_db -v ON_ERROR_STOP=1 -v "account=$1" -At <<'SQL'
UPDATE users SET role='ADMIN' WHERE username=:'account' RETURNING username;
SQL
)
[[ "$result" != 'UPDATE 0' ]] || { echo 'User not found.' >&2; exit 1; }
printf 'Administrator role assigned to the requested local account. Sign in again.\n'
