"""Repository-path glob matching shared by the Python guards.

`*` matches within one path segment, `**` across segments, `?` one character. This is the
behaviour people expect from .gitignore-style patterns; `fnmatch` lets `*` cross `/`, which made
`database/migrations/*.sql` also match `database/migrations/archive/old.sql` and protected files
nobody meant to protect.
"""
from __future__ import annotations

import re
from functools import lru_cache


@lru_cache(maxsize=None)
def compile_glob(pattern: str) -> re.Pattern:
    """Compiles a repository glob (** crosses directories, * does not) into an anchored regex."""
    out, i = [], 0
    while i < len(pattern):
        char = pattern[i]
        if pattern.startswith('**/', i):
            out.append('(?:.*/)?')
            i += 3
        elif pattern.startswith('**', i):
            out.append('.*')
            i += 2
        elif char == '*':
            out.append('[^/]*')
            i += 1
        elif char == '?':
            out.append('[^/]')
            i += 1
        else:
            out.append(re.escape(char))
            i += 1
    return re.compile('^' + ''.join(out) + '$')


def matches_any(path: str, patterns: list[str]) -> bool:
    """True when the path matches any of the globs."""
    return any(compile_glob(p).match(path) for p in patterns)
