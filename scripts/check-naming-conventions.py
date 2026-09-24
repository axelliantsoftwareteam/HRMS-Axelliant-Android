#!/usr/bin/env python3
"""New files, and new database objects, are named the way their ecosystem names them.

  check-naming-conventions.py <base-ref>     files added or renamed since base-ref (CI, pre-push)
  check-naming-conventions.py --staged       files staged for commit (pre-commit)
  check-naming-conventions.py --all          every tracked file (an audit; not a gate)

Why this exists: a codebase where half the files are `UserService.cs` and half `user_service.cs`,
or half the tables are `Customer` and half `customer_address`, makes every search a guess and every
ORM mapping a special case. On PostgreSQL it is worse than untidy: an unquoted identifier folds to
lower case, so a table created as "Customer" can only ever be reached quoted, and one quoted and one
unquoted reference to the same name are two different tables.

Only new and renamed files are checked, so adopting this never demands renaming the world. Existing
misnamed files are standards debt: fix them when you touch them (see AIAGENTS.md).

Identifier casing INSIDE code (camelCase locals, PascalCase types, UPPER_SNAKE constants) is the job
of each language's linter, configured by the stack templates: ruff's pep8-naming (N), ESLint
@typescript-eslint/naming-convention, .editorconfig dotnet_naming_rule, ktlint/detekt, SwiftLint
identifier_name, `dart analyze`, golint/revive. This script covers what those tools cannot see:
file names and SQL.

Configuration: DB_APPLIED_GLOBS (which .sql files are migrations), DB_NAMING=snake|pascal
(pascal only for existing SQL Server estates; every new database is snake_case).
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402
from globs import matches_any  # noqa: E402

PASCAL = r'[A-Z][A-Za-z0-9]*'
SNAKE = r'[a-z][a-z0-9]*(?:_[a-z0-9]+)*'
KEBAB = r'[a-z][a-z0-9]*(?:-[a-z0-9]+)*'
CAMEL = r'[a-z][a-zA-Z0-9]*'

# (extension, compiled rule for the file NAME, human description)
RULES = [
    ('.cs',    rf'^(?:{PASCAL}(?:\.{PASCAL})*|\d{{8,14}}_{PASCAL})(?:\.g|\.Designer)?\.cs$', 'PascalCase.cs'),
    ('.py',    rf'^(?:{SNAKE}|__init__|__main__|_{SNAKE}|conftest)\.pyi?$', 'snake_case.py'),
    ('.java',  rf'^{PASCAL}\.java$', 'PascalCase.java'),
    ('.kt',    rf'^{PASCAL}\.kt$', 'PascalCase.kt'),
    ('.kts',   rf'^(?:{CAMEL}|{KEBAB})(?:\.gradle)?\.kts$', 'settings.gradle.kts / build.gradle.kts'),
    ('.swift', rf'^{PASCAL}(?:\+{PASCAL})?\.swift$', 'PascalCase.swift or Type+Extension.swift'),
    ('.go',    rf'^{SNAKE}(?:_test)?\.go$', 'snake_case.go'),
    ('.dart',  rf'^{SNAKE}\.dart$', 'snake_case.dart'),
    ('.ts',    rf'^(?:{KEBAB}|{CAMEL}|\[{{1,2}}\.{{0,3}}{CAMEL}\]{{1,2}})(?:\.{KEBAB})*\.ts$', 'kebab-case.ts (Angular: name.kind.ts), camelCase.ts for hooks/utilities'),
    ('.tsx',   rf'^(?:{PASCAL}|{KEBAB}|{CAMEL}|\[{{1,2}}\.{{0,3}}{CAMEL}\]{{1,2}})(?:\.{KEBAB})*\.tsx$', 'PascalCase.tsx for components, kebab-case.tsx for routes'),
    ('.js',    rf'^(?:{KEBAB}|{CAMEL})(?:\.{KEBAB})*\.js$', 'kebab-case.js'),
    ('.jsx',   rf'^(?:{PASCAL}|{KEBAB})(?:\.{KEBAB})*\.jsx$', 'PascalCase.jsx'),
    ('.mjs',   rf'^(?:{KEBAB}|{CAMEL})(?:\.{KEBAB})*\.mjs$', 'kebab-case.mjs'),
    ('.scss',  rf'^_?{KEBAB}(?:\.{KEBAB})*\.scss$', 'kebab-case.scss'),
    ('.sh',    rf'^{KEBAB}\.sh$', 'kebab-case.sh'),
    ('.ps1',   rf'^(?:{KEBAB}|{PASCAL}(?:-{PASCAL})*)\.ps1$', 'kebab-case.ps1 or Verb-Noun.ps1'),
    ('.tf',    rf'^{SNAKE}\.tf$', 'snake_case.tf'),
    ('.yml',   rf'^(?:{KEBAB}|{SNAKE}|\.[\w.-]+)(?:\.{KEBAB})*\.yml$', 'kebab-case.yml'),
    ('.yaml',  rf'^(?:{KEBAB}|{SNAKE}|\.[\w.-]+)(?:\.{KEBAB})*\.yaml$', 'kebab-case.yaml'),
]
COMPILED = [(ext, re.compile(rule), text) for ext, rule, text in RULES]

# Names fixed by a tool, a platform or a convention older than ours.
FIXED_NAMES = re.compile(
    r'^(Dockerfile(\.[\w-]+)?|[\w-]+\.Dockerfile|Makefile|Jenkinsfile|Procfile|Gemfile|Podfile|Fastfile|Appfile|Matchfile|'
    r'README(\.[\w-]+)?\.md|CHANGELOG\.md|CONTRIBUTING\.md|CODE_OF_CONDUCT\.md|LICENSE(\.\w+)?|SECURITY\.md|'
    r'AIAGENTS(\.local)?\.md|CLAUDE\.md|GEMINI\.md|AGENTS\.md|CODEOWNERS|'
    r'next-env\.d\.ts|vite-env\.d\.ts|global\.d\.ts|.*\.d\.ts|.*\.config\.(ts|js|mjs|cjs)|'
    r'AssemblyInfo\.cs|GlobalUsings\.cs|Program\.cs|Startup\.cs|'
    r'Package\.swift|AppDelegate\.swift|SceneDelegate\.swift|build\.gradle(\.kts)?|settings\.gradle(\.kts)?|gradlew|'
    r'docker-compose(\.[\w-]+)?\.ya?ml|compose(\.[\w-]+)?\.ya?ml)$')
IGNORED_DIRS = re.compile(r'(^|/)(node_modules|vendor|dist|build|bin|obj|\.git|Pods|DerivedData|\.gradle|'
                          r'generated|__generated__|migrations/versions)/')
ENTRY_POINT_DIR = re.compile(r'(^|/)(scripts|bin|tools)/')
MIGRATION_NAME = re.compile(r'^(?:\d{12,14}_[a-z0-9]+(?:_[a-z0-9]+)*|V\d+(?:_\d+)*__[a-z0-9]+(?:_[a-z0-9]+)*|R__[a-z0-9_]+|schema)\.sql$')

SQL_COMMENT = re.compile(r'--[^\n]*|/\*.*?\*/', re.DOTALL)
CREATE_TABLE = re.compile(r'\bCREATE\s+(?:TEMP(?:ORARY)?\s+)?TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([\w."\[\]`]+)\s*\((.*?)\)\s*(?:[A-Z]|;|$)', re.I | re.S)
ADD_COLUMN = re.compile(r'\bADD\s+(?:COLUMN\s+)?(?:IF\s+NOT\s+EXISTS\s+)?([\w"\[\]`]+)\s+\w', re.I)
INDEX = re.compile(r'\bCREATE\s+(UNIQUE\s+)?(?:CLUSTERED\s+|NONCLUSTERED\s+)?INDEX\s+(?:CONCURRENTLY\s+)?(?:IF\s+NOT\s+EXISTS\s+)?([\w"\[\]`]+)', re.I)
CONSTRAINT = re.compile(r'\bCONSTRAINT\s+([\w"\[\]`]+)\s+(PRIMARY\s+KEY|FOREIGN\s+KEY|UNIQUE|CHECK|DEFAULT|REFERENCES)', re.I)
CONSTRAINT_PREFIX = {'PRIMARY': ('pk_', '_pkey'), 'FOREIGN': ('fk_', '_fkey'), 'REFERENCES': ('fk_', '_fkey'),
                     'UNIQUE': ('uq_', 'ux_', '_key', '_uq'), 'CHECK': ('ck_', '_check'), 'DEFAULT': ('df_',)}
NOT_COLUMNS = {'CONSTRAINT', 'PRIMARY', 'FOREIGN', 'UNIQUE', 'CHECK', 'INDEX', 'KEY', 'EXCLUDE', 'LIKE', 'PERIOD'}


def git_lines(*args: str) -> list[str]:
    """Runs git with the given arguments and returns its output lines."""
    return subprocess.run(['git', *args], check=True, capture_output=True, text=True).stdout.splitlines()


def candidate_files(argv: list[str]) -> list[str]:
    """Returns the files to check: added/renamed since a base ref, staged, or all."""
    if argv == ['--all']:
        return git_lines('ls-files')
    if argv == ['--staged']:
        return git_lines('diff', '--cached', '--name-only', '--diff-filter=AR')
    if len(argv) == 1:
        return git_lines('diff', '--name-only', '--diff-filter=AR', *ax_config.diff_range(argv[0]))
    raise SystemExit(__doc__)


def bare(identifier: str) -> str:
    """Returns an SQL identifier without schema prefix or quoting."""
    return identifier.split('.')[-1].strip('"[]`')


def check_identifier(kind: str, raw: str, naming: str) -> str | None:
    """Returns a problem description when an SQL identifier breaks the naming rule, else None."""
    name = bare(raw)
    if naming == 'pascal':
        ok = re.fullmatch(r'[A-Z][A-Za-z0-9]*(?:_[A-Za-z0-9]+)*', name) or re.fullmatch(SNAKE, name)
        return None if ok else f'{kind} {name!r} is neither PascalCase nor snake_case'
    if '"' in raw and name != name.lower():
        return f'{kind} {raw} is quoted mixed case; PostgreSQL will only ever find it quoted. Use snake_case'
    return None if re.fullmatch(SNAKE, name) else f'{kind} {name!r} is not snake_case'


def check_sql(path: str, naming: str) -> list[str]:
    """Returns naming problems in a migration: tables, columns, indexes and constraints."""
    with open(path, encoding='utf-8', errors='replace') as handle:
        sql = SQL_COMMENT.sub('', handle.read())
    problems = []
    for table, body in CREATE_TABLE.findall(sql):
        problems.append(check_identifier('table', table, naming))
        depth, column_lines, current = 0, [], ''
        for char in body:  # split the column list on top-level commas only
            depth += (char == '(') - (char == ')')
            if char == ',' and depth == 0:
                column_lines.append(current)
                current = ''
            else:
                current += char
        column_lines.append(current)
        for definition in column_lines:
            first = definition.strip().split()[:1]
            if first and first[0].upper() not in NOT_COLUMNS:
                problems.append(check_identifier('column', first[0], naming))
    for column in ADD_COLUMN.findall(sql):
        if column.upper() not in NOT_COLUMNS:
            problems.append(check_identifier('column', column, naming))
    for unique, index in INDEX.findall(sql):
        name = bare(index)
        problems.append(check_identifier('index', index, naming))
        allowed = ('ux_', 'uq_', '_key', '_uq', '_idx') if unique else ('ix_', 'idx_', '_idx')
        if naming == 'snake' and not (name.startswith(allowed) or name.endswith(allowed)):
            problems.append(f"index {name!r} should be named {'ux_' if unique else 'ix_'}<table>_<columns>")
    for name, kind in CONSTRAINT.findall(sql):
        allowed = CONSTRAINT_PREFIX[kind.split()[0].upper()]
        plain = bare(name)
        if naming == 'snake' and not (plain.startswith(allowed) or plain.endswith(allowed)):
            problems.append(f'constraint {plain!r} should start with {allowed[0]} ({kind.upper()})')
    return [f'{path}: {p}' for p in problems if p]


def main(argv: list[str]) -> int:
    """Entry point: checks file names and new migrations; returns the exit code."""
    root = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True).stdout.strip()
    if root:
        os.chdir(root)
    files = [f for f in candidate_files(argv[1:]) if os.path.exists(f) and not IGNORED_DIRS.search(f)]
    migrations = os.environ.get('DB_APPLIED_GLOBS', 'database/schema.sql database/migrations/*.sql').split()
    naming = os.environ.get('DB_NAMING', 'snake')
    problems = []
    for path in files:
        name = os.path.basename(path)
        if FIXED_NAMES.match(name):
            continue
        if path.endswith('.sql') and matches_any(path, migrations):
            if not MIGRATION_NAME.match(name):
                problems.append(f'{path}: migration must be named <yyyymmddhhmm[ss]>_<snake_description>.sql '
                                f'(or Flyway V<n>__<snake_description>.sql) so it sorts in application order')
            problems += check_sql(path, naming)
            continue
        # Executable entry points beside shell scripts may share their kebab-case (PEP 8 governs
        # importable modules; `scripts/check-docs-drift.py` is run, never imported).
        if name.endswith('.py') and ENTRY_POINT_DIR.search(path) and re.fullmatch(rf'{KEBAB}\.py', name):
            continue
        for extension, rule, text in COMPILED:
            if name.endswith(extension) and not rule.match(name):
                problems.append(f'{path}: file name should be {text}')
                break
    if problems:
        print('Naming conventions (see standards/03-code-quality/naming-conventions.md):')
        for problem in problems:
            print('  ' + problem)
        print(f'\n{len(problems)} problem(s). Rename before merge; renaming later breaks every link and import.')
        return 1
    print(f'Naming OK: {len(files)} new or renamed file(s) checked.')
    return 0


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
