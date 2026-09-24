#!/usr/bin/env bash
# Resolve a genuinely working Python 3 into $PYTHON3.
#
# Source this, do not execute it:
#
#     . "$(dirname "${BASH_SOURCE[0]}")/lib/python-interpreter.sh"
#     "$PYTHON3" scripts/some-check.py
#
# Why this exists: Windows ships a "python3" App Execution Alias that is only a
# Microsoft Store advert. `command -v python3` finds it, so every guardrail that
# located an interpreter instead of running one believed Python was present;
# invoking it prints "Python was not found..." and exits 49. Under `set -e` that
# aborted the pre-commit hook on every Windows machine while Linux CI stayed
# green, so the failure looked like a broken repository rather than a missing
# interpreter.
#
# Each candidate is therefore executed and asked its version. The first that
# answers "3" wins.

# shellcheck disable=SC2034  # PYTHON3 is consumed by the scripts that source this file
resolve_python3() {
    local candidate
    for candidate in python3 python py; do
        if command -v "$candidate" >/dev/null 2>&1 \
           && "$candidate" -c 'import sys; sys.exit(0 if sys.version_info[0] == 3 else 1)' \
              >/dev/null 2>&1; then
            PYTHON3="$candidate"
            return 0
        fi
    done

    echo "No working Python 3 interpreter found (tried: python3, python, py)." >&2
    echo "Install Python 3.12+ and make sure it is on PATH." >&2
    return 1
}

resolve_python3
