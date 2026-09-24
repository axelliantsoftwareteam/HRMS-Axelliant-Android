#!/usr/bin/env bash
# Request-path code logs through the framework logger, never the console.
#
# Why this exists: console output does not reach the APM, carries no correlation id, and has no
# level to filter on — so it is invisible when something goes wrong and noise when nothing is. It
# has also been used to print request payloads and an API key straight to stdout, where the
# hosting platform keeps them.
#
# Startup code runs before logging is configured, and CLIs/scripts talk to a terminal on purpose;
# CONSOLE_LOGGING_ALLOW in .axelliant/standards.env names those paths. Tests are always exempt.
#
# Usage: scripts/check-console-logging.sh [<base-ref>]   with a base, only lines added since it count
# (existing console calls in a legacy repository are standards debt; every new one fails).
#
# What it does NOT catch: a logger configured to write to the console with no structure. That is
# covered by the logging standard and reviewed, not grepped.
set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/config.sh"
cd "$AX_REPO_ROOT"

if [ -z "$REQUEST_PATH_TREES" ]; then
    echo "console-logging: REQUEST_PATH_TREES is not set in .axelliant/standards.env; nothing to scan."
    exit 0
fi

# One pattern per language: C#, Python, TS/JS, Java, Kotlin, Go, Swift, Dart.
pattern='(^|[^.[:alnum:]_])(Console\.(Write|WriteLine|Error\.Write)|print\(|console\.(log|debug|info|warn|error|trace)\(|System\.(out|err)\.print|println\(|fmt\.Print|NSLog\(|debugPrint\()'
test_re='(\.spec\.|\.test\.|_test\.|Tests?\.(cs|java|kt|swift)$|(^|/)(tests?|__tests__|spec|androidTest|testFixtures)/|(^|/)test_)'
build_re='(^|/)(obj|bin|node_modules|dist|build|target|\.venv|venv|\.next|coverage|vendor|\.gradle)/'

matches=""
for tree in $REQUEST_PATH_TREES; do
    [ -e "$tree" ] || continue
    found="$(grep -rnE "$pattern" "$tree" \
        --include='*.cs' --include='*.py' --include='*.ts' --include='*.tsx' --include='*.js' \
        --include='*.jsx' --include='*.mjs' --include='*.java' --include='*.kt' --include='*.go' \
        --include='*.swift' --include='*.dart' 2>/dev/null \
        | grep -Ev "$build_re" | grep -Ev "$test_re" | grep -Ev "$CONSOLE_LOGGING_ALLOW" \
        | grep -Ev ':[0-9]+:[[:space:]]*(//|#|\*|/\*)' || true)"
    [ -n "$found" ] && matches+="$found"$'\n'
done
if [ -n "${1:-}" ] && [ -n "${matches//$'\n'/}" ]; then
    . "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/python-interpreter.sh"
    matches="$(printf '%s' "$matches" | sed '/^$/d' | "$PYTHON3" "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/filter-added.py" "$1")"
fi

if [ -n "${matches//$'\n'/}" ]; then
    echo "Console output in request-path code. Use the injected framework logger so it reaches the APM"
    echo "with a level and a correlation id:"
    printf '%s' "$matches" | sed '/^$/d; s/^/  /'
    exit 1
fi
echo "No console output in request-path code."
