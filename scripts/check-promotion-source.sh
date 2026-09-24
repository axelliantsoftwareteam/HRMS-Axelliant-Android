#!/usr/bin/env bash
# The release branch accepts code only from the integration branch.
#
# Why this exists: day-to-day work merges into `uat` (INTEGRATION_BRANCH) and is promoted into
# `main` (RELEASE_BRANCH) by a single pull request. Branch rulesets can require reviews and status
# checks on the release branch, but they cannot express "only this branch may be the source" — so
# without this, nothing stops a feature branch targeting `main` directly and shipping code that
# never ran in UAT. That gap is what this closes.
#
# Emergency path: the ruleset's bypass actors can merge without this check. Keeping the escape
# hatch in the ruleset rather than in this script keeps it auditable, and independent of how
# somebody named their branch.
#
# Usage:  scripts/check-promotion-source.sh <base-branch> <head-branch>
set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/config.sh"

base="${1:-}"
head="${2:-}"
if [ -z "$base" ] || [ -z "$head" ]; then
    echo "usage: $0 <base-branch> <head-branch>" >&2
    exit 2
fi

# Only pull requests into the release branch are restricted. `master` is treated as a release
# branch too, so repositories that have not renamed it are still protected.
if [ "$base" != "$RELEASE_BRANCH" ] && [ "$base" != "main" ] && [ "$base" != "master" ]; then
    echo "promotion-source: base '$base' is not a release branch; nothing to enforce."
    exit 0
fi

# A repository adopting the standards may not have its integration branch yet. The rule cannot be
# enforced until it exists, so say so loudly and pass; once the branch exists, it is enforced.
if git remote get-url origin >/dev/null 2>&1 \
   && [ -z "$(git ls-remote --heads origin "$INTEGRATION_BRANCH" 2>/dev/null)" ]; then
    echo "promotion-source: WARNING '$INTEGRATION_BRANCH' does not exist on origin yet, so '$base' cannot be"
    echo "promotion-source: restricted to it. Create it from '$base' to activate the promotion model."
    exit 0
fi

if [ "$head" = "$INTEGRATION_BRANCH" ]; then
    echo "promotion-source: $INTEGRATION_BRANCH -> $base. OK."
    exit 0
fi

cat >&2 <<MSG

Pull request rejected: '$head' -> '$base'

  '$base' is promoted from '$INTEGRATION_BRANCH' only. A branch cannot merge into it
  directly, because that would ship code that never ran in UAT.

    1. Retarget this pull request to '$INTEGRATION_BRANCH' (Edit, next to the title).
       Nothing needs to be re-pushed.
    2. Once merged and verified, open one promotion PR from '$INTEGRATION_BRANCH' into '$base'.

  Urgent production fix? An administrator can bypass the ruleset. Ask rather than
  renaming the branch - renaming will not satisfy this check.

MSG
exit 1
