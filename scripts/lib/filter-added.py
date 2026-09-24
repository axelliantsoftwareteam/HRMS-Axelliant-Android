#!/usr/bin/env python3
"""Filter `path:line:...` findings on stdin to those on lines added since a base ref.

  <finder> | python3 scripts/lib/filter-added.py <base-ref>

Used by the grep-based gates so that, given a base, they judge only what a change adds.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ax_config  # noqa: E402

added = ax_config.added_lines(sys.argv[1])
root = ax_config.repo_root()
for raw in sys.stdin:
    path, _, rest = raw.partition(':')
    number = rest.partition(':')[0]
    relative = os.path.relpath(os.path.abspath(path), root).replace(os.sep, '/')
    if number.isdigit() and int(number) in added.get(relative, set()):
        sys.stdout.write(raw)
