#!/usr/bin/env bash
# An exception's own words never reach an API caller.
#
# Why this exists and what it does not catch: see scripts/exception-leaks.py.
# Scans REQUEST_PATH_TREES from .axelliant/standards.env. The reporting helper and the documented
# domain exception are named by EXCEPTION_REPORTING_ALLOW.
#
# Usage: scripts/check-exception-leaks.sh [<base-ref>]   with a base, only lines added since it count
#        (CI and check-before-push pass one; run with no argument for a full audit).
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
. "$here/lib/python-interpreter.sh"
cd "$AX_REPO_ROOT"

if [ -z "$REQUEST_PATH_TREES" ]; then
    echo "exception-leaks: REQUEST_PATH_TREES is not set in .axelliant/standards.env; nothing to scan."
    echo "exception-leaks: set it to the directories that serve requests (e.g. 'src/Api src/Application')."
    exit 0
fi

roots=()
for tree in $REQUEST_PATH_TREES; do [ -e "$tree" ] && roots+=("$tree"); done
[ "${#roots[@]}" -gt 0 ] || { echo "exception-leaks: no configured tree exists in this checkout."; exit 0; }

"$PYTHON3" "$here/exception-leaks.py" --allow "$EXCEPTION_REPORTING_ALLOW" --base "${1:-}" "${roots[@]}"
