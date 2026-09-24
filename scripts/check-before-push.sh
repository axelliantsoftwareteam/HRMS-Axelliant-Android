#!/usr/bin/env bash
# The single command that runs everything CI will run, before you push.
#
# Why this exists: a red CI run costs a context switch and a queue slot; the same failure caught
# here costs seconds. It runs every gate, keeps going after a failure so you see them all at once,
# and prints a table at the end. Then it runs the repository's own build and tests from
# .axelliant/local-checks.sh (provided by the stack templates).
#
# Usage: scripts/check-before-push.sh [--fast]     --fast skips the stack build/test and Docker checks
set -uo pipefail
# Run from a hook, git exports GIT_DIR and friends; every tool and test below finds its repository
# from the working directory instead (see scripts/tests/harness.py for what the leak once did).
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_PREFIX

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
. "$here/lib/python-interpreter.sh"
cd "$AX_REPO_ROOT" || exit 1

fast=0
[ "${1:-}" = "--fast" ] && fast=1

git fetch -q origin "$INTEGRATION_BRANCH" 2>/dev/null || true
base="origin/$INTEGRATION_BRANCH"
# A new repository may have no integration branch yet, or only one commit; the gates fall back to
# comparing with the empty tree when the base does not resolve.
git rev-parse -q --verify "$base" >/dev/null || base="origin/$RELEASE_BRANCH"
git rev-parse -q --verify "$base" >/dev/null || base="HEAD~1"

results=()
run() {
    local name="$1"; shift
    printf '\n\033[1m== %s\033[0m\n' "$name"
    if "$@"; then results+=("PASS  $name"); else results+=("FAIL  $name"); fi
}

run "repo hygiene"          "$here/check-repo-hygiene.sh"
run "branch name"           "$here/check-branch-name.sh"
run "method length"         "$here/check-method-length.sh"
run "file-length budget"    "$here/check-file-length-budget.sh" "$base"
run "naming conventions"    "$PYTHON3" "$here/check-naming-conventions.py" "$base"
run "code documentation"    "$PYTHON3" "$here/check-code-docs.py" "$base"
run "exception leaks"       "$here/check-exception-leaks.sh" "$base"
run "console logging"       "$here/check-console-logging.sh" "$base"
run "applied db scripts"    "$PYTHON3" "$here/check-applied-scripts.py" "$base"
run "migration extensions"  "$PYTHON3" "$here/check-migration-extensions.py"
run "docs drift"            "$PYTHON3" "$here/check-docs-drift.py"
run "workflow run steps"    "$PYTHON3" "$here/check-workflow-interpolation.py"
run "docker"                "$PYTHON3" "$here/check-docker.py"
run "infrastructure as code" "$PYTHON3" "$here/check-infra-as-code.py"
run "coverage ratchet"      "$PYTHON3" "$here/check-coverage.py" ratchet "$base"
[ -f .axelliant/standards.lock ] && run "standards in sync" "$here/sync-standards.sh" --check
[ -d "$here/tests" ] && run "guard-script tests" "$PYTHON3" -m unittest discover -s "$here/tests" -q
if command -v gitleaks >/dev/null 2>&1; then
    run "secret scan"       gitleaks detect --redact --no-banner --log-opts="$base..HEAD"
fi
if [ "$fast" -eq 0 ]; then
    run "schema contract"   "$here/check-schema-drift.sh" "$base"
    [ -x .axelliant/local-checks.sh ] && run "stack build, lint and tests" .axelliant/local-checks.sh
fi

printf '\n\033[1m== Summary\033[0m\n'
failed=0
for row in "${results[@]}"; do
    case "$row" in FAIL*) failed=1; printf '\033[31m%s\033[0m\n' "$row" ;; *) printf '\033[32m%s\033[0m\n' "$row" ;; esac
done
[ "$failed" -eq 0 ] && echo "All checks passed. Safe to push." || echo "Fix the failures above before pushing."
exit "$failed"
