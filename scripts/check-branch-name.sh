#!/usr/bin/env bash
# A branch name says what the branch is for.
#
# Convention:  <type>/<short-description>
#              <type>/<TICKET-123>-<short-description>
#
#   type          feature fix bugfix hotfix chore docs refactor test perf ci build release infra
#   TICKET-123    optional, uppercase project key + number (e.g. AXE-412)
#   description   lowercase words separated by - . or _
#
# Why this exists: names like `bug-verification`, `test2` and `main_ali_v1` accumulate until
# nobody knows which branches are live, which are abandoned, and which hold someone's only copy
# of a week's work. A type prefix makes the branch list sortable and lets the labeler, the
# cleanup job and the release notes work from the name alone.
#
# Usage:  scripts/check-branch-name.sh [branch-name]     (defaults to the current branch)
# Escape hatch for a deliberate exception:  SKIP_BRANCH_NAME_CHECK=1 git push ...
set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/config.sh"

if [ "${SKIP_BRANCH_NAME_CHECK:-}" = "1" ]; then
    echo "branch-name: check skipped by SKIP_BRANCH_NAME_CHECK=1."
    exit 0
fi

# symbolic-ref names the branch even before its first commit; rev-parse prints "HEAD" and fails there.
branch="${1:-$(git symbolic-ref --short -q HEAD 2>/dev/null || echo HEAD)}"
[ "$branch" = "HEAD" ] && exit 0

for exempt in $EXEMPT_BRANCHES "$INTEGRATION_BRANCH" "$RELEASE_BRANCH"; do
    [ "$branch" = "$exempt" ] && exit 0
done

TYPES='feature|fix|bugfix|hotfix|chore|docs|refactor|test|perf|ci|build|release|infra'
PATTERN="^(${TYPES})/([A-Z][A-Z0-9]*-[0-9]+-)?[a-z0-9]+([._-][a-z0-9]+)*$"

if printf '%s' "$branch" | grep -Eq "$PATTERN"; then
    exit 0
fi

cat >&2 <<MSG

Branch name rejected: '$branch'

Expected:  <type>/<short-description>
       or  <type>/<TICKET-123>-<short-description>

  type         ${TYPES//|/, }
  TICKET-123   optional (e.g. ${TICKET_PREFIX}-412)
  description  lowercase, words separated by - . or _

Examples:
  feature/warehouse-picking
  fix/chat-product-search
  chore/${TICKET_PREFIX}-412-bump-deps

Rename your branch:        git branch -m <new-name>
Deliberate exception:      SKIP_BRANCH_NAME_CHECK=1 git push ...

MSG
exit 1
