#!/usr/bin/env python3
"""Fails when a GitHub Actions `run:` step interpolates `${{ ... }}`.

An expression is substituted as **text** before bash ever sees it. A pull
request title, a branch name or an input containing `"; rm -rf / #` therefore
becomes shell, and the value that does it is usually one somebody else controls.
The fix is always the same: pass it through `env:` and read `$THE_VARIABLE`.

    # wrong
    - run: dotnet restore "${{ inputs.solution }}"

    # right
    - env:
        SOLUTION: ${{ inputs.solution }}
      run: dotnet restore "$SOLUTION"

Why this exists: the gruhnguitar repository shipped this twice - fixed in its
workflows, then reintroduced by new composite actions - and each time Semgrep in
CI noticed several minutes and one merged pull request later. The shared
guardrail workflow had it too until this gate was adopted company-wide. Semgrep
and CodeQL find it as well; the point of this gate is that the pre-commit hook
runs it in two seconds without a runner.

What it does NOT catch: an expression in `with:` passed to an action that then
builds a shell command from it. That is the action's defect; pin actions by SHA
and review what they run.

Usage:  scripts/check-workflow-interpolation.py [paths...]
        (defaults to .github/workflows and .github/actions)
"""

from __future__ import annotations

import pathlib
import re
import sys

DEFAULT_ROOTS = [".github/workflows", ".github/actions"]

# `${{ ... }}`, the thing that gets substituted as text.
EXPRESSION = re.compile(r"\$\{\{.*?\}\}")

# A step's `run:` key, at any indentation, in either the single-line form
# (`run: something`) or a block form (`run: |`, `run: >`).
RUN_KEY = re.compile(r"^(?P<indent>\s*)-?\s*run:\s*(?P<value>.*)$")


def findings(path: pathlib.Path) -> list[tuple[int, str]]:
    """Every line inside a `run:` value that carries an expression."""
    found: list[tuple[int, str]] = []
    lines = path.read_text(encoding="utf-8").splitlines()

    index = 0
    while index < len(lines):
        match = RUN_KEY.match(lines[index])
        if match is None:
            index += 1
            continue

        value = match.group("value").strip()

        # Single-line: `run: dotnet restore "${{ inputs.solution }}"`.
        if value and value not in ("|", ">", "|-", ">-", "|+", ">+"):
            if EXPRESSION.search(lines[index]):
                found.append((index + 1, lines[index].strip()))
            index += 1
            continue

        # Block scalar: everything indented further than the `run:` key.
        run_indent = len(match.group("indent"))
        index += 1
        while index < len(lines):
            line = lines[index]
            if line.strip() and (len(line) - len(line.lstrip())) <= run_indent:
                break
            if EXPRESSION.search(line):
                found.append((index + 1, line.strip()))
            index += 1

    return found


def main(argv: list[str]) -> int:
    """Scans the given files or directories (default: .github/workflows and .github/actions).

    Prints each `run:` line that interpolates an expression, to stderr. Returns 1 when any is
    found, 0 otherwise, including when there are no workflow files at all.
    """
    roots = [pathlib.Path(a) for a in argv[1:]] or [pathlib.Path(r) for r in DEFAULT_ROOTS]

    files: list[pathlib.Path] = []
    for root in roots:
        if root.is_file():
            files.append(root)
        elif root.is_dir():
            files.extend(sorted(root.rglob("*.yml")))
            files.extend(sorted(root.rglob("*.yaml")))

    if not files:
        print("workflow-interpolation: no workflow or action files found.")
        return 0

    problems = {path: hits for path in files if (hits := findings(path))}

    if not problems:
        print(f"No expression reaches a run step ({len(files)} workflow/action file(s) scanned).")
        return 0

    total = sum(len(hits) for hits in problems.values())
    print(f"FAIL {total} run step(s) interpolate an expression:\n", file=sys.stderr)
    for path, hits in problems.items():
        for line_number, text in hits:
            print(f"    {path}:{line_number}", file=sys.stderr)
            print(f"        {text[:110]}", file=sys.stderr)
    print(
        "\n    An expression is substituted as text before the shell sees it, so a value\n"
        "    somebody else controls becomes somebody else's shell command.\n\n"
        "    Pass it through env: instead --\n"
        "        env:\n"
        "          THING: ${{ inputs.thing }}\n"
        "        run: do-something \"$THING\"\n",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
