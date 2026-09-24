#!/usr/bin/env python3
"""Fail when a database script that is already on the base branch has been changed.

  check-applied-scripts.py <base-ref>

Why this exists: migration runners (ours, Flyway, EF Core, Alembic, Liquibase, Django) record
every script they apply — by checksum, or by name — and either refuse to start when one changes or,
worse, silently never re-run it. Either way the mistake is reported at deploy time, in the
environment, as an outage or as drift. On a previous platform an edit to the baseline schema passed
every CI check, merged, and kept UAT from starting until the file was restored byte for byte.

Anything on the base branch has to be assumed applied somewhere, so this compares those files with
the base branch and rejects any that were modified, deleted or renamed. New scripts are free to
change until they merge. To change what an applied script did, add a new migration.

Configuration (.axelliant/standards.env):
  DB_APPLIED_GLOBS    space-separated globs of applied scripts (default: database/schema.sql
                      database/migrations/*.sql). Examples: `db/migration/V*.sql` (Flyway),
                      `alembic/versions/*.py`, `src/**/Migrations/*.cs`, `prisma/migrations/*/migration.sql`
  DB_APPLIED_EXCLUDE  globs that match the above but legitimately change on every migration
                      (default: EF Core's `*ModelSnapshot.cs`)

What it does NOT catch: a change made directly in a live database. That is check-schema-drift's job.
"""
from __future__ import annotations

import os
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402
from globs import matches_any  # noqa: E402

DEFAULT_GLOBS = 'database/schema.sql database/migrations/*.sql'
DEFAULT_EXCLUDE = '**/*ModelSnapshot.cs'


def settings() -> tuple[list[str], list[str]]:
    """Returns (applied-script globs, excluded globs) from the environment."""
    return (os.environ.get('DB_APPLIED_GLOBS', DEFAULT_GLOBS).split(),
            os.environ.get('DB_APPLIED_EXCLUDE', DEFAULT_EXCLUDE).split())


def changed_paths(base_ref: str, cwd: str | None = None) -> list[tuple[str, str]]:
    """(status, path) for every file that differs from base_ref, with rename detection off.

    With renames off a rename shows as a deletion of the old path, which is what matters: runners
    look scripts up by name, so a renamed script is a missing script and a new one to apply.
    """
    result = subprocess.run(['git', 'diff', '--name-status', '--no-renames', *ax_config.diff_range(base_ref)],
                            cwd=cwd, check=True, capture_output=True, text=True)
    rows = []
    for line in result.stdout.splitlines():
        status, _, path = line.partition('\t')
        if path:
            rows.append((status, path))
    return rows


def violations(rows: list[tuple[str, str]], globs: list[str], exclude: list[str]) -> list[str]:
    """Returns a description of every applied script the diff modifies or deletes."""
    problems = []
    for status, path in rows:
        if not matches_any(path, globs) or matches_any(path, exclude):
            continue
        if status.startswith('M'):
            problems.append(f'modified: {path}')
        elif status.startswith('D'):
            problems.append(f'deleted:  {path}')
    return problems


def main(argv: list[str]) -> int:
    """Entry point: compares applied scripts with the base ref and reports changes; returns the exit code."""
    if len(argv) != 2:
        print('usage: check-applied-scripts.py <base-ref>', file=sys.stderr)
        return 2
    globs, exclude = settings()
    problems = violations(changed_paths(argv[1]), globs, exclude)
    if not problems:
        print('No applied database script was changed.')
        return 0
    print('Applied database scripts were changed. Every environment that already ran them will refuse')
    print('to start, or will silently never apply the change:')
    for problem in problems:
        print(f'  {problem}')
    print('\nRestore them exactly, and put the change in a new migration instead.')
    return 1


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
