#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "${SKIP_PRE_PUSH:-}" == "1" ]]; then
  echo "Skipping pre-push hook because SKIP_PRE_PUSH=1"
  exit 0
fi

if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
  echo "gradlew is not executable. Fix with: chmod +x gradlew" >&2
  exit 1
fi

echo "Running Android quality gate: lint, unit tests, and assembleDebug"
./gradlew lint testDebugUnitTest assembleDebug
