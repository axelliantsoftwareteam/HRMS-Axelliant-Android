#!/usr/bin/env python3
"""Fail when a database script creates a PostgreSQL extension nobody allow-listed.

  check-migration-extensions.py [<repo-root>]

Why this exists: managed PostgreSQL (Azure Flexible Server, AWS RDS/Aurora, Cloud SQL) rejects
CREATE EXTENSION for anything not in the server's allow-list. A migration runner treats that as
fatal, so the application does not start. It happened twice on one platform in two days: pgvector
was caught before deploy; pgcrypto took UAT down after the merge.

The allow-list is DB_ALLOWED_EXTENSIONS in .axelliant/standards.env, or — when that is empty — the
default list of the Terraform variable DB_EXTENSIONS_TERRAFORM_VAR in DB_EXTENSIONS_TERRAFORM_FILE,
which is the value actually applied to the server. The allow-list change must be applied to every
environment BEFORE the migration deploys; this check cannot apply it for you.

What it does NOT catch: an extension created by application code at runtime, or one the server
allows but at a different version than the migration expects.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402
from globs import matches_any  # noqa: E402

BUILT_IN = {'PLPGSQL'}
CREATE_EXTENSION = re.compile(
    r'\bCREATE\s+EXTENSION\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:"([^"]+)"|([A-Za-z0-9_]+))', re.IGNORECASE)
LINE_COMMENT = re.compile(r'--[^\n]*')
BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.DOTALL)
DEFAULT_LIST = re.compile(r'default\s*=\s*\[(?P<items>[^\]]*)\]')


def extensions_in_sql(sql: str) -> set[str]:
    """Returns the upper-cased extensions a SQL script creates, ignoring comments."""
    clean = LINE_COMMENT.sub('', BLOCK_COMMENT.sub('', sql))
    return {(quoted or bare).upper() for quoted, bare in CREATE_EXTENSION.findall(clean)}


def terraform_allowed(variables_tf: str, variable: str) -> set[str]:
    """Returns the default extension allow-list of a Terraform variable."""
    block = re.search(r'variable\s+"' + re.escape(variable) + r'"\s*\{(?P<body>.*?)\n\}', variables_tf, re.DOTALL)
    if block is None:
        raise ValueError(f'variable "{variable}" not found')
    default = DEFAULT_LIST.search(block.group('body'))
    if default is None:
        raise ValueError(f'{variable} has no default list')
    return {item.upper() for item in re.findall(r'"([^"]+)"', default.group('items'))}


def allowed(root: Path) -> set[str]:
    """Returns the allow-listed extensions: from the environment, else from Terraform, plus built-ins."""
    explicit = os.environ.get('DB_ALLOWED_EXTENSIONS', '').split()
    if explicit:
        return {e.upper() for e in explicit} | BUILT_IN
    tf = root / os.environ.get('DB_EXTENSIONS_TERRAFORM_FILE', 'infra/azure/variables.tf')
    variable = os.environ.get('DB_EXTENSIONS_TERRAFORM_VAR', 'postgres_allowed_extensions')
    if not tf.exists():
        return set(BUILT_IN)  # No allow-list anywhere: every extension must be declared.
    return terraform_allowed(tf.read_text(encoding='utf-8'), variable) | BUILT_IN


def scripts(root: Path) -> list[Path]:
    """Returns every SQL file matching the applied-script globs, sorted."""
    globs = os.environ.get('DB_APPLIED_GLOBS', 'database/schema.sql database/migrations/*.sql').split()
    found = []
    for path in sorted(root.rglob('*.sql')):
        relative = path.relative_to(root).as_posix()
        if matches_any(relative, globs):
            found.append(path)
    return found


def violations(root: Path) -> list[tuple[str, str]]:
    """Returns (script, extension) for every extension a script needs that is not allowed."""
    permitted = allowed(root)
    missing = []
    for script in scripts(root):
        for extension in sorted(extensions_in_sql(script.read_text(encoding='utf-8')) - permitted):
            missing.append((script.relative_to(root).as_posix(), extension))
    return missing


def main(argv: list[str]) -> int:
    """Entry point: reports extensions missing from the allow-list; returns the exit code."""
    root = Path(argv[1]) if len(argv) > 1 else Path('.')
    if os.environ.get('DB_ENGINE', 'postgres') != 'postgres':
        print('migration-extensions: DB_ENGINE is not postgres; nothing to check.')
        return 0
    try:
        missing = violations(root)
    except (OSError, ValueError) as error:
        print(f'check-migration-extensions: {error}', file=sys.stderr)
        return 2
    if not missing:
        print('Every extension the database scripts create is allow-listed.')
        return 0
    print('These extensions are created by database scripts but are not allow-listed on the server:')
    for script, extension in missing:
        print(f'  {extension}  (in {script})')
    print('Add them to DB_ALLOWED_EXTENSIONS / the Terraform allow-list and apply that to every '
          'environment BEFORE the migration deploys, or the application will not start.')
    return 1


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
