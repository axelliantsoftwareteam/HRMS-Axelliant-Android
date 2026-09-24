"""Load the repository's `.axelliant/standards.env` into os.environ, for the Python guards.

The bash guards source lib/config.sh; the Python guards are also run directly (by CI workflow steps
and by people), so they must read the same file themselves — otherwise a repository's configuration
silently does not apply when a Python gate runs on its own. Same rules as config.sh: KEY=value lines,
one layer of quotes stripped, nothing evaluated, and a value already in the environment wins.
"""
from __future__ import annotations

import os
import re
import subprocess

KEY = re.compile(r'^[A-Z][A-Z0-9_]*$')


def repo_root() -> str:
    """Returns the repository root, or the current directory outside a repository."""
    result = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True)
    return result.stdout.strip() or os.getcwd()


def load(root: str | None = None) -> None:
    """Loads .axelliant/standards.env into os.environ without overriding values already set."""
    path = os.environ.get('AX_CONFIG_FILE') or os.path.join(root or repo_root(), '.axelliant', 'standards.env')
    if not os.path.exists(path):
        return
    with open(path, encoding='utf-8') as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            key, _, value = line.partition('=')
            key = key.strip()
            if not KEY.match(key):
                continue
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in '"\'':
                value = value[1:-1]
            os.environ.setdefault(key, value)


EMPTY_TREE = '4b825dc642cb6eb9a060e54bf8d69288fbee4904'  # git's well-known empty tree


def diff_range(base: str) -> list[str]:
    """Arguments for `git diff` comparing HEAD with base.

    A brand-new repository has no base yet (`HEAD~1` of the first commit, or an integration branch
    not pushed); diffing against it crashes. Then compare with the empty tree instead, so every file
    counts as added — which on a first commit is exactly true.
    """
    def exists(ref: str) -> bool:
        return subprocess.run(['git', 'rev-parse', '--verify', '-q', f'{ref}^{{commit}}'], capture_output=True).returncode == 0
    if not exists('HEAD'):
        return ['--cached', EMPTY_TREE]  # no commit at all yet: what is staged is what is new
    return [f'{base}...HEAD'] if exists(base) else [EMPTY_TREE, 'HEAD']


def added_lines(base: str, exclude_moved: bool = True) -> dict[str, set[int]]:
    """Repository path -> line numbers added (or changed) since base, for diff-scoped gates.

    Gates that scan code (exception leaks, console logging, code documentation) use this to judge only
    what a change adds: existing findings in a legacy repository are standards debt to fix when
    touched, and must not make every pull request fail — while any new violation still does.

    With exclude_moved (the default), a line whose exact text was also deleted somewhere in the same
    diff is treated as moved, not new: splitting a god file into partials, or one class per file,
    re-adds every line it moves, and a refactor must not be judged as if it wrote that code.
    """
    from collections import Counter
    diff = subprocess.run(['git', 'diff', '-U0', '--no-color', *diff_range(base)],
                          capture_output=True, text=True, check=True).stdout
    result: dict[str, set[int]] = {}
    added_text: list[tuple[str, int, str]] = []
    removed = Counter()
    current, number = None, 0
    for line in diff.splitlines():
        if line.startswith('+++ '):
            current = line[6:] if line.startswith('+++ b/') else None
            if current:
                result.setdefault(current, set())
        elif line.startswith('--- '):
            continue
        elif line.startswith('@@'):
            match = re.search(r'\+(\d+)(?:,(\d+))?', line)
            number = int(match.group(1))
        elif line.startswith('-'):
            removed[line[1:].strip()] += 1
        elif line.startswith('+') and current:
            added_text.append((current, number, line[1:].strip()))
            number += 1
    for path, lineno, text in added_text:
        if exclude_moved and text and removed[text] > 0:
            removed[text] -= 1
            continue
        result[path].add(lineno)
    return result
