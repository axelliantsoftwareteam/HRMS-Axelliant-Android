#!/usr/bin/env python3
"""Find exception text on its way to an API caller, in any of our server languages.

  exception-leaks.py [--allow REGEX] [--base REF] <root> [<root> ...]

With --base, only findings on lines added since REF are reported: in a legacy repository existing
leaks are standards debt to fix when touched, and every new one still fails.

Why: a raw exception message tells an attacker about the schema, the ORM and the file layout, and
tells an honest user nothing they can act on. Failures go through the repository's one reporting
helper, which gives the caller a correlation reference and sends the detail to the logger and APM.
A sentence deliberately written for a human is thrown as the repository's documented domain
exception, whose message is passed through on purpose. `--allow` names both (the
EXCEPTION_REPORTING_ALLOW setting).

Three routes reach a caller, and a grep for the first only proves the others were not looked for:

    response.Message = $"...{ex.Message}"                 direct          (C#)
    raise HTTPException(500, detail=str(e))               direct          (Python)
    res.status(500).json({ message: err.message })        direct          (TS/JS)
    return ResponseEntity.status(500).body(e.getMessage())                (Java/Kotlin)
    http.Error(w, err.Error(), 500)                                       (Go)
    var cause = ex.GetBaseException().Message;            through a local, then returned

The last is why this is a small checker rather than a pattern: a local that takes exception text is
followed to see whether it later reaches a response.

What it does NOT catch: exception text serialised by a framework default (e.g. a debug error page
left on in production, or `detail` exposed by an unconfigured exception handler). The global
handler and its configuration are the defence there; this cannot see configuration.
"""
from __future__ import annotations

import os
import re
import sys

LANGUAGES = {
    'csharp': {
        'ext': ('.cs',),
        'function': re.compile(r'^\s*(?:\[[^\]]*\]\s*)*(?:public|private|protected|internal)\s[^=;]*\('),
        'comment': '//',
        'text': re.compile(r'\b\w+\.GetBaseException\(\)\.Message\b|\b(?:ex|exception|e|err|error)\.(?:Message|InnerException|StackTrace|ToString\(\))'
                           r'|\bSqlState\b|\bConstraintName\b'),
        'sink': re.compile(r'\b(?:_response|response|_resp|resp|result|_result)\.(?:Message|Data|Error)\s*\+?='
                           r'|\breturn\s+(?:BadRequest|Ok|StatusCode|Problem|Conflict|NotFound|Unauthorized|UnprocessableEntity|Results\.\w+|TypedResults\.\w+)\s*\('
                           r'|\b(?:Message|Error|Detail|Title|Errors)\s*=\s*'),
        'log': re.compile(r'_?[Ll]ogger\.|\.Log(?:Error|Warning|Information|Debug|Critical|Trace)\(|Activity\.|span\.'),
        'local': re.compile(r'^\s*(?:var|string)\s+(\w+)\s*=\s*(.+)$'),
    },
    'python': {
        'ext': ('.py',),
        'function': re.compile(r'^\s*(?:async\s+)?def\s+\w+'),
        'comment': '#',
        'text': re.compile(r'\bstr\(\s*(?:e|ex|exc|err|error|exception)\s*\)|\brepr\(\s*(?:e|ex|exc|err|error)\s*\)'
                           r'|\b(?:e|ex|exc|err|error|exception)\.(?:args|message|detail|orig)\b|traceback\.format_exc\(|f"[^"]*\{(?:e|ex|exc|err|error)\}'
                           r"|f'[^']*\{(?:e|ex|exc|err|error)\}"),
        'sink': re.compile(r'\bHTTPException\(|\bJSONResponse\(|\bjsonify\(|\bResponse\(|\bPlainTextResponse\(|\babort\('
                           r'|\breturn\s+\{|["\'](?:detail|error|message|msg)["\']\s*:'),
        'log': re.compile(r'\b(?:_?logger|log|logging)\.(?:debug|info|warning|error|exception|critical)\(|\bspan\.|sentry_sdk\.'),
        'local': re.compile(r'^\s*(\w+)\s*(?::\s*\w+\s*)?=\s*(.+)$'),
    },
    'typescript': {
        'ext': ('.ts', '.js', '.mjs', '.cjs'),
        'function': re.compile(r'^\s*(?:export\s+)?(?:async\s+)?function\b|^\s*(?:public|private|protected|static|async)\s+\w+\s*\(|^\s*(?:export\s+)?const\s+\w+\s*=\s*(?:async\s*)?\('),
        'comment': '//',
        'text': re.compile(r'\b(?:e|ex|err|error|exception|cause)\.(?:message|stack|toString\(\)|detail|sqlMessage|code)\b|\bString\(\s*(?:e|err|error)\s*\)'),
        'sink': re.compile(r'\bres\.(?:status\([^)]*\)\.)?(?:json|send|end)\(|\breply\.(?:code\([^)]*\)\.)?send\(|\bctx\.body\s*='
                           r'|\bNextResponse\.json\(|\bResponse\.json\(|\bnew\s+Response\(|throw\s+new\s+HttpException\(|\bthrow\s+new\s+\w+Exception\('
                           r'|\b(?:message|error|detail)\s*:\s*'),
        'log': re.compile(r'\b(?:this\.)?(?:_?logger|log)\.(?:debug|info|warn|error|fatal|trace|log)\(|\bspan\.'),
        'local': re.compile(r'^\s*(?:const|let|var)\s+(\w+)\s*(?::[^=]+)?=\s*(.+)$'),
    },
    'jvm': {
        'ext': ('.java', '.kt'),
        'function': re.compile(r'^\s*(?:[\w@]+\s+)*fun\s+\w+|^\s*(?:public|private|protected)\s[^=;]*\('),
        'comment': '//',
        'text': re.compile(r'\b(?:e|ex|exc|err|exception|t|throwable)\.(?:getMessage|getLocalizedMessage|toString|getCause|message|localizedMessage)\b'
                           r'|\bExceptionUtils\.getStackTrace\('),
        'sink': re.compile(r'\bResponseEntity\b|\.body\(|\bok\(|\bbadRequest\(|\bstatus\(\s*\d+\s*\)|\bcall\.respond\w*\('
                           r'|\bthrow\s+(?:new\s+)?ResponseStatusException\('),
        'log': re.compile(r'\b(?:log|logger|LOG|LOGGER)\.(?:debug|info|warn|error|trace)\('),
        'local': re.compile(r'^\s*(?:val|var|String|final\s+String)\s+(\w+)\s*(?::\s*\w+\??)?\s*=\s*(.+)$'),
    },
    'go': {
        'ext': ('.go',),
        'function': re.compile(r'^func\s'),
        'comment': '//',
        'text': re.compile(r'\berr\.Error\(\)|%v",\s*err\b|%s",\s*err\b|\berr\b\s*\)'),
        'sink': re.compile(r'\bhttp\.Error\(|\bc\.(?:JSON|String|AbortWithStatusJSON)\(|\bw\.Write\(|\bjson\.NewEncoder\(w\)'),
        'log': re.compile(r'\b(?:log|logger|slog|zap|zerolog)\.\w+\('),
        'local': re.compile(r'^\s*(\w+)\s*:?=\s*(.+)$'),
    },
}

SKIP = {'obj', 'bin', 'node_modules', '.venv', 'venv', '__pycache__', 'dist', 'build', 'target',
        'coverage', '.next', 'tests', 'test', '__tests__', 'spec', 'migrations', 'vendor', '.gradle'}
TEST_FILE = re.compile(r'(\.spec\.|\.test\.|_test\.|Tests?\.(cs|java|kt)$|(^|/)test_)')


def language_for(path: str):
    """Returns the language rules for a file by extension, or None."""
    for language in LANGUAGES.values():
        if path.endswith(language['ext']):
            return language
    return None


def leaks_in(path: str, allow: re.Pattern | None):
    """Returns (line, text, why) for every place exception text reaches a response in one file."""
    language = language_for(path)
    if language is None:
        return []
    with open(path, encoding='utf-8', errors='replace') as handle:
        lines = handle.read().replace('\r\n', '\n').split('\n')

    tainted: set[str] = set()
    found = []
    for number, line in enumerate(lines, start=1):
        if line.lstrip().startswith(language['comment']):
            continue
        # A local's taint ends with the function that declared it; a same-named local in the next
        # function is a different variable.
        if language['function'].match(line):
            tainted.clear()
        carries = bool(language['text'].search(line))
        permitted = bool(language['log'].search(line)) or bool(allow and allow.search(line))

        local = language['local'].match(line)
        if local and carries and not permitted and not language['sink'].search(local.group(2)):
            tainted.add(local.group(1))
            continue
        if not language['sink'].search(line) or permitted:
            continue
        if carries:
            found.append((number, line.strip(), 'exception text'))
            continue
        for name in tainted:
            if re.search(r'\b' + re.escape(name) + r'\b', line):
                found.append((number, line.strip(), f"'{name}', which holds exception text"))
                break
    return found


def scan(roots: list[str], allow: re.Pattern | None) -> list[str]:
    """Returns a report line for every leak under the given roots, skipping tests and build output."""
    report = []
    for root in roots:
        if os.path.isfile(root):
            candidates = [root]
        else:
            candidates = []
            for base, dirs, files in os.walk(root):
                dirs[:] = sorted(d for d in dirs if d not in SKIP)
                candidates += [os.path.join(base, f) for f in sorted(files)]
        for path in candidates:
            if TEST_FILE.search(path.replace(os.sep, '/')):
                continue
            for number, text, why in leaks_in(path, allow):
                report.append(f'{path}:{number}: returns {why}\n      {text}')
    return report


def main(argv: list[str]) -> int:
    """Entry point: parses --allow/--base and reports leaks; returns the exit code."""
    args = argv[1:]
    allow = None
    base = None
    while len(args) >= 2 and args[0] in ('--allow', '--base'):
        if args[0] == '--allow':
            allow = re.compile(args[1]) if args[1] else None
        else:
            base = args[1] or None
        args = args[2:]
    if not args:
        print(__doc__)
        return 2
    report = scan(args, allow)
    if base:
        sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
        import ax_config
        added = ax_config.added_lines(base)
        root = ax_config.repo_root()

        def is_new(entry: str) -> bool:
            path, _, rest = entry.partition(':')
            relative = os.path.relpath(os.path.abspath(path), root).replace(os.sep, '/')
            return int(rest.partition(':')[0]) in added.get(relative, set())
        report = [entry for entry in report if is_new(entry)]
    if report:
        print('Exception text is being returned to the caller. Route the failure through the reporting '
              'helper, which returns a correlation reference and logs the detail:')
        for entry in report:
            print('  ' + entry)
        print(f'\n{len(report)} leak(s).')
        return 1
    print('No exception text is returned to callers.')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
