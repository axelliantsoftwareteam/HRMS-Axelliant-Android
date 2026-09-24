#!/usr/bin/env bash
# Point git at the repository's hooks and commit template. Run once per clone.
#
# Why this exists: git never installs hooks on clone — deliberately, since a hook is code that runs
# on your machine. So every clone needs this once, or the hooks silently do nothing and the first
# anyone hears of a bad commit message is a red CI run.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "Run this inside a git repository." >&2; exit 1; }
cd "$repo_root"

for hook in .githooks/pre-commit .githooks/commit-msg .githooks/pre-push .githooks/post-checkout .githooks/post-merge; do
    [ -f "$hook" ] || { echo "Expected hook '$hook' not found." >&2; exit 1; }
    chmod +x "$hook"
done
chmod +x scripts/*.sh scripts/*.py 2>/dev/null || true
[ -f .axelliant/pre-commit.sh ] && chmod +x .axelliant/pre-commit.sh
[ -f .axelliant/local-checks.sh ] && chmod +x .axelliant/local-checks.sh

git config core.hooksPath .githooks
[ -f .gitmessage ] && git config commit.template .gitmessage

echo "Hooks installed: commit-msg, pre-commit, pre-push, post-checkout, post-merge (core.hooksPath=.githooks)."
# Link .axelliant/standards to the latest standards; the post-checkout and post-merge hooks keep it current.
[ -x scripts/link-standards.sh ] && scripts/link-standards.sh
command -v gitleaks >/dev/null 2>&1 || echo "Recommended: install gitleaks so secrets are caught before commit (brew install gitleaks / scoop install gitleaks)."
command -v gh >/dev/null 2>&1 || echo "Recommended: install the GitHub CLI so stacked branches are detected automatically."
