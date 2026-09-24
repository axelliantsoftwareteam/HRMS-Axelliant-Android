#!/usr/bin/env bash
# Nothing that should not be in git is in git.
#
# Why this exists: three classes of file keep arriving in repositories, and each costs more to
# remove than it would have cost to refuse.
#
#   * Build output (bin/, obj/, dist/, target/, DerivedData/, .next/ ...) — it churns on every
#     build, makes every diff unreadable, and has shipped stale binaries that no source produced.
#   * Secret-shaped files (.env, *.pem, *.key, *.pfx, id_rsa, secrets.json, keystores) — a
#     secret that reaches a remote is burned. Deleting the file does not un-publish it; only
#     rotation does, and rotation is an incident.
#   * Files over the size limit (10 MB) — they make every clone slow for ever, because git keeps
#     them in history after deletion.
#
# What this does NOT catch: a secret pasted into an ordinary source file. That is gitleaks' job
# (run by pre-commit and CI); this only catches the files whose name gives them away.
#
# Legitimate exceptions go in LARGE_FILE_ALLOWLIST in .axelliant/standards.env, with the reason in
# the pull request that adds them.
set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/config.sh"
cd "$AX_REPO_ROOT"

failures=0
echo "Checking tracked repository hygiene..."

tracked="$(git ls-files | while IFS= read -r f; do [ -e "$f" ] && printf '%s\n' "$f"; done || true)"

# Build output and IDE state for every stack we work in.
forbidden_re='(^|/)(bin|obj|dist|out|target|build|coverage|TestResults|\.vs|\.gradle|\.next|\.nuxt|\.angular|\.svelte-kit|DerivedData|xcuserdata|node_modules|__pycache__|\.pytest_cache|\.mypy_cache|\.ruff_cache|\.dart_tool|\.terraform)(/|$)'
forbidden_re+='|\.csproj\.user$|\.suo$|\.DS_Store$|Thumbs\.db$|\.tmp$|\.py[cod]$|\.class$|\.o$|\.so$|\.dll$|\.exe$|\.apk$|\.aab$|\.ipa$|\.tfstate(\.backup)?$'
# Keep legitimately named source folders: a Gradle/Maven "build" logic module is rare but real.
keep_re='(^|/)(buildSrc|build-logic)/|(^|/)gradle/wrapper/gradle-wrapper\.jar$'

bad="$(printf '%s\n' "$tracked" | grep -E "$forbidden_re" | grep -Ev "$keep_re" || true)"
if [ -n "$bad" ]; then
    echo "FAIL tracked build output / generated files:"
    printf '%s\n' "$bad" | sed 's/^/  /'
    failures=$((failures + 1))
fi

secret_re='(^|/)\.env($|\.)|\.pem$|\.pfx$|\.p12$|\.key$|\.jks$|\.keystore$|(^|/)id_(rsa|dsa|ecdsa|ed25519)($|\.|_)|(^|/)secrets?\.(json|ya?ml|toml)$|(^|/)credentials\.json$|(^|/)google-services\.json$|(^|/)GoogleService-Info\.plist$|\.mobileprovision$|(^|/)appsettings\.[A-Za-z]*\.local\.json$'
# Templates of secret files are the documented way to describe them.
secret_ok_re='\.example$|\.sample$|\.template$|(^|/)\.env\.example$'
secrets="$(printf '%s\n' "$tracked" | grep -E "$secret_re" | grep -Ev "$secret_ok_re" || true)"
if [ -n "$secrets" ]; then
    echo "FAIL tracked secret-shaped files (rotate anything real, then remove):"
    printf '%s\n' "$secrets" | sed 's/^/  /'
    failures=$((failures + 1))
fi

echo "Checking tracked files over $((LARGE_FILE_LIMIT_BYTES / 1048576)) MB..."
while IFS= read -r file; do
    [ -f "$file" ] || continue
    size="$(wc -c < "$file" | tr -d ' ')"
    [ "$size" -gt "$LARGE_FILE_LIMIT_BYTES" ] || continue
    if [ -n "$LARGE_FILE_ALLOWLIST" ] && printf '%s' "$file" | grep -Eq "$LARGE_FILE_ALLOWLIST"; then
        echo "WARN allowlisted large file: $file ($size bytes)"
        continue
    fi
    echo "FAIL tracked file exceeds limit: $file ($size bytes)"
    failures=$((failures + 1))
done <<< "$tracked"

if [ "$failures" -gt 0 ]; then
    echo "Repository hygiene check failed."
    exit 1
fi
echo "Repository hygiene check passed."
