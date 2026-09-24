#!/usr/bin/env bash
# No god files: a changed file may not cross its line budget, and an oversized one may not grow.
#
# Why this exists: a 3,000-line service is where every change collides, every review skims, and
# every merge conflicts. Nobody plans one; it arrives a hundred lines at a time. This makes each
# of those hundred-line steps visible at the moment it happens.
#
# Modes:
#   scripts/check-file-length-budget.sh <base-ref>   diff mode (CI): files changed since base-ref
#   scripts/check-file-length-budget.sh --staged     staged mode (pre-commit): the staged blobs
#
# A file already over budget on the base branch is "legacy": it may shrink or stay the same,
# never grow (a whitespace-only growth is warned, not failed). A new file, or one that crosses
# its budget in this change, fails.
#
# Budgets: the defaults below, overridden by `.axelliant/file-length-budgets` — one
# "<glob> <limit>" per line, first match wins, limit 0 means no budget. Use it to tighten a
# budget, or to give a genuinely aggregate file type (generated clients, entity model files) a
# higher one. Never to rescue a single file: that is what the legacy ratchet is for.
#
# What it does NOT catch: a file that is long because it is one enormous function. That is the
# method-length gate's job.
set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/config.sh"
cd "$AX_REPO_ROOT"

MODE="diff"
BASE_REF="${1:-}"

if [[ "${1:-}" == "--staged" ]]; then
  MODE="staged"
  BASE_REF=""
fi

normalize_base_ref() {
  local candidate="$1"
  if [[ -z "$candidate" || "$candidate" =~ ^0+$ ]]; then
    if git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
      git rev-parse HEAD~1
    else
      echo ""
    fi
    return
  fi

  if git rev-parse --verify "$candidate^{commit}" >/dev/null 2>&1; then
    echo "$candidate"
    return
  fi

  if git rev-parse --verify "origin/$candidate^{commit}" >/dev/null 2>&1; then
    echo "origin/$candidate"
    return
  fi

  echo ""
}

get_changed_files() {
  # Emits name-status rows ("M<TAB>path", "R###<TAB>old<TAB>new", ...) so renames keep their base path for the ratchet.
  if [[ "$MODE" == "staged" ]]; then
    git diff --cached --name-status --diff-filter=ACMR --find-renames
    return
  fi

  local normalized
  normalized="$(normalize_base_ref "$BASE_REF")"
  if [[ -n "$normalized" ]]; then
    git diff --name-status --diff-filter=ACMR --find-renames "$normalized"...HEAD
  else
    git diff --name-status --diff-filter=ACMR --find-renames HEAD~1...HEAD 2>/dev/null || git ls-files | sed $'s/^/M\t/'
  fi
}

BUDGET_OVERRIDES=()
if [[ -f .axelliant/file-length-budgets ]]; then
  while read -r glob limit _; do
    limit="${limit%$'\r'}"  # a CRLF checkout on Windows leaves "150\r", which is not a number
    [[ -z "${glob:-}" || "$glob" == \#* ]] && continue
    BUDGET_OVERRIDES+=("$glob $limit")
  done < .axelliant/file-length-budgets
fi

file_max_lines() {
  local file="$1" entry glob
  for entry in "${BUDGET_OVERRIDES[@]+"${BUDGET_OVERRIDES[@]}"}"; do
    glob="${entry% *}"
    # shellcheck disable=SC2053  # the glob is deliberately unquoted: it is a pattern
    if [[ "$file" == $glob ]]; then echo "${entry##* }"; return; fi
  done

  case "$file" in
    # Lockfiles, generated contracts, snapshots and ORM-generated migrations are written by tools, not people.
    *package-lock.json|*yarn.lock|*pnpm-lock.yaml|*poetry.lock|*uv.lock|*Podfile.lock|*.expected.sql|*.snap|*/openapi.json|*/openapi.yaml|*/Migrations/*.cs|*.Designer.cs|*ModelSnapshot.cs|*.g.cs|*.g.dart|*.freezed.dart)
      echo 0 ;;
    *Controller.cs|*/controllers/*.py|*/routes/*.py|*/routers/*.py|*Controller.java|*Controller.kt|*.controller.ts|*/api/*.ts|*/app/api/*.tsx)
      echo 250 ;;
    *Tests.cs|*Test.cs|*/test_*.py|*_test.py|*.spec.ts|*.test.ts|*.spec.tsx|*.test.tsx|*.test.js|*.spec.js|*Test.java|*Test.kt|*Tests.swift|*_test.go|*_test.dart)
      echo 600 ;;
    *.component.html|*.html|*.vue|*.svelte)                           echo 300 ;;
    *.cs|*.py|*.java|*.kt|*.swift|*.go|*.dart|*.rb|*.php)              echo 500 ;;
    *.ts|*.tsx|*.js|*.jsx|*.mjs|*.cjs)                                  echo 400 ;;
    *.css|*.scss|*.less)                                                echo 400 ;;
    *.csproj|*.gradle|*.gradle.kts|pom.xml|*/pom.xml)                   echo 250 ;;
    *.sh|*.ps1)                                                         echo 250 ;;
    *.yml|*.yaml)                                                       echo 300 ;;
    *.json)                                                             echo 700 ;;
    *.sql)                                                              echo 800 ;;
    *.tf)                                                               echo 400 ;;
    Dockerfile|*/Dockerfile|*.Dockerfile)                               echo 120 ;;
    *)                                                                  echo 0 ;;
  esac
}

line_count_worktree() {
  local file="$1"
  # In staged mode, evaluate the staged blob (index) rather than the worktree file.
  if [[ "$MODE" == "staged" ]]; then
    if git cat-file -e ":$file" 2>/dev/null; then
      git show ":$file" | wc -l | tr -d ' '
      return
    fi
    if [[ -e "$file" ]]; then
      wc -l < "$file" | tr -d ' '
      return
    fi
    echo "0"
    return
  fi
  wc -l < "$file" | tr -d ' '
}

line_count_at_ref() {
  local ref="$1"
  local file="$2"

  if [[ -z "$ref" ]]; then
    echo ""
    return
  fi

  if ! git cat-file -e "${ref}:${file}" 2>/dev/null; then
    echo ""
    return
  fi

  git show "${ref}:${file}" | wc -l | tr -d ' '
}

has_only_whitespace_diff_against_base() {
  local ref="$1" base_file="$2" file="$3" base_commit old new

  [[ -n "$ref" ]] || return 1
  if [[ "$MODE" == "diff" ]]; then
    base_commit="$(git merge-base "$ref" HEAD 2>/dev/null)" || return 1
    new="$(git show "HEAD:$file" 2>/dev/null)" || return 1
  else
    base_commit="$ref"
    new="$(git show ":$file" 2>/dev/null)" || return 1
  fi
  old="$(git show "$base_commit:$base_file" 2>/dev/null)" || return 1

  # Growth is formatting-only when the content is identical once every space, tab and line break is
  # removed. `git diff -w` ignores spaces within a line but not a line a formatter split in two, so a
  # Prettier reflow of an already oversized file used to fail as real growth.
  [[ "$(printf '%s' "$old" | tr -d ' \t\r\n')" == "$(printf '%s' "$new" | tr -d ' \t\r\n')" ]]
}

BASE_FOR_COMPARE=""
if [[ "$MODE" == "diff" ]]; then
  BASE_FOR_COMPARE="$(normalize_base_ref "$BASE_REF")"
elif git rev-parse --verify -q HEAD >/dev/null; then
  # Staged mode compares the staged blob with the last commit; without a base every modified
  # oversized file looked new and failed, instead of being allowed to shrink.
  BASE_FOR_COMPARE="HEAD"
fi

CHANGED_FILES=()
CHANGED_BASE_FILES=()
while IFS=$'\t' read -r status p1 p2; do
  [[ -n "$status" ]] || continue
  case "$status" in
    # Pure rename/copy: identical content, no growth; ratchet state carries over.
    R100|C100) continue ;;
    R*|C*) CHANGED_FILES+=("$p2"); CHANGED_BASE_FILES+=("$p1") ;;
    *) CHANGED_FILES+=("$p1"); CHANGED_BASE_FILES+=("$p1") ;;
  esac
done < <(get_changed_files)

if [[ "${#CHANGED_FILES[@]}" -eq 0 ]]; then
  echo "No changed files to validate for file-length budgets."
  exit 0
fi

failures=0
legacy_notes=0

for idx in "${!CHANGED_FILES[@]}"; do
  file="${CHANGED_FILES[$idx]}"
  base_file="${CHANGED_BASE_FILES[$idx]}"
  [[ -f "$file" ]] || continue

  max_lines="$(file_max_lines "$file")"
  [[ "$max_lines" -gt 0 ]] || continue

  current_lines="$(line_count_worktree "$file")"
  previous_lines="$(line_count_at_ref "$BASE_FOR_COMPARE" "$base_file")"

  if [[ "$current_lines" -le "$max_lines" ]]; then
    continue
  fi

  if [[ -z "$previous_lines" ]]; then
    echo "FAIL new file exceeds budget: $file (${current_lines} lines > ${max_lines})"
    failures=$((failures + 1))
    continue
  fi

  if [[ "$previous_lines" -le "$max_lines" ]]; then
    echo "FAIL file crossed budget: $file (${previous_lines} -> ${current_lines}, limit ${max_lines})"
    failures=$((failures + 1))
    continue
  fi

  if [[ "$current_lines" -gt "$previous_lines" ]]; then
    if has_only_whitespace_diff_against_base "$BASE_FOR_COMPARE" "$base_file" "$file"; then
      echo "WARN legacy oversized file grew only due to whitespace/formatting changes: $file (${previous_lines} -> ${current_lines}, limit ${max_lines})"
      legacy_notes=$((legacy_notes + 1))
      continue
    fi
    echo "FAIL legacy oversized file grew: $file (${previous_lines} -> ${current_lines}, limit ${max_lines})"
    failures=$((failures + 1))
    continue
  fi

  echo "OK ratcheting legacy oversized file downward: $file (${previous_lines} -> ${current_lines}, limit ${max_lines})"
  legacy_notes=$((legacy_notes + 1))
done

if [[ "$legacy_notes" -gt 0 ]]; then
  echo "Ratcheting notes: $legacy_notes legacy oversized files reduced or unchanged."
fi

if [[ "$failures" -gt 0 ]]; then
  echo "File-length budget check failed (${failures} violation(s)). Split large files/classes/services before merge."
  exit 1
fi

echo "File-length budget check passed."
