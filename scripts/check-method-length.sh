#!/usr/bin/env bash
# No new god functions, in any language.
#
# Why this exists: see scripts/method-length.py. The short version — a function of 150+ lines
# cannot be read in one sitting or tested on its own, and it is where defects hide.
#
# Each tree in METHOD_LENGTH_TREES (".axelliant/standards.env") has its own baseline, so a change
# in one service cannot mask a regression in another. With no trees configured, the whole
# repository is one tree with baseline `.axelliant/method-length-baseline-repo.txt`.
#
# Baselines may only shrink. After splitting something up, regenerate the one you changed:
#   scripts/method-length.py baseline <tree> .axelliant/method-length-baseline-<name>.txt 150
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
. "$here/lib/python-interpreter.sh"
cd "$AX_REPO_ROOT"

trees="${METHOD_LENGTH_TREES:-.:repo}"
status=0
for entry in $trees; do
    tree="${entry%%:*}"
    name="${entry##*:}"
    baseline=".axelliant/method-length-baseline-${name}.txt"
    # A tree can be absent from a sparse or partial checkout; that is not a failure.
    [ -d "$tree" ] || continue
    printf '%-32s ' "$tree"
    "$PYTHON3" "$here/method-length.py" check "$tree" "$baseline" "$METHOD_LENGTH_LIMIT" || status=1
done
exit "$status"
