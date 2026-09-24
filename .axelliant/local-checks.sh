#!/usr/bin/env bash
# The build and tests CI runs (.github/workflows/android-ci.yml). Owned by this repository; called by
# scripts/check-before-push.sh. Needs JDK 17 and the Android SDK (compileSdk 36).
# Android lint, the JVM unit tests and the debug build. ktlint, detekt and a Kover coverage report
# are not configured yet (standards debt in docs/ledger.md); add them here, and the coverage floor,
# when they are.
set -euo pipefail
root="$(git rev-parse --show-toplevel)"
cd "$root"
bash ./gradlew --no-daemon lint testDebugUnitTest assembleDebug --stacktrace
