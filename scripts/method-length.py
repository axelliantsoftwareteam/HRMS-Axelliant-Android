#!/usr/bin/env python3
"""Measure function lengths, and hold the line against god functions.

  method-length.py check    <root> <baseline> [limit]
  method-length.py baseline <root> <baseline> [limit]
  method-length.py report   <root> [limit]

A function at or over `limit` lines must be listed in the baseline, and may never grow past the
length recorded there. New long functions fail; shrinking one and re-baselining is how the list
gets shorter. Nothing here forces an existing long function to be split today — only stops it
getting worse, and stops new ones.

Why: a function of several hundred lines cannot be read in one sitting, cannot be reached from a
test except through everything around it, and hides the difference between two paths that
diverge halfway down. That is how untestable code and undetected defects get in.

Languages: Python is parsed with `ast` (exact). C#, Java, Kotlin, Swift, Go, Dart, TypeScript and
JavaScript are measured by matching a signature and counting braces to the close. Comments, string
and character literals, verbatim and raw strings, and template literals (with their `${...}`
interpolations) are blanked first, so a `}` in a comment or a string never closes a body early — an
early close under-measures, which is the one error a gate like this must not make. What remains
heuristic: a regex literal containing a brace, and an apostrophe in JSX text (read as a string to the
end of that line). A wrong measurement is visible in the output, so it is corrected rather than hidden.

What it does NOT catch: a long function written as a chain of lambdas, a long SQL string, or a
template (.html/.razor/.vue template block). Those are the reviewer's job.
"""
from __future__ import annotations

import ast
import os
import re
import sys

DEFAULT_LIMIT = 150

# Words that open a block but are not a function.
CONTROL = {
    'if', 'for', 'foreach', 'while', 'switch', 'catch', 'return', 'else', 'do', 'try', 'using',
    'lock', 'synchronized', 'when', 'guard', 'new', 'throw', 'await', 'typeof', 'sizeof',
    'nameof', 'function', 'select', 'defer', 'go', 'case', 'with', 'fixed', 'checked', 'super',
    'this', 'yield', 'match',
    # Test-framework containers group tests; they are not functions anyone calls.
    'describe', 'context', 'suite', 'fdescribe', 'xdescribe',
}

# A type declaration (including a primary constructor) is not a function body. Anchored to the
# start of the declaration so a generic argument such as `Task<object>` is not mistaken for one.
TYPE_DECLARATION = re.compile(
    r'^\s*(?:[\w@]+\s+)*(class|record|struct|interface|enum|object|protocol|extension)\s+\w')

SIGNATURES = {
    '.cs': re.compile(
        r'^\s*(?:\[[^\]]*\]\s*)*(?:(?:public|private|internal|protected|static|virtual|override|'
        r'abstract|sealed|async|extern|unsafe|new|partial)\s+)+[\w<>\[\],.?() ]*?\b(\w+)\s*(?:<[^>]*>)?\s*\('),
    '.java': re.compile(
        r'^\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:(?:public|private|protected|static|final|abstract|'
        r'synchronized|native|default)\s+)*(?:<[^>]*>\s*)?[\w<>\[\],.? ]+\s+(\w+)\s*\([^;]*$'),
    '.kt': re.compile(r'^\s*(?:[\w@()\s]*\s)?fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\('),
    '.kts': re.compile(r'^\s*(?:[\w@()\s]*\s)?fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\('),
    '.swift': re.compile(r'^\s*(?:[@\w()\s]*\s)?(?:func\s+(\w+)|(init|deinit)\b)\s*[<(]?'),
    '.go': re.compile(r'^func\s+(?:\([^)]*\)\s*)?(\w+)\s*[\[(]'),
    '.dart': re.compile(
        r'^\s*(?:@\w+\s*)*(?:(?:static|external|factory|async)\s+)*(?:[\w<>?,\[\] ]+\s+)?(\w+)\s*\([^;]*$'),
    '.ts': re.compile(
        r'^\s*(?:export\s+)?(?:default\s+)?(?:(?:public|private|protected|static|async|readonly|'
        r'override|abstract|get|set)\s+)*(?:function\s*\*?\s*(\w+)|(\w+)\s*(?:<[^>]*>)?\s*\([^;]*$|'
        r'(?:const|let|var)\s+(\w+)\s*(?::[^=]+)?=\s*(?:async\s+)?(?:function\b|\([^)]*\)\s*(?::[^=]+)?=>|\w+\s*=>))'),
}
for alias in ('.tsx', '.js', '.jsx', '.mjs', '.cjs'):
    SIGNATURES[alias] = SIGNATURES['.ts']

EXTENSIONS = tuple(SIGNATURES) + ('.py',)
# Backticks delimit template literals with `${...}` interpolation here; elsewhere (Go) a raw string.
TEMPLATE_LITERAL_EXTENSIONS = ('.ts', '.tsx', '.js', '.jsx', '.mjs', '.cjs')

# Not our code: dependencies, build output, caches, generated migrations.
SKIP = {
    'obj', 'bin', 'node_modules', '.venv', 'venv', 'env', '.git', '__pycache__', '.pytest_cache',
    '.ruff_cache', '.mypy_cache', 'migrations', 'Migrations', '.python_packages', 'site-packages',
    'dist', 'build', 'out', 'target', 'coverage', '.next', '.angular', '.gradle', 'DerivedData',
    'Pods', '.dart_tool', 'generated', '__generated__', 'vendor', 'storybook-static',
}


def _name(match: re.Match) -> str | None:
    return next((g for g in match.groups() if g), None)


def code_only(lines: list[str], template_interpolation: bool) -> list[str]:
    """Return the lines with comments and literal contents blanked, keeping line numbers.

    Strings keep their delimiters (`""`) so a signature such as `f(a = "x") {` still matches. State
    carries across lines for block comments, triple-quoted/raw strings, C# verbatim strings, and
    backtick strings (JS/TS templates, Go raw strings). Plain `'` and `"` strings end at the end of the
    line, which is the language rule for every supported language and bounds the damage of an
    apostrophe in prose (JSX text) to one line.
    """
    out: list[str] = []
    mode = None          # None | 'block' | '"""' | 'verbatim' | '`'
    interpolation = []   # brace depth per open `${` inside a template literal
    for line in lines:
        kept: list[str] = []
        i, n = 0, len(line)
        quote = None     # single-line ' or " string in progress
        while i < n:
            c, pair = line[i], line[i:i + 2]
            if mode == 'block':
                if pair == '*/':
                    mode, i = None, i + 2
                else:
                    i += 1
                continue
            if mode == '"""':
                if line.startswith('"""', i):
                    kept.append('""'); mode, i = None, i + 3
                else:
                    i += 1
                continue
            if mode == 'verbatim':
                if pair == '""':
                    i += 2
                elif c == '"':
                    kept.append('""'); mode, i = None, i + 1
                else:
                    i += 1
                continue
            if mode == '`' and not interpolation:
                if c == '\\':
                    i += 2
                elif c == '`':
                    kept.append('""'); mode, i = None, i + 1
                elif template_interpolation and pair == '${':
                    interpolation.append(0); i += 2
                else:
                    i += 1
                continue
            if mode == '`' and interpolation:
                # Inside `${ ... }`: code, but its braces balance within the template, so none are kept.
                if c == '{':
                    interpolation[-1] += 1
                elif c == '}':
                    if interpolation[-1] == 0:
                        interpolation.pop()
                    else:
                        interpolation[-1] -= 1
                i += 1
                continue
            if quote:
                if c == '\\':
                    i += 2
                elif c == quote:
                    kept.append(quote * 2); quote, i = None, i + 1
                else:
                    i += 1
                continue
            if pair == '//':
                break
            if pair == '/*':
                mode, i = 'block', i + 2
                continue
            if line.startswith('"""', i):
                mode, i = '"""', i + 3
                continue
            if pair in ('@"',) or line.startswith(('$@"', '@$"'), i):
                mode = 'verbatim'
                i += 2 if pair == '@"' else 3
                continue
            if c == '`':
                mode, i = '`', i + 1
                continue
            if c in '"\'':
                quote, i = c, i + 1
                continue
            kept.append(c)
            i += 1
        out.append(''.join(kept))
    return out


def brace_functions(path: str, signature: re.Pattern):
    """Yield (length, name, line) for every brace-delimited function body in one file."""
    with open(path, encoding='utf-8', errors='replace', newline='') as handle:
        raw = handle.read().replace('\r\n', '\n').split('\n')
    lines = code_only(raw, template_interpolation=os.path.splitext(path)[1] in TEMPLATE_LITERAL_EXTENSIONS)

    index = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        match = signature.match(line)
        name = _name(match) if match else None
        # `new` is also a C# modifier (member hiding), so `new Dictionary<K, V>(comparer)` — an object
        # creation, often the start of a long data initializer — must not be taken for a method.
        if stripped.startswith('new ') and not re.match(
                r'new\s+(?:public|private|protected|internal|static|virtual|override|abstract|sealed|async|unsafe|extern)\b', stripped):
            index += 1
            continue
        if (not match or not name or name in CONTROL or stripped.startswith(('//', '*', '/*'))
                or TYPE_DECLARATION.match(line) or stripped.endswith(';')):
            index += 1
            continue

        depth, opened, end = 0, False, None
        for cursor in range(index, min(index + 5000, len(lines))):
            text = lines[cursor]
            depth += text.count('{') - text.count('}')
            if '{' in text:
                opened = True
            if opened and depth <= 0:
                end = cursor
                break
            # A declaration with no body: interface/abstract/extern method or a call statement.
            if not opened and cursor > index and text.rstrip().endswith(';'):
                break
            # Expression-bodied or single-expression functions never open a brace. Stop when the
            # next function begins rather than after a fixed count, so a long parameter list —
            # one per line — is still followed to its body.
            if not opened and cursor > index and (signature.match(text) or cursor - index > 60):
                break

        if end is None:
            index += 1
            continue

        yield end - index + 1, name, index + 1
        # Continue inside the body too: nested/local functions are measured on their own.
        index += 1


def python_functions(path: str):
    """Yield (length, name, line) for every function in one Python file, exactly."""
    try:
        with open(path, encoding='utf-8', errors='replace') as handle:
            tree = ast.parse(handle.read())
    except SyntaxError:
        return  # The linter reports a file that will not parse; this is not the place.
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.end_lineno:
            start = min([node.lineno] + [d.lineno for d in node.decorator_list])
            yield node.end_lineno - start + 1, node.name, start


def functions(path: str):
    """Yields (length, name, line) for every function in a file, whatever its language."""
    extension = os.path.splitext(path)[1]
    if extension == '.py':
        yield from python_functions(path)
    elif extension in SIGNATURES and not path.endswith(('.d.ts', '.min.js')):
        yield from brace_functions(path, SIGNATURES[extension])


def measure(root: str, limit: int) -> dict[str, int]:
    """Every function at or over the limit, keyed by 'path::name'."""
    found: dict[str, int] = {}
    for base, dirs, files in os.walk(root):
        dirs[:] = sorted(d for d in dirs if d not in SKIP)
        for name in sorted(files):
            if not name.endswith(EXTENSIONS):
                continue
            path = os.path.join(base, name)
            # Forward slashes always: the baseline is shared between Linux CI and Windows
            # developers. A baseline written with backslashes matched nothing on Linux, so every
            # recorded function was reported as new and the gate failed on a clean checkout.
            relative = os.path.relpath(path, root).replace(os.sep, '/')
            for length, function, _line in functions(path):
                if length >= limit:
                    key = f'{relative}::{function}'
                    found[key] = max(found.get(key, 0), length)  # overloads held to the longest
    return found


def read_baseline(path: str) -> dict[str, int]:
    """Returns {path::function: allowed length} from a baseline file (empty if missing)."""
    recorded: dict[str, int] = {}
    if not os.path.exists(path):
        return recorded
    with open(path, encoding='utf-8') as handle:
        for row in handle:
            row = row.strip()
            if not row or row.startswith('#'):
                continue
            key, _, length = row.rpartition(' ')
            recorded[key.strip()] = int(length)
    return recorded


def write_baseline(path: str, found: dict[str, int], limit: int) -> None:
    """Writes the functions at or over the limit as the new baseline."""
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(
            f'# Functions of {limit} lines or more, with the length each is currently allowed.\n'
            '# Generated by scripts/method-length.py. A function may shrink freely; growing one,\n'
            '# or adding a new long one, fails the build. Shrink and regenerate to shorten this\n'
            '# list. It is meant to reach zero, and may only ever get shorter.\n'
        )
        for key in sorted(found):
            handle.write(f'{key} {found[key]}\n')


def check(root: str, baseline_path: str, limit: int) -> int:
    """Fails on functions over the limit that are new or longer than their baseline; returns the exit code."""
    found = measure(root, limit)
    recorded = read_baseline(baseline_path)
    added = sorted(key for key in found if key not in recorded)
    grown = sorted((k, recorded[k], found[k]) for k in found if k in recorded and found[k] > recorded[k])

    for key in added:
        print(f'NEW  {key} is {found[key]} lines (limit {limit}). Split it into named steps.')
    for key, was, now in grown:
        print(f'GREW {key} {was} -> {now} lines. It was already too long; do not add to it.')

    if added or grown:
        print(f'\n{len(added)} new and {len(grown)} grown. Split the work into named functions.\n'
              f'If you deliberately shortened others, regenerate the baseline:\n'
              f'  scripts/method-length.py baseline {root} {baseline_path} {limit}')
        return 1

    improved = sum(1 for k in recorded if found.get(k, 0) < recorded[k])
    print(f'Method length OK: {len(found)} at {limit}+ lines, none new or grown ({improved} improved or gone).')
    return 0


def main(argv: list[str]) -> int:
    """Entry point: dispatches to check, baseline or report; returns the exit code."""
    if len(argv) >= 3 and argv[1] == 'report':
        limit = int(argv[3]) if len(argv) > 3 else DEFAULT_LIMIT
        for key, length in sorted(measure(argv[2], limit).items(), key=lambda kv: -kv[1]):
            print(f'{length:6d}  {key}')
        return 0
    if len(argv) < 4 or argv[1] not in ('check', 'baseline'):
        print(__doc__)
        return 2
    mode, root, baseline_path = argv[1], argv[2], argv[3]
    limit = int(argv[4]) if len(argv) > 4 else DEFAULT_LIMIT
    if mode == 'baseline':
        found = measure(root, limit)
        write_baseline(baseline_path, found, limit)
        print(f'Recorded {len(found)} functions of {limit}+ lines in {baseline_path}.')
        return 0
    return check(root, baseline_path, limit)


if __name__ == '__main__':
    sys.exit(main(sys.argv))
