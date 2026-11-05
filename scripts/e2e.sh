#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/prepare-e2e.py
version=$(node -p "require('./web/node_modules/@playwright/test/package.json').version")
# Copy instead of bind-mounting: Docker Desktop may lack access to Downloads.
container=$(docker create --init --ipc=host --network stackr-local_default \
  --workdir /app --env STACKR_URL=http://web \
  "mcr.microsoft.com/playwright:v${version}-noble" npx playwright test "$@")
cleanup() { docker rm -f "$container" >/dev/null 2>&1 || true; }
trap cleanup EXIT
COPYFILE_DISABLE=1 tar --no-xattrs -C web --exclude=test-results --exclude=playwright-report -cf - . | docker cp - "$container:/app"
docker start -a "$container" || true
status=$(docker inspect -f '{{.State.ExitCode}}' "$container")
rm -rf web/test-results web/playwright-report
docker cp "$container:/app/test-results" web/ 2>/dev/null || true
docker cp "$container:/app/playwright-report" web/ 2>/dev/null || true
exit "$status"
