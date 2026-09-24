#!/usr/bin/env python3
"""Docker is mandatory, and every Dockerfile meets the container standard.

  check-docker.py

Why this exists: "works on my machine" is a defect with a long tail. Every Axelliant repository
runs its dependencies locally through Docker Compose, and every deployable ships as an image built
from a Dockerfile in the repository — so the thing that was tested is the thing that runs, on a
laptop, in CI and in production alike.

Rules (DOCKER_MODE in .axelliant/standards.env):
  service       (default) a compose file at the root that BUILDS AND RUNS THE APPLICATION (every
                developer runs the code in Docker on their machine), AND a Dockerfile in every DOCKER_DEPLOYABLES
                directory (or at least one Dockerfile when none are listed)
  compose-only  libraries, mobile apps and SDKs: a compose file for local dependencies/mocks/tooling
  exempt        requires DOCKER_EXEMPT_REASON; recorded in CI output on every run

Every tracked Dockerfile, whatever the mode:
  FAIL  base image unpinned or `:latest`         a rebuild must produce the same image
  FAIL  final stage runs as root                  a container escape then starts as root
  FAIL  ADD from a URL                            unverified download; use curl + checksum
  FAIL  secret-looking ENV/ARG with a value       it is baked into every layer, for ever
  FAIL  no .dockerignore beside it or at the root .git, .env and node_modules end up in the image
  WARN  no HEALTHCHECK                            fine when the orchestrator probes; say so
  WARN  apt-get install without --no-install-recommends

hadolint runs as well when it is installed (CI installs it). What this does NOT catch: a vulnerable
package inside the image — that is the image scan's job (Trivy in the CI template).
"""
from __future__ import annotations

import os
import posixpath
import re
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402

COMPOSE = ('compose.yaml', 'compose.yml', 'docker-compose.yml', 'docker-compose.yaml')
SECRET = re.compile(r'^(ENV|ARG)\s+([A-Z0-9_]*(PASSWORD|PASSWD|SECRET|TOKEN|API_?KEY|PRIVATE_?KEY|CONNECTION_?STRING|CREDENTIALS?)[A-Z0-9_]*)\s*[= ]\s*(\S+)', re.I)


def tracked() -> list[str]:
    """Returns every tracked file that exists in the working tree."""
    out = subprocess.run(['git', 'ls-files'], capture_output=True, text=True, check=True).stdout
    return [f for f in out.splitlines() if os.path.exists(f)]


def is_dockerfile(path: str) -> bool:
    """True for Dockerfile, Dockerfile.<name> and <name>.Dockerfile."""
    name = os.path.basename(path)
    return name == 'Dockerfile' or name.startswith('Dockerfile.') or name.endswith('.Dockerfile')


def instructions(path: str) -> list[tuple[int, str]]:
    """(line number, instruction) with continuations joined and comments dropped."""
    result, buffer, start = [], '', 0
    with open(path, encoding='utf-8', errors='replace') as handle:
        for number, raw in enumerate(handle, start=1):
            line = raw.rstrip('\n')
            if not buffer and (not line.strip() or line.lstrip().startswith('#')):
                continue
            if not buffer:
                start = number
            if line.rstrip().endswith('\\'):
                buffer += line.rstrip()[:-1] + ' '
                continue
            result.append((start, (buffer + line).strip()))
            buffer = ''
    if buffer:
        result.append((start, buffer.strip()))
    return result


def lint(path: str, files: set[str]) -> tuple[list[str], list[str]]:
    """Returns (failures, warnings) for one Dockerfile against the container standard."""
    fails, warns = [], []
    steps = instructions(path)
    stages, final_user, has_healthcheck = set(), None, False
    for number, text in steps:
        keyword, _, rest = text.partition(' ')
        keyword = keyword.upper()
        where = f'{path}:{number}'
        if keyword == 'FROM':
            final_user = None  # USER resets per stage
            parts = [p for p in rest.split() if not p.startswith('--')]
            image = parts[0] if parts else ''
            if len(parts) >= 3 and parts[1].upper() == 'AS':
                stages.add(parts[2].lower())
            if image.lower() in stages or image == 'scratch' or '$' in image:
                continue
            if '@sha256:' not in image:
                tag = image.rsplit('/', 1)[-1].partition(':')[2]
                if not tag:
                    fails.append(f'{where}: base image "{image}" has no tag (means :latest); pin a version')
                elif tag == 'latest' or tag.endswith('-latest'):
                    fails.append(f'{where}: base image "{image}" uses a moving tag; pin a version')
        elif keyword == 'USER':
            final_user = rest.split(':')[0].strip()
        elif keyword == 'HEALTHCHECK':
            has_healthcheck = True
        elif keyword == 'ADD' and re.search(r'\bhttps?://', rest):
            fails.append(f'{where}: ADD from a URL; download with curl and verify a checksum instead')
        elif keyword == 'RUN' and 'apt-get install' in rest and '--no-install-recommends' not in rest:
            warns.append(f'{where}: apt-get install without --no-install-recommends')
        secret = SECRET.match(text)
        if secret and not secret.group(4).startswith('$'):
            fails.append(f'{where}: {secret.group(1)} {secret.group(2)} has a value baked into the image; '
                         f'pass it at runtime from the secret store')
    if final_user in (None, 'root', '0'):
        fails.append(f'{path}: final stage runs as root; add a non-root USER (e.g. `USER app` or `USER 10001`)')
    if not has_healthcheck:
        warns.append(f'{path}: no HEALTHCHECK (acceptable when the orchestrator probes /health/live; say so in a comment)')
    # git lists paths with '/' on every platform; os.path.join would give 'fe\.dockerignore' on
    # Windows, never match, and fail every Dockerfile outside the repository root.
    directory = posixpath.dirname(path)
    if posixpath.join(directory, '.dockerignore').lstrip('/') not in files and '.dockerignore' not in files:
        fails.append(f'{path}: no .dockerignore beside it or at the repository root')
    return fails, warns


def main() -> int:
    """Entry point: checks the compose/Dockerfile requirements for the repository mode; returns the exit code."""
    root = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True).stdout.strip()
    if root:
        os.chdir(root)
    mode = os.environ.get('DOCKER_MODE', 'service')
    files = set(tracked())
    dockerfiles = sorted(f for f in files if is_dockerfile(f))
    # Stack templates parked in .axelliant/ are linted but do not count as the repository's Dockerfile.
    deployable_dockerfiles = [d for d in dockerfiles if not d.startswith('.axelliant/')]
    fails, warns = [], []

    if mode == 'exempt':
        reason = os.environ.get('DOCKER_EXEMPT_REASON', '').strip()
        if not reason:
            print('FAIL DOCKER_MODE=exempt needs DOCKER_EXEMPT_REASON in .axelliant/standards.env.')
            return 1
        print(f'Docker: exempt — {reason}')
    else:
        if not any(c in files for c in COMPOSE):
            fails.append('no compose.yaml at the repository root; every repository runs its local dependencies through Docker Compose')
        if mode == 'service':
            # Every developer runs the application itself in Docker, not only its dependencies.
            compose = next((c for c in COMPOSE if c in files), None)
            if compose and not re.search(r'^\s+build\s*:', open(compose, encoding='utf-8').read(), re.MULTILINE):
                fails.append(f'{compose} does not build the application (no `build:` service); every developer must be able '
                             f'to run the app in Docker — add it, e.g. under `profiles: ["app"]`')
            deployables = os.environ.get('DOCKER_DEPLOYABLES', '').split()
            for directory in deployables:
                if not any(os.path.dirname(d) == directory.rstrip('/') for d in dockerfiles):
                    fails.append(f'deployable "{directory}" has no Dockerfile')
            if not deployables and not deployable_dockerfiles:
                fails.append('no Dockerfile in the repository; every deployable ships as an image (or set DOCKER_MODE=compose-only for a library/mobile repo)')
        elif mode != 'compose-only':
            fails.append(f'unknown DOCKER_MODE "{mode}" (service | compose-only | exempt)')

    for path in dockerfiles:
        f, w = lint(path, files)
        fails += f
        warns += w
        if shutil.which('hadolint'):
            result = subprocess.run(['hadolint', '--failure-threshold', 'error', path], capture_output=True, text=True)
            if result.returncode:
                fails += [f'hadolint {line}' for line in result.stdout.splitlines() if line.strip()]

    for warning in warns:
        print(f'WARN {warning}')
    for failure in fails:
        print(f'FAIL {failure}')
    if fails:
        print(f'\nDocker standard: {len(fails)} failure(s). See standards/10-containers-and-infra/docker.md.')
        return 1
    print(f'Docker OK: mode {mode}, {len(dockerfiles)} Dockerfile(s) checked.')
    return 0


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main())
