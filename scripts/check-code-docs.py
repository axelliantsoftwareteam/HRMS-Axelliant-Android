#!/usr/bin/env python3
"""Every public declaration a change ADDS carries a doc comment.

  check-code-docs.py <base-ref>     declarations added since base-ref (CI, pre-push)
  check-code-docs.py --staged       declarations in staged changes (pre-commit)

Why this exists: code is read far more than it is written, usually by someone who was not there — a
colleague, the client's next vendor, an AI agent. A doc comment is how that reader learns what a unit
is for and how to call it safely. Linters can enforce this too, but only once a repository has
switched them on, and switching them on over a legacy codebase produces thousands of findings that
get suppressed wholesale. This checks only what the change adds, so it works from day one on any
repository.

Languages and what counts as public:
  Python      module-level and class-level def/class not starting with "_"      docstring (exact, via ast)
  C#          `public` types and members, except `override` and repeated `partial` parts   `///` above
  Java        `public` types and methods, except `@Override`                    `/** */` above
  Kotlin      class/interface/object/fun without private/internal/protected/override   `/** */` above
  TS/JS       `export` function/class/interface/type/enum/const                  `/** */` above
  Go          exported func/type/method (capitalised)                            `// Name …` above
  Swift       `public`/`open` declarations and top-level types                   `///` or `/** */` above
  Dart        top-level and 2-space-indented members not starting with "_"      `///` above

Attributes, annotations and decorators may sit between the doc comment and the declaration.
Tests, generated code and build output are never checked. CODE_DOCS_EXCLUDE (extended regex, in
.axelliant/standards.env) excludes further paths, e.g. EF Core entity folders or generated clients.

What it does NOT catch: a doc comment that is wrong or merely restates the signature. Review that.
"""
from __future__ import annotations

import ast
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402

TEST_OR_GENERATED = re.compile(
    r'(^|/)(tests?|__tests__|spec|androidTest|testFixtures|UITests|generated|__generated__|Migrations|migrations|'
    r'node_modules|dist|build|bin|obj|vendor|Pods|\.dart_tool)/|(\.spec\.|\.test\.|_test\.|Tests?\.(cs|java|kt|swift)$|'
    r'(^|/)test_|\.g\.(cs|dart)$|\.Designer\.cs$|\.d\.ts$|\.freezed\.dart$|conftest\.py$)')
ANNOTATION = re.compile(r'^\s*(\[[^\]]*\]\s*)+$|^\s*(@[\w.]+(\([^)]*\))?\s*)+$|^\s*#\[.*\]\s*$')

RULES = {
    '.cs': (re.compile(r'^\s*public\s+(?!override\b)(?:(?:static|sealed|abstract|virtual|async|partial|readonly|required|new|unsafe|extern)\s+)*'
                       r'(?:class|interface|record|struct|enum|delegate|event|[\w<>\[\],.?() ]+\s+\w+\s*[({=;]|[\w<>\[\],.?() ]+\s+\w+\s*$)'),
            lambda prev: prev.lstrip().startswith('///')),
    '.java': (re.compile(r'^\s*public\s+(?:(?:static|final|abstract|synchronized|default|sealed)\s+)*'
                         r'(?:class|interface|record|enum|@interface|<[^>]*>\s*[\w<>\[\],.? ]+\s+\w+\s*\(|[\w<>\[\],.? ]+\s+\w+\s*\()'),
              lambda prev: prev.rstrip().endswith('*/')),
    '.kt': (re.compile(r'^\s*(?:(?:public|open|abstract|sealed|data|enum|inline|value|suspend|operator|infix|tailrec|external|annotation|inner)\s+)*'
                       r'(?:class|interface|object|fun)\s'),
            lambda prev: prev.rstrip().endswith('*/')),
    '.ts': (re.compile(r'^\s*export\s+(?:default\s+)?(?:declare\s+)?(?:async\s+)?(?:abstract\s+)?(?:function\*?|class|interface|type|enum|const)\s+\w'),
            lambda prev: prev.rstrip().endswith('*/')),
    '.go': (re.compile(r'^(?:func\s+(?:\([^)]*\)\s*)?[A-Z]\w*\s*[\[(]|type\s+[A-Z]\w*\s)'),
            lambda prev: prev.startswith('//')),
    '.swift': (re.compile(r'^\s*(?:@\w+\s+)*(?:public|open)\s+(?:(?:final|static|class|override|mutating|convenience|required)\s+)*'
                          r'(?:func|class|struct|enum|protocol|actor|var|let|init|typealias|subscript)\b|^(?:final\s+)?(?:class|struct|enum|protocol|actor)\s+[A-Z]'),
               lambda prev: prev.lstrip().startswith('///') or prev.rstrip().endswith('*/')),
    '.dart': (re.compile(r'^(?:abstract\s+|sealed\s+|base\s+|final\s+)*(?:class|enum|mixin|extension|typedef)\s+[A-Z]|^[A-Za-z][\w<>?, ]*\s+[a-z]\w*\s*\(|^  (?:static\s+|Future<[^>]*>\s+|[\w<>?]+\s+)[a-z]\w*\s*\('),
              lambda prev: prev.lstrip().startswith('///')),
}
for alias in ('.tsx', '.js', '.jsx', '.mjs'):
    RULES[alias] = RULES['.ts']
RULES['.kts'] = RULES['.kt']
OVERRIDE = re.compile(r'\boverride\b|@Override\b')
KOTLIN_NOT_PUBLIC = re.compile(r'\b(private|internal|protected|override)\b')
CONTROL = re.compile(r'^\s*(?:return|if|for|while|switch|catch|else|new|throw|await)\b')


def git(*args: str) -> str:
    """Runs git with the given arguments and returns its standard output."""
    return subprocess.run(['git', *args], check=True, capture_output=True, text=True).stdout


def added_lines(argv: list[str]) -> dict[str, set[int]]:
    """Lines this change genuinely adds (moved lines excluded — see ax_config.added_lines)."""
    if argv == ['--staged']:
        diff = git('diff', '--cached', '-U0', '--no-color')
        return _parse_staged(diff)
    if len(argv) == 1:
        return ax_config.added_lines(argv[0])
    raise SystemExit(__doc__)


def _parse_staged(diff: str) -> dict[str, set[int]]:
    from collections import Counter
    result: dict[str, set[int]] = {}
    added, removed, current, number = [], Counter(), None, 0
    for line in diff.splitlines():
        if line.startswith('+++ '):
            current = line[6:] if line.startswith('+++ b/') else None
            if current:
                result.setdefault(current, set())
        elif line.startswith('--- '):
            continue
        elif line.startswith('@@'):
            number = int(re.search(r'\+(\d+)', line).group(1))
        elif line.startswith('-'):
            removed[line[1:].strip()] += 1
        elif line.startswith('+') and current:
            added.append((current, number, line[1:].strip())); number += 1
    for path, lineno, text in added:
        if text and removed[text] > 0:
            removed[text] -= 1
            continue
        result[path].add(lineno)
    return result


def python_missing(path: str, added: set[int]) -> list[str]:
    """Returns undocumented public module- and class-level functions and classes added in a Python file."""
    try:
        tree = ast.parse(open(path, encoding='utf-8', errors='replace').read())
    except SyntaxError:
        return []
    missing = []

    def visit(nodes, inside_function: bool) -> None:
        for node in nodes:
            is_def = isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef))
            if is_def and not inside_function and node.lineno in added and not node.name.startswith('_'):
                decorators = {getattr(d, 'id', getattr(d, 'attr', '')) for d in getattr(node, 'decorator_list', [])}
                if not ast.get_docstring(node) and 'override' not in decorators:
                    missing.append(f'{path}:{node.lineno}: {node.name} has no docstring')
            if is_def:
                visit(node.body, inside_function or not isinstance(node, ast.ClassDef))
    visit(tree.body, False)
    return missing


def declared_name(line: str) -> str:
    """The identifier a declaration line introduces, for the report."""
    text = line.strip()
    keyword = re.search(r'\b(?:class|interface|record|struct|enum|protocol|actor|object|function\*?|fun|func|type|const|typedef|mixin|extension)\s+(?:[\w.]+\.)?([A-Za-z_]\w*)', text)
    if keyword:
        return keyword.group(1)
    call = re.search(r'([A-Za-z_]\w*)\s*(?:<[^>]*>)?\s*\(', text)
    if call:
        return call.group(1)
    member = re.search(r'([A-Za-z_]\w*)\s*(?:\{|=|;|$)', text)
    return member.group(1) if member else text[:60]


def brace_missing(path: str, added: set[int], extension: str) -> list[str]:
    """Returns undocumented public declarations added in a C-family file, per the language rule."""
    declaration, documented = RULES[extension]
    lines = open(path, encoding='utf-8', errors='replace').read().replace('\r\n', '\n').split('\n')
    missing = []
    for number in sorted(added):
        if number > len(lines):
            continue
        line = lines[number - 1]
        if not declaration.match(line) or CONTROL.match(line) or OVERRIDE.search(line):
            continue
        # A partial type is documented once, on any one of its parts (as C#'s CS1591 expects).
        if extension == '.cs' and re.search(r'\bpartial\s+(class|record|struct|interface)\b', line):
            continue
        if extension in ('.kt', '.kts') and KOTLIN_NOT_PUBLIC.search(line.split('fun')[0].split('class')[0]):
            continue
        if extension == '.dart' and re.search(r'\b_\w*\s*\(', line.split('(')[0] + '('):
            continue
        cursor = number - 2
        while cursor >= 0 and ANNOTATION.match(lines[cursor]):
            if OVERRIDE.search(lines[cursor]):
                break
            cursor -= 1
        if cursor >= 0 and OVERRIDE.search(lines[cursor]):
            continue
        previous = lines[cursor] if cursor >= 0 else ''
        if not documented(previous):
            missing.append(f'{path}:{number}: {declared_name(line)} has no doc comment')
    return missing


def main(argv: list[str]) -> int:
    """Entry point: checks every file the change adds lines to; returns the exit code."""
    root = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True).stdout.strip()
    if root:
        os.chdir(root)
    exclude = os.environ.get('CODE_DOCS_EXCLUDE', '')
    missing = []
    for path, added in added_lines(argv[1:]).items():
        extension = os.path.splitext(path)[1]
        if not added or not os.path.exists(path) or TEST_OR_GENERATED.search(path):
            continue
        if exclude and re.search(exclude, path):
            continue
        if extension == '.py':
            missing += python_missing(path, added)
        elif extension in RULES:
            missing += brace_missing(path, added, extension)
    if missing:
        print('Public declarations added without a doc comment (standards/03-code-quality/code-documentation.md):')
        for row in missing:
            print('  ' + row)
        print(f'\n{len(missing)} undocumented. Say what it does, its parameters and return, what it throws, and its side effects.')
        return 1
    print('Code documentation OK: every public declaration this change adds is documented.')
    return 0


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
