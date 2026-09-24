#!/usr/bin/env bash
# Apply the repository's GitHub configuration: every ruleset in .github/rulesets/ and the repository
# settings. Idempotent — safe to re-run; an existing ruleset with the same name is updated in place.
#
# Why this exists: GitHub does not read .github/settings.yml or .github/rulesets/*.json by itself.
# Those files are the reviewed source of truth; this script is what makes them true. Without it the
# committed configuration and the live configuration drift, and nobody notices until a force-push
# lands on main.
#
# Usage: scripts/apply-github-ruleset.sh <owner/repo> [--dry-run]
# Needs: gh, authenticated as a repository administrator (gh auth login).
set -euo pipefail

repo="${1:-}"
dry="${2:-}"
[ -n "$repo" ] || { echo "usage: $0 <owner/repo> [--dry-run]" >&2; exit 2; }
command -v gh >/dev/null || { echo "gh (GitHub CLI) is required." >&2; exit 1; }
cd "$(git rev-parse --show-toplevel)"

say() { printf '%s\n' "$*"; }
api() { if [ "$dry" = "--dry-run" ]; then say "DRY-RUN gh api $*"; else gh api "$@" >/dev/null; fi; }

say "Repository settings for $repo"
api -X PATCH "repos/$repo" \
    -F delete_branch_on_merge=true -F allow_auto_merge=true -F allow_update_branch=true \
    -F allow_merge_commit=true -F allow_squash_merge=true -F allow_rebase_merge=false \
    -f squash_merge_commit_title=PR_TITLE -f squash_merge_commit_message=PR_BODY \
    -F has_wiki=false

existing="$(gh api "repos/$repo/rulesets" --jq '.[] | "\(.id)\t\(.name)"' 2>/dev/null || true)"
shopt -s nullglob
for file in .github/rulesets/*.json; do
    name="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["name"])' "$file")"
    payload="$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); [d.pop(k,None) for k in ("id","source","source_type","_links","created_at","updated_at","node_id","current_user_can_bypass")]; print(json.dumps(d))' "$file")"
    id="$(printf '%s\n' "$existing" | awk -F'\t' -v n="$name" '$2==n {print $1}')"
    if [ -n "$id" ]; then
        say "Updating ruleset '$name' ($id) from $file"
        printf '%s' "$payload" | api -X PUT "repos/$repo/rulesets/$id" --input -
    else
        say "Creating ruleset '$name' from $file"
        printf '%s' "$payload" | api -X POST "repos/$repo/rulesets" --input -
    fi
done
say "Done. Verify under Settings -> Rules -> Rulesets."
