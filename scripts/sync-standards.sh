#!/usr/bin/env bash
# Attach the Axelliant engineering standards to a repository, or bring it up to a newer version.
#
#   scripts/sync-standards.sh [--ref <tag|branch>] [--target <dir>]   copy managed + seed files
#   scripts/sync-standards.sh --check                                 CI: managed files unmodified?
#
# Why this exists: standards copied by hand into forty repositories are forty different standards
# within a quarter. This copies the company-wide files (agent instructions, hooks, gates, the
# guardrail and labeler workflows) exactly, records their checksums in .axelliant/standards.lock,
# and lets CI fail when one was edited locally — a local edit is either a fix that belongs upstream
# for everyone, or a weakening that belongs nowhere.
#
# Files the repository legitimately owns (branch names, areas, coverage floors, reviewers) are
# "seed" files in templates/manifest.txt: copied once, never overwritten.
#
# A repository may keep its own version of a managed file when it genuinely needs one (e.g. a schema
# contract generator tied to its own migration runner): list it in STANDARDS_KEEP_LOCAL in
# .axelliant/standards.env, with the reason in a comment. It is then neither overwritten nor checked.
# The same list declines a seed file the repository already covers another way (a SAST workflow of its
# own, rulesets under other names): it is not copied in, now or on any later sync.
#
# Run from inside a consuming repository and it clones the standards at --ref (default: the latest
# tag, else main) and runs from there. Run from a checkout of the standards repository itself and it
# uses that checkout as the source.
set -euo pipefail

STANDARDS_REPO="${STANDARDS_REPO:-https://github.com/axelliantsoftwareteam/eng-wiki-standards.git}"
LOCK=".axelliant/standards.lock"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ref="" target="" mode="sync"
while [ $# -gt 0 ]; do
    case "$1" in
        --ref) ref="$2"; shift 2 ;;
        --target) target="$2"; shift 2 ;;
        --check) mode="check"; shift ;;
        -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

# Carriage returns are stripped before hashing: .gitattributes checks *.ps1/*.cmd out with CRLF on
# every platform, so the same committed file has different bytes on a Linux runner and a Mac.
sha() {
    if command -v sha256sum >/dev/null; then tr -d '\r' < "$1" | sha256sum | cut -d' ' -f1
    else tr -d '\r' < "$1" | shasum -a 256 | cut -d' ' -f1; fi
}

if [ "$mode" = "check" ]; then
    cd "$(git rev-parse --show-toplevel)"
    [ -f "$LOCK" ] || { echo "standards-sync: no $LOCK; this repository has not adopted the standards. Run scripts/sync-standards.sh."; exit 1; }
    failures=0
    while read -r checksum path; do
        case "$checksum" in ''|'#'*) continue ;; esac
        [ -n "$path" ] || continue
        if [ ! -f "$path" ]; then echo "FAIL managed file missing: $path"; failures=$((failures + 1));
        elif [ "$(sha "$path")" != "$checksum" ]; then echo "FAIL managed file edited locally: $path"; failures=$((failures + 1)); fi
    done < "$LOCK"
    if [ "$failures" -gt 0 ]; then
        echo
        echo "Managed files are the company-wide standard and are overwritten on every sync."
        echo "Revert the local edit and propose the change in $STANDARDS_REPO instead;"
        echo "repository-specific rules belong in AIAGENTS.local.md and .axelliant/standards.env."
        exit 1
    fi
    echo "standards-sync: all managed files match $(sed -n 's/^# ref: //p' "$LOCK")."
    exit 0
fi

# Resolve the source: this checkout if it is the standards repository, otherwise a fresh clone.
source_dir=""
if [ -f "$here/../templates/manifest.txt" ] && [ -z "$ref" ]; then
    source_dir="$(cd "$here/.." && pwd)"
else
    tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
    if [ -z "$ref" ]; then
        ref="$(git ls-remote --tags --sort=-v:refname "$STANDARDS_REPO" 'v*' 2>/dev/null | sed -n '1s#.*refs/tags/##p' | sed 's/\^{}//')"
        ref="${ref:-main}"
    fi
    echo "Fetching standards $ref from $STANDARDS_REPO"
    git clone -q --depth 1 --branch "$ref" "$STANDARDS_REPO" "$tmp/standards"
    source_dir="$tmp/standards"
fi

target="${target:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
target="$(cd "$target" && pwd)"
[ "$source_dir" != "$target" ] || { echo "Refusing to sync the standards repository onto itself." >&2; exit 2; }
commit="$(git -C "$source_dir" rev-parse --verify -q HEAD 2>/dev/null || echo unknown)"
ref="${ref:-$(git -C "$source_dir" describe --tags --always 2>/dev/null || echo local)}"

keep_local=" $(sed -n 's/^STANDARDS_KEEP_LOCAL=//p' "$target/.axelliant/standards.env" 2>/dev/null | tr -d "\"'" || true) "
lock_rows=() copied=0 seeded=0 kept=0 local_kept=0
while read -r kind src dest; do
    case "$kind" in ''|'#'*) continue ;; esac
    ( cd "$source_dir" && compgen -G "$src" >/dev/null ) || { echo "WARN manifest entry matches nothing: $src"; continue; }
    while IFS= read -r file; do
        out="${dest:-$file}"
        mkdir -p "$target/$(dirname "$out")"
        if [ "$kind" = "managed" ] && [ "${keep_local#* "$out" }" != "$keep_local" ]; then
            local_kept=$((local_kept + 1))
        elif [ "$kind" = "managed" ]; then
            cp "$source_dir/$file" "$target/$out"
            case "$out" in *.sh|*.py|.githooks/*) chmod +x "$target/$out" ;; esac
            lock_rows+=("$(sha "$target/$out") $out")
            copied=$((copied + 1))
        elif [ "${keep_local#* "$out" }" != "$keep_local" ]; then
            local_kept=$((local_kept + 1))
        elif [ ! -e "$target/$out" ]; then
            cp "$source_dir/$file" "$target/$out"
            seeded=$((seeded + 1))
        else
            kept=$((kept + 1))
        fi
    done < <(cd "$source_dir" && compgen -G "$src" | LC_ALL=C sort)
done < "$source_dir/templates/manifest.txt"

mkdir -p "$target/.axelliant"
{
    echo "# Written by scripts/sync-standards.sh. Do not edit: CI compares managed files against these checksums."
    echo "# ref: $ref"
    echo "# commit: $commit"
    printf '%s\n' "${lock_rows[@]}" | LC_ALL=C sort -k2
} > "$target/$LOCK"

echo "Standards $ref applied to $target: $copied managed file(s) written, $seeded seeded, $kept seed file(s) kept as the repository's own, $local_kept file(s) kept local or declined (STANDARDS_KEEP_LOCAL)."
echo "Next: review the diff, set .axelliant/standards.env for this repository, run scripts/install-git-hooks.sh,"
echo "then open a PR titled 'chore: adopt axelliant engineering standards $ref'."
