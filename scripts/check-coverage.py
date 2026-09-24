#!/usr/bin/env python3
"""Coverage is a ratchet: it is checked against a floor, and the floor only ever goes up.

  check-coverage.py check   <name> <report> [<report> ...]   fail if below the floor for <name>
  check-coverage.py ratchet <base-ref>                        fail if any floor was lowered

Floors live in `.axelliant/coverage-floors`, one line per coverage target, with the reason it last
moved — the number is in one place, and its history is in git:

    api      line=62 branch=48   # 2026-09-24 raised after billing tests (AXE-412)
    web      line=71 branch=55   # 2026-09-20 initial floor

Why a ratchet: a fixed target that a legacy codebase cannot meet gets lowered "temporarily" and
stays lowered; a floor that rises with every improvement locks each gain in. Line AND branch are
both enforced because they are not the same thing: a test that walks one side of an `if` covers the
line and proves nothing about the other side, which is usually where the defect is.

Report formats, detected from content: Cobertura XML (.NET coverlet, Python coverage.py, Jest),
JaCoCo/Kover XML (Java, Kotlin, Android), LCOV (Karma, Jest, Vitest, Flutter), Xcode xccov JSON
(`xcrun xccov view --report --json`, line only), Go cover profiles (statement ≈ line only).
Multiple reports for one target are summed.

The watcher: every check prints, and writes to the GitHub job summary, how many more covered lines
reach the next whole percent, and says when the floor can be raised. Raising it is a one-line PR.

What it does NOT catch: tests that execute code without asserting anything. Coverage proves a line
ran, not that it was checked. Review the assertions.
"""
from __future__ import annotations

import json
import math
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402
import xml.etree.ElementTree as ET

FLOORS = '.axelliant/coverage-floors'


def parse_floors(text: str) -> dict[str, dict[str, float]]:
    """Parses coverage-floor lines into {target: {"line": n, "branch": n}}, ignoring comments."""
    floors = {}
    for raw in text.splitlines():
        line = raw.split('#', 1)[0].strip()
        if not line:
            continue
        name, *pairs = line.split()
        floors[name] = {k: float(v) for k, _, v in (p.partition('=') for p in pairs) if k in ('line', 'branch')}
    return floors


def totals(path: str) -> dict[str, int]:
    """{'lines': n, 'lines_hit': n, 'branches': n, 'branches_hit': n} for one report."""
    with open(path, encoding='utf-8', errors='replace') as handle:
        head = handle.read(4096)
    if head.lstrip().startswith('{'):
        return _xccov(path)
    if head.startswith('mode:'):
        return _go(path)
    if re.search(r'^(TN|SF):', head, re.M):
        return _lcov(path)
    root = _parse_report_xml(path)
    if root.tag == 'coverage':
        return _cobertura(root)
    if root.tag == 'report':
        return _jacoco(root)
    raise ValueError(f'{path}: unrecognised coverage format')


def _parse_report_xml(path: str):
    """Parses a Cobertura or JaCoCo report and returns its root element.

    Python's parser does not fetch external entities, but it does expand entities declared inside the
    document, which is how an "XML bomb" exhausts memory. No coverage tool declares entities (JaCoCo
    names an external DTD, which is never fetched), so a report that does is refused rather than
    parsed. That is the mitigation the use-defused-xml rule asks for, without a dependency.
    Raises ValueError for a report that declares an entity.
    """
    with open(path, 'rb') as handle:
        data = handle.read()
    if b'<!ENTITY' in data:
        raise ValueError(f'{path}: declares an XML entity; coverage reports never do, so it is refused')
    return ET.fromstring(data)  # nosemgrep: python.lang.security.use-defused-xml-parse, python.lang.security.use-defused-xml


def _cobertura(root) -> dict[str, int]:
    if root.get('lines-valid') is not None:
        return {'lines': int(root.get('lines-valid')), 'lines_hit': int(root.get('lines-covered')),
                'branches': int(root.get('branches-valid') or 0), 'branches_hit': int(root.get('branches-covered') or 0)}
    t = {'lines': 0, 'lines_hit': 0, 'branches': 0, 'branches_hit': 0}
    for line in root.iter('line'):
        t['lines'] += 1
        t['lines_hit'] += int(line.get('hits', '0')) > 0
        cond = re.search(r'\((\d+)/(\d+)\)', line.get('condition-coverage', ''))
        if cond:
            t['branches_hit'] += int(cond.group(1))
            t['branches'] += int(cond.group(2))
    return t


def _jacoco(root) -> dict[str, int]:
    t = {'lines': 0, 'lines_hit': 0, 'branches': 0, 'branches_hit': 0}
    for counter in root.findall('counter'):  # report-level counters only: children would double count
        missed, covered = int(counter.get('missed')), int(counter.get('covered'))
        if counter.get('type') == 'LINE':
            t['lines'], t['lines_hit'] = missed + covered, covered
        elif counter.get('type') == 'BRANCH':
            t['branches'], t['branches_hit'] = missed + covered, covered
    return t


def _lcov(path: str) -> dict[str, int]:
    t = {'lines': 0, 'lines_hit': 0, 'branches': 0, 'branches_hit': 0}
    keys = {'LF': 'lines', 'LH': 'lines_hit', 'BRF': 'branches', 'BRH': 'branches_hit'}
    with open(path, encoding='utf-8', errors='replace') as handle:
        for line in handle:
            key, _, value = line.strip().partition(':')
            if key in keys:
                t[keys[key]] += int(value)
    return t


def _xccov(path: str) -> dict[str, int]:
    with open(path, encoding='utf-8') as handle:
        data = json.load(handle)
    targets = [t for t in data.get('targets', []) if not t.get('name', '').endswith(('Tests.xctest', 'UITests.xctest'))]
    return {'lines': sum(t['executableLines'] for t in targets), 'lines_hit': sum(t['coveredLines'] for t in targets),
            'branches': 0, 'branches_hit': 0}


def _go(path: str) -> dict[str, int]:
    t = {'lines': 0, 'lines_hit': 0, 'branches': 0, 'branches_hit': 0}
    with open(path, encoding='utf-8') as handle:
        for line in list(handle)[1:]:
            parts = line.split()
            if len(parts) == 3:
                t['lines'] += int(parts[1])
                t['lines_hit'] += int(parts[1]) if int(parts[2]) > 0 else 0
    return t


def percent(hit: int, total: int) -> float:
    """Returns hit/total as a percentage, or 100 when there is nothing to cover."""
    return 100.0 * hit / total if total else 100.0


def summary(text: str) -> None:
    """Prints text and appends it to the GitHub job summary when running in Actions."""
    print(text)
    target = os.environ.get('GITHUB_STEP_SUMMARY')
    if target:
        with open(target, 'a', encoding='utf-8') as handle:
            handle.write(text + '\n')


def check(name: str, reports: list[str]) -> int:
    """Checks the summed coverage of the reports against the floor for a target; returns the exit code."""
    floors = parse_floors(open(FLOORS, encoding='utf-8').read()) if os.path.exists(FLOORS) else {}
    if name not in floors:
        print(f'FAIL no floor for "{name}" in {FLOORS}. Add one: `{name} line=<n> branch=<n>  # <date> <why>`.')
        return 1
    if not reports:
        print(f'FAIL no coverage report given for "{name}". Run the tests with coverage first; a missing report '
              'is a failure, never a pass.')
        return 1
    t = {'lines': 0, 'lines_hit': 0, 'branches': 0, 'branches_hit': 0}
    for report in reports:
        try:
            measured = totals(report)
        except (ValueError, ET.ParseError) as error:
            print(f'FAIL cannot read coverage report: {error}')
            return 1
        for key, value in measured.items():
            t[key] += value
    if t['lines'] == 0:
        print(f'FAIL the coverage reports for "{name}" measure no lines at all: {", ".join(reports)}. '
              'Check the include patterns; an empty denominator proves nothing.')
        return 1
    line, branch = percent(t['lines_hit'], t['lines']), percent(t['branches_hit'], t['branches'])
    floor = floors[name]
    status = 0
    rows = [f'### Coverage: {name}', '', '| | actual | floor | next step |', '|---|---|---|---|']
    for label, actual, hit, total in (('line', line, t['lines_hit'], t['lines']),
                                      ('branch', branch, t['branches_hit'], t['branches'])):
        if label not in floor:
            continue
        if total == 0 and label == 'branch':
            # Xcode and Go profiles cannot report branches; their floors say branch=0. Anything else with
            # a branch floor and no branch data has branch collection switched off, which must not pass.
            rows.append(f'| {label} | n/a (report carries no branches) | {floor[label]:.0f}% | — |')
            if floor[label] > 0:
                print(f'FAIL "{name}" has a branch floor of {floor[label]:g}% but its reports carry no branch data. '
                      'Enable branch collection (coverlet, v8, JaCoCo all support it), or set branch=0 for a '
                      'format that cannot report branches.')
                status = 1
            continue
        goal = math.floor(actual) + 1
        needed = max(0, math.ceil(goal / 100.0 * total) - hit)
        rows.append(f'| {label} | {actual:.2f}% ({hit}/{total}) | {floor[label]:.0f}% | {needed} more covered to reach {goal}% |')
        if actual + 1e-9 < floor[label]:
            status = 1
    summary('\n'.join(rows) + '\n')
    if status:
        print(f'FAIL coverage for "{name}" is below its floor. Add tests; never lower the floor.')
        return 1
    raisable = {k: math.floor(v) for k, v in (('line', line), ('branch', branch)) if k in floor and math.floor(v) > floor[k]}
    if raisable:
        suggestion = ' '.join(f'{k}={v}' for k, v in raisable.items())
        summary(f'The floor can be raised: set `{name} {suggestion}` in {FLOORS} to lock this in.')
    return 0


def ratchet(base_ref: str) -> int:
    """Fails when any floor in the committed file is lower than on the base ref; returns the exit code."""
    old = subprocess.run(['git', 'show', f'{base_ref}:{FLOORS}'], capture_output=True, text=True)
    if old.returncode != 0 or not os.path.exists(FLOORS):
        print('Coverage ratchet: no floors on the base branch yet; nothing to compare.')
        return 0
    before, after = parse_floors(old.stdout), parse_floors(open(FLOORS, encoding='utf-8').read())
    lowered = [f'{n} {k}: {v:g} -> {after.get(n, {}).get(k, 0):g}' for n, kinds in before.items()
               for k, v in kinds.items() if after.get(n, {}).get(k, 0) < v]
    if lowered:
        print('FAIL a coverage floor was lowered or removed. Floors only go up:')
        for row in lowered:
            print('  ' + row)
        return 1
    print('Coverage ratchet OK: no floor lowered.')
    return 0


def main(argv: list[str]) -> int:
    """Entry point: dispatches to check or ratchet; returns the exit code."""
    root = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True).stdout.strip()
    # Report paths are the caller's, relative to where it ran; the floors file is the repository's.
    reports = [os.path.abspath(path) for path in argv[3:]]
    if root:
        os.chdir(root)
    if len(argv) >= 3 and argv[1] == 'check':
        missing = [path for path in reports if not os.path.isfile(path)]
        if missing:
            print(f'FAIL coverage report not found: {", ".join(missing)}. Run the tests with coverage first.')
            return 1
        return check(argv[2], reports)
    if len(argv) == 3 and argv[1] == 'ratchet':
        return ratchet(argv[2])
    print(__doc__)
    return 2


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
