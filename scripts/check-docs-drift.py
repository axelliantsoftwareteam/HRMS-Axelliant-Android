#!/usr/bin/env python3
"""Verify the documentation against the repository.

Why this exists: documentation that lies is worse than no documentation — a reader who trusts a
broken link, a route that no longer exists, or the older of two pages on the same topic builds on a
false picture. These drifts are invisible in review, so a machine checks them on every PR.

  check-docs-drift.py                # every check
  check-docs-drift.py links routes   # named checks only

Checks:
  links   every relative markdown link resolves to a real file — case-sensitively, because CI is
          Linux even when the author's laptop is not. Absolute links starting with
          DOCS_LOCAL_URL_PREFIX are treated as links into this repository and checked too
  routes  every `DOCS_ROUTE_PREFIX` route a document mentions (default /api/) is served by the code
          (ASP.NET controllers and minimal APIs, FastAPI, Flask, Express, NestJS, Spring, Next.js)
  titles  no two markdown pages share a top-level title — one page per topic; if two pages answer
          the same question one of them is wrong

DOCS_DRIFT_EXCLUDE (extended regex) removes whole trees from every check — for files that are
copied elsewhere and only resolve at their destination, such as the standards repository's templates.

Historical and design documents may legitimately name routes that are not shipped. List them in
`.axelliant/docs-drift-allowlist`, one `path | reason` per line. An entry without a reason is
rejected: the allowlist is a record of intent, not a place to silence failures.

What it does NOT catch: a route that exists but whose documented request/response shape is wrong
(the OpenAPI contract test's job), or prose that is simply out of date.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402
import urllib.parse

ALLOWLIST_FILE = '.axelliant/docs-drift-allowlist'
CODE_SPAN = re.compile(r'`[^`\n]+`')
FENCE = re.compile(r'^(```|~~~).*?^\1', re.DOTALL | re.MULTILINE)
LINK = re.compile(r'\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)')
PARAM = re.compile(r'\{[^}]*\}|<[^>]*>|:\w+|\[\.{0,3}\w+\]')


def git(*args: str, **kwargs) -> subprocess.CompletedProcess:
    """Runs git with the given arguments and returns the completed process."""
    return subprocess.run(['git', *args], capture_output=True, text=True, **kwargs)


def tracked(suffixes: tuple[str, ...]) -> list[str]:
    """Returns tracked files with the given suffixes that exist in the working tree."""
    out = git('ls-files', '-z', check=True).stdout
    return [f for f in out.split('\0') if f and f.lower().endswith(suffixes) and os.path.exists(f)]


def read(path: str) -> str:
    """Returns a file as text, replacing undecodable bytes."""
    with open(path, encoding='utf-8', errors='replace') as handle:
        return handle.read()


def load_allowlist() -> tuple[dict[str, str], list[str]]:
    """Returns ({path: reason}, problems) from the docs-drift allowlist; entries need a reason."""
    allowed, problems = {}, []
    if not os.path.exists(ALLOWLIST_FILE):
        return allowed, problems
    for number, line in enumerate(read(ALLOWLIST_FILE).splitlines(), start=1):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        path, _, reason = (part.strip() for part in line.partition('|'))
        if not reason:
            problems.append(f'{ALLOWLIST_FILE}:{number}: "{path}" has no reason; every entry needs one')
        allowed[path] = reason
    return allowed, problems


# ------------------------------------------------------------------ links
def exists_case_sensitive(path: str) -> bool:
    """True when the path exists with exactly this case, as it must on Linux CI."""
    if not os.path.exists(path):
        return False
    parent, base = os.path.split(os.path.normpath(path))
    try:
        return base in os.listdir(parent or '.') or base in ('.', '..')
    except OSError:
        return False


def outside_repository(resolved: str) -> bool:
    """True for a resolved relative path that climbs above the repository root."""
    return resolved == '..' or resolved.startswith('..' + os.sep)


def ignored(paths: list[str]) -> set[str]:
    """Returns the subset of paths git ignores (links to local-only files are not broken)."""
    paths = [p.replace(os.sep, '/') for p in paths if not outside_repository(p)]
    if not paths:
        return set()
    result = git('check-ignore', '-z', '--stdin', input='\0'.join(paths))
    if result.returncode not in (0, 1):
        raise SystemExit('git check-ignore failed:\n' + result.stderr.strip())
    return {p for p in result.stdout.split('\0') if p}


def strip_code(text: str) -> str:
    """Blanks fenced code blocks and inline code so they are not scanned for links."""
    text = FENCE.sub(lambda m: '\n' * m.group(0).count('\n'), text)
    return CODE_SPAN.sub(lambda m: ' ' * len(m.group(0)), text)


def check_links(files: list[str]) -> list[str]:
    """Returns a failure for every relative link that does not resolve."""
    candidates = []
    for path in files:
        for match in LINK.finditer(strip_code(read(path))):
            target = match.group(2).strip().strip('<>')
            # Absolute links into THIS repository (e.g. the standards wiki linking to itself from
            # files that are copied into other repositories) are verified as local paths.
            local_prefix = os.environ.get('DOCS_LOCAL_URL_PREFIX', '')
            if local_prefix and target.startswith(local_prefix):
                target = '/' + target[len(local_prefix):]
            elif re.match(r'^[a-z][a-z0-9+.-]*:', target, re.I) or target.startswith('#'):
                continue
            target = target.split('#')[0].split('?')[0]
            if not target:
                continue
            base = '.' if target.startswith('/') else os.path.dirname(path)
            resolved = os.path.normpath(os.path.join(base, urllib.parse.unquote(target.lstrip('/'))))
            if not outside_repository(resolved) and not exists_case_sensitive(resolved):
                candidates.append((path, target, resolved))
    skip = ignored([c[2] for c in candidates])
    return [f'{p}: broken link -> {t}' for p, t, r in candidates if r.replace(os.sep, '/') not in skip]


# ----------------------------------------------------------------- routes
def normalise(route: str) -> str:
    """Normalises a route for comparison: no query or trailing slash, parameters as {}, lower case."""
    route = route.split('?')[0].split('#')[0].strip().rstrip('/.,;:)')
    route = PARAM.sub('{}', route).lower()
    return '/' + '/'.join(s for s in route.split('/') if s)


def join(prefix: str, suffix: str) -> str:
    """Joins a route prefix and suffix; a suffix starting with ~/ is absolute."""
    if suffix.startswith('~/'):
        return suffix[1:]
    return '/' + '/'.join(s.strip('/') for s in (prefix, suffix) if s and s.strip('/'))


def served_routes() -> set[str]:
    """Returns every route the code serves, across the supported frameworks, normalised."""
    routes: set[str] = set()
    sources = {}
    for path in tracked(('.cs', '.py', '.ts', '.js', '.java', '.kt', '.mjs')):
        if re.search(r'(^|/)(tests?|__tests__|node_modules|dist|bin|obj)/|\.(spec|test)\.', path):
            continue
        sources[path] = read(path)
    routes |= _aspnet({p: src for p, src in sources.items() if p.endswith('.cs')})
    for path, src in sources.items():
        routes |= _python(src) | _node(src) | _spring(src)
        nxt = re.search(r'(?:^|/)(?:src/)?(app|pages)/(api/.*?)(?:/route|/index)?\.(?:ts|js|tsx|jsx)$', path)
        if nxt:
            routes.add('/' + re.sub(r'\(\w+\)/', '', nxt.group(2)))
    return {normalise(r) for r in routes}


CS_CLASS = re.compile(r'\bclass\s+(\w+?)(Controller)?\b')
CS_CLASS_ROUTE = re.compile(r'\[Route\("([^"]*)"\)\]((?:\s*\[[^\]]*\]|\s*///[^\n]*)*)\s*(?:public\s+|internal\s+)?(?:sealed\s+|abstract\s+|partial\s+|static\s+)*class\s+(\w+)')
CS_ACTION = re.compile(r'(?:\[|,\s*)(?:Http(?:Get|Post|Put|Delete|Patch)|Route)\("([^"]*)"\)')
CS_CONST = re.compile(r'\bconst\s+string\s+(\w+)\s*=\s*"([^"]*)"')


def _aspnet(sources: dict[str, str]) -> set[str]:
    """Controllers (including partial classes split across files), minimal APIs and hubs."""
    out: set[str] = set()
    prefixes: dict[str, str] = {}
    constants: dict[str, str] = {}
    # Pass 1: class-level route templates and string constants, from every file. A partial
    # controller declares its [Route] in one file and its actions in others.
    for src in sources.values():
        for m in CS_CLASS_ROUTE.finditer(src):
            prefixes[m.group(3)] = m.group(1)
        for m in CS_CLASS.finditer(src):
            for c in CS_CONST.finditer(src):
                constants[f'{m.group(1)}{m.group(2) or ""}.{c.group(1)}'] = c.group(2)
    # Pass 2: actions, resolved against their class's template.
    for src in sources.values():
        cls = CS_CLASS.search(src)
        if not cls or not cls.group(2):
            continue
        name = cls.group(1) + 'Controller'
        template = prefixes.get(name, 'api/[controller]').replace('[controller]', cls.group(1))
        class_route_spans = {m.start(1) for m in CS_CLASS_ROUTE.finditer(src)}
        actions = [m.group(1) for m in CS_ACTION.finditer(src) if m.start(1) not in class_route_spans]
        if '[action]' in template:
            # Action-name routing: every public method is reachable at its own name.
            base = template.replace('/[action]', '').replace('[action]', '')
            methods = re.findall(r'public\s+(?:async\s+|virtual\s+|override\s+)*[\w<>\[\],.? ]+\s+(\w+)\s*\(', src)
            out |= {join(base, m) for m in methods}
            template = base
        out |= {join(template, a) for a in actions} | {join(template, '')}
    for src in sources.values():
        groups = {m.group(1): m.group(2) for m in re.finditer(r'(\w+)\s*=\s*\w+\.MapGroup\("([^"]*)"\)', src)}
        for m in re.finditer(r'(\w+)\.Map(?:Get|Post|Put|Delete|Patch|Methods|Hub<[^>]*>)\(\s*(?:"([^"]*)"|([\w.]+))', src):
            path = m.group(2) if m.group(2) is not None else constants.get(m.group(3) or '', '')
            if path:
                out.add(join(groups.get(m.group(1), ''), path))
    return out


def _python(src: str) -> set[str]:
    out = set()
    prefix = re.search(r'APIRouter\([^)]*prefix\s*=\s*["\']([^"\']*)', src)
    bp = re.search(r'Blueprint\([^)]*url_prefix\s*=\s*["\']([^"\']*)', src)
    base = (prefix or bp).group(1) if (prefix or bp) else ''
    for m in re.finditer(r'@\w+\.(?:get|post|put|delete|patch|route|api_route|websocket)\(\s*["\']([^"\']*)', src):
        out.add(join(base, m.group(1)))
    return out


def _node(src: str) -> set[str]:
    out = set()
    for m in re.finditer(r'\b(?:app|router|server|fastify|\w+Router)\.(?:get|post|put|delete|patch|all|use)\(\s*[\'"`](/[^\'"`]*)', src):
        out.add(m.group(1))
    ctrl = re.search(r"@Controller\(\s*['\"]([^'\"]*)", src)
    for m in re.finditer(r"@(?:Get|Post|Put|Delete|Patch|All)\(\s*(?:['\"]([^'\"]*))?", src):
        out.add(join(ctrl.group(1) if ctrl else '', m.group(1) or ''))
    return out


def _spring(src: str) -> set[str]:
    out = set()
    cls = re.search(r'@RequestMapping\(\s*(?:value\s*=\s*|path\s*=\s*)?"([^"]*)"[\s\S]{0,200}?\bclass\b', src)
    for m in re.finditer(r'@(?:Get|Post|Put|Delete|Patch|Request)Mapping\(\s*(?:value\s*=\s*|path\s*=\s*)?"([^"]*)"', src):
        if cls and m.start() == cls.start():
            continue
        out.add(join(cls.group(1) if cls else '', m.group(1)))
    for m in re.finditer(r'\b(?:get|post|put|delete|patch|route)\(\s*"(/[^"]*)"', src):  # Ktor
        out.add(m.group(1))
    return out


def route_matches(documented: str, served: set[str]) -> bool:
    """True when a documented route is served, allowing templates, families and mount prefixes."""
    if documented in served:
        return True
    # A served template segment ({id}) matches any documented value (42, {orderId}, nightly-run).
    parts = documented.split('/')
    for route in served:
        pattern = route.split('/')
        if len(pattern) == len(parts) and all(p == '{}' or p == d for p, d in zip(pattern, parts)):
            return True
    # A route family ("the /api/SubPlan endpoints") is documented correctly if something is served
    # under it.
    if any(route.startswith(documented + '/') for route in served):
        return True
    # Mount prefixes (app.use('/api', router), include_router(prefix=...)) are often in another
    # file, so a served route matching the documented route's tail at a segment boundary counts.
    for route in served:
        literal = [s for s in route.split('/') if s and s != '{}']
        if literal and documented.endswith(route) and documented[-len(route) - 1:-len(route)] in ('', '/'):
            return True
    return False


def check_routes(files: list[str], allowed: dict[str, str]) -> list[str]:
    """Returns a failure for every documented route under the prefix that no code serves."""
    prefix = os.environ.get('DOCS_ROUTE_PREFIX', '/api/')
    if not prefix:
        return []
    served = served_routes()
    if not served:
        print('routes: no served routes found in this repository; route check skipped.')
        return []
    pattern = re.compile(r'(?<![\w/.])(' + re.escape(prefix) + r'[A-Za-z0-9_\-/{}:<>\[\].*]*)')
    failures = []
    for path in files:
        if path in allowed:
            continue
        for number, line in enumerate(read(path).splitlines(), start=1):
            for match in pattern.finditer(line):
                documented = normalise(match.group(1))
                literal = [seg for seg in documented[len(normalise(prefix)):].split('/') if seg and seg != '{}']
                # Bare prefixes, wildcards and pure templates ("/api/<context>/<resource>") describe
                # a shape, not an endpoint.
                if not literal or '*' in match.group(1) or '...' in match.group(1):
                    continue
                if not route_matches(documented, served):
                    failures.append(f'{path}:{number}: documents {match.group(1)} but no code serves it')
    return failures


# ----------------------------------------------------------------- titles
def title_scope(path: str) -> str | None:
    """Pages compete for a title only within one documentation tree.

    Each project folder in a wiki (`projects/<name>/`) is its own tree — every project rightly has a
    page called "Architecture" — and the template folder is excluded. Everything else is one tree.
    """
    parts = path.split('/')
    if parts[0] == 'projects' and len(parts) > 2:
        return None if parts[1].startswith('_') else f'projects/{parts[1]}'
    return 'repository'


def check_titles(files: list[str], allowed: dict[str, str]) -> list[str]:
    """Returns a failure for every page whose title repeats another page in the same tree."""
    seen: dict[tuple[str, str], str] = {}
    failures = []
    for path in files:
        scope = title_scope(path)
        if scope is None or path in allowed or os.path.basename(path).upper() in ('CHANGELOG.MD',):
            continue
        match = re.search(r'^# (.+)$', strip_code(read(path)), re.MULTILINE)
        if not match:
            continue
        key = (scope, re.sub(r'\s+', ' ', match.group(1)).strip().lower())
        if key in seen:
            failures.append(f'{path}: same title as {seen[key]} ("{match.group(1).strip()}") — one page per topic')
        else:
            seen[key] = path
    return failures


def main(argv: list[str]) -> int:
    """Entry point: runs the requested checks and reports drift; returns the exit code."""
    root = git('rev-parse', '--show-toplevel').stdout.strip()
    if root:
        os.chdir(root)
    wanted = set(argv[1:]) or {'links', 'routes', 'titles'}
    exclude = os.environ.get('DOCS_DRIFT_EXCLUDE', '')
    files = [f for f in tracked(('.md', '.mdx')) if not re.search(r'(^|/)(node_modules|vendor|\.github/ISSUE_TEMPLATE)/', f)
             and not (exclude and re.search(exclude, f))]
    allowed, failures = load_allowlist()
    if 'links' in wanted:
        failures += check_links(files)
    if 'routes' in wanted:
        failures += check_routes(files, allowed)
    if 'titles' in wanted:
        failures += check_titles(files, allowed)
    if failures:
        print('Documentation drift:')
        for failure in failures:
            print('  ' + failure)
        print(f'\n{len(failures)} problem(s). Fix the document (or the link), or — for a genuinely historical '
              f'page — add it to {ALLOWLIST_FILE} with a reason.')
        return 1
    print(f'Documentation OK: {len(files)} markdown files, checks: {", ".join(sorted(wanted))}.')
    return 0


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main(sys.argv))
