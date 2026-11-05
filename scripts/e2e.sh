#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/prepare-e2e.py
version=$(node -p "require('./web/node_modules/@playwright/test/package.json').version")
# Chromium runs in the official testing image, avoiding host browser permissions.
docker run --rm --init --ipc=host --network stackr-local_default \
  --mount "type=bind,src=$(pwd)/web,dst=/app" --workdir /app \
  --env STACKR_URL=http://web "mcr.microsoft.com/playwright:v${version}-noble" npx playwright test "$@"
