#!/usr/bin/env bash
# The committed schema contract matches what the migrations actually produce.
#
# Why this exists: it catches (a) a migration merged without regenerating the contract, so review
# never saw what the schema became, and (b) the contract hand-edited to paper over a difference.
# It is also the only check that notices an object someone created by hand in a live database and
# then back-filled into a migration that does not actually produce it.
#
# Wraps `generate-schema-contract.sh --check`. When Docker is unavailable it passes only if this
# branch changed nothing under the database paths — a branch that changes schema and cannot prove
# its contract does not get a green check by default.
#
# Usage: scripts/check-schema-drift.sh [<base-ref>]
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
cd "$AX_REPO_ROOT"

base="${1:-origin/$INTEGRATION_BRANCH}"

# An explicit opt-out, for repositories whose migrations are code (EF Core, Alembic) and are checked
# by the framework's own model/migration comparison instead. The reason is printed on every run.
if [ "$DB_SCHEMA_CONTRACT" = "none" ]; then
    echo "schema-drift: no SQL schema contract in this repository — ${DB_SCHEMA_CONTRACT_REASON:-no reason given (set DB_SCHEMA_CONTRACT_REASON)}."
    [ -n "${DB_SCHEMA_CONTRACT_REASON:-}" ] || exit 1
    exit 0
fi

if [ ! -f "$DB_SCHEMA_CONTRACT" ] && [ -z "$(git ls-files -- $DB_APPLIED_GLOBS 2>/dev/null)" ]; then
    echo "schema-drift: no database scripts and no contract in this repository; nothing to check."
    exit 0
fi

if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    exec "$here/generate-schema-contract.sh" --check
fi

changed="$(git diff --name-only "$base"...HEAD -- $DB_APPLIED_GLOBS "$DB_SCHEMA_CONTRACT" 2>/dev/null || true)"
if [ -n "$changed" ]; then
    echo "FAIL Docker is not running, and this branch changes database scripts:"
    printf '%s\n' "$changed" | sed 's/^/  /'
    echo "Start Docker and run scripts/generate-schema-contract.sh. Never approximate the contract."
    exit 1
fi
echo "schema-drift: Docker unavailable, but no database script changed on this branch; skipped."
