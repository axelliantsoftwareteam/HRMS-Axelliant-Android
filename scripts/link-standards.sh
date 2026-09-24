#!/usr/bin/env bash
# Link .axelliant/standards to the latest Axelliant engineering standards (the eng-wiki-standards
# wiki), so every agent and person working in this repository reads the current rules.
#
#   scripts/link-standards.sh            refresh the wiki copy and (re)create the link
#   scripts/link-standards.sh --quiet    the same, printing only problems (what the git hooks run)
#
# Why this exists: the standards live in one place, the wiki, and change there. A copy synced into
# each repository goes stale between syncs; a link to a clone that is kept at the wiki's latest
# `main` does not. Agents are told (AIAGENTS.md) to run this at the start of every session and to
# read .axelliant/standards/ before changing code; the post-checkout and post-merge hooks run it too.
#
# How: one read-only copy of the wiki per machine, at ~/.axelliant/eng-wiki-standards (override
# with AXELLIANT_STANDARDS_CACHE). The script owns that copy, so it resets it to origin/main on every
# run — never edit it; propose changes to eng-wiki-standards instead. The link is local: it is added
# to .git/info/exclude, never committed, so CI and other machines are unaffected.
#
# It never fails the caller: without network it keeps the copy it has and says so. What it does NOT
# do: guarantee an agent reads the pages. That is AIAGENTS.md's instruction and the gates' job.
set -uo pipefail

quiet=0
[ "${1:-}" = "--quiet" ] && quiet=1
say() { [ "$quiet" -eq 1 ] || printf '%s\n' "$*"; }
warn() { printf 'link-standards: %s\n' "$*" >&2; }

remote="${AXELLIANT_STANDARDS_REMOTE:-https://github.com/axelliantsoftwareteam/eng-wiki-standards.git}"
cache="${AXELLIANT_STANDARDS_CACHE:-$HOME/.axelliant/eng-wiki-standards}"
repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || { warn "run this inside a git repository."; exit 0; }

# The standards repository reads its own standards/ folder; there is nothing to link.
case "$(git -C "$repo_root" remote get-url origin 2>/dev/null)" in
    *eng-wiki-standards*) say "This is eng-wiki-standards itself: read standards/ directly."; exit 0 ;;
esac

export GIT_TERMINAL_PROMPT=0
if [ -d "$cache/.git" ]; then
    if git -C "$cache" fetch -q --depth 1 origin main 2>/dev/null; then
        git -C "$cache" reset -q --hard FETCH_HEAD
    else
        warn "could not reach the wiki; using the copy from $(git -C "$cache" log -1 --format=%cs 2>/dev/null || echo 'an earlier run')."
    fi
else
    mkdir -p "$(dirname "$cache")"
    if ! git clone -q --depth 1 --branch main "$remote" "$cache" 2>/dev/null; then
        warn "could not clone $remote; read the standards online: https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/standards"
        exit 0
    fi
fi

link="$repo_root/.axelliant/standards"
mkdir -p "$repo_root/.axelliant"
if [ -e "$link" ] && [ ! -L "$link" ]; then
    warn "$link exists and is not a link; leaving it alone."
    exit 0
fi
ln -sfn "$cache/standards" "$link" 2>/dev/null || {
    warn "could not create a symlink here (on Windows, enable Developer Mode); read $cache/standards directly."
    exit 0
}

exclude="$(git -C "$repo_root" rev-parse --git-path info/exclude)"
case "$exclude" in /*) ;; *) exclude="$repo_root/$exclude" ;; esac
mkdir -p "$(dirname "$exclude")"
grep -qxF '/.axelliant/standards' "$exclude" 2>/dev/null || printf '%s\n' '/.axelliant/standards' >> "$exclude"

say "Standards linked: .axelliant/standards -> $cache/standards (main @ $(git -C "$cache" log -1 --format='%h, %cs'))."
