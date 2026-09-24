#!/usr/bin/env python3
"""Every repository's infrastructure is defined as code.

  check-infra-as-code.py

Why this exists: infrastructure created by clicking in a portal cannot be reviewed, reproduced,
rolled back or rebuilt after an incident, and it drifts between UAT and production until "works in
UAT" means nothing. Every Axelliant repository therefore carries the code that creates what it runs
on — Terraform, Bicep, or idempotent Azure CLI scripts — written by a developer or by an AI agent,
reviewed like any other code.

INFRA_MODE in .axelliant/standards.env:
  local     (default) INFRA_DIR (default infra/) holds *.tf, *.bicep / *.bicepparam, or Azure CLI
            scripts (*.sh / *.ps1 that call `az`), plus a README saying how to apply it
  external  the infrastructure lives in another repository: INFRA_REPO names it (URL)
  exempt    requires INFRA_EXEMPT_REASON (documentation-only repositories); printed on every run

Also checked in local mode: Terraform provider lockfile committed; no state files (hygiene also
catches them).

What it does NOT catch: infrastructure that exists in the cloud but is missing from the code. Only a
plan against the real environment shows that drift — run `terraform plan` / `az deployment what-if`
in CI.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib'))
import ax_config  # noqa: E402

AZ_CALL = re.compile(r'(^|[\s;&|(])az\s+[a-z]', re.MULTILINE)


def tracked(prefix: str) -> list[str]:
    """Returns tracked files under the prefix that exist in the working tree."""
    out = subprocess.run(['git', 'ls-files', '--', prefix], capture_output=True, text=True, check=True).stdout
    return [f for f in out.splitlines() if os.path.exists(f)]


def check_local(directory: str) -> tuple[list[str], list[str], str]:
    """Returns (failures, warnings, kinds found) for infrastructure kept in this repository."""
    files = tracked(directory)
    fails, warns = [], []
    terraform = [f for f in files if f.endswith('.tf')]
    bicep = [f for f in files if f.endswith(('.bicep', '.bicepparam'))]
    az_scripts = []
    for f in files:
        if f.endswith(('.sh', '.ps1')):
            with open(f, encoding='utf-8', errors='replace') as handle:
                if AZ_CALL.search(handle.read()):
                    az_scripts.append(f)
    kinds = [k for k, v in (('Terraform', terraform), ('Bicep', bicep), ('Azure CLI', az_scripts)) if v]
    if not kinds:
        fails.append(f'no infrastructure as code in {directory} — add Terraform (*.tf), Bicep (*.bicep) or '
                     f'idempotent Azure CLI scripts, or set INFRA_MODE=external with INFRA_REPO')
    if files and not any(os.path.basename(f).lower() == 'readme.md' for f in files):
        fails.append(f'{directory} has no README.md saying how to authenticate, plan and apply')
    for f in files:
        if re.search(r'\.tfstate(\.backup)?$', f):
            fails.append(f'{f}: Terraform state is committed; use remote state and rotate anything it contains')
    roots = {os.path.dirname(f) for f in terraform}
    for root in sorted(roots):
        text = ''.join(open(f, encoding='utf-8', errors='replace').read() for f in terraform if os.path.dirname(f) == root)
        if 'required_providers' in text and not os.path.exists(os.path.join(root, '.terraform.lock.hcl')):
            warns.append(f'{root}: no .terraform.lock.hcl committed; run terraform init and commit it so providers are pinned')
    return fails, warns, ', '.join(kinds)


def main() -> int:
    """Entry point: checks the repository against its INFRA_MODE; returns the exit code."""
    root = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True).stdout.strip()
    if root:
        os.chdir(root)
    mode = os.environ.get('INFRA_MODE', 'local')
    if mode == 'exempt':
        reason = os.environ.get('INFRA_EXEMPT_REASON', '').strip()
        if not reason:
            print('FAIL INFRA_MODE=exempt needs INFRA_EXEMPT_REASON in .axelliant/standards.env.')
            return 1
        print(f'Infrastructure as code: exempt — {reason}')
        return 0
    if mode == 'external':
        repo = os.environ.get('INFRA_REPO', '').strip()
        if not repo:
            print('FAIL INFRA_MODE=external needs INFRA_REPO (the repository holding this project\'s infrastructure).')
            return 1
        print(f'Infrastructure as code: defined in {repo}')
        return 0
    if mode != 'local':
        print(f'FAIL unknown INFRA_MODE "{mode}" (local | external | exempt)')
        return 1
    directory = os.environ.get('INFRA_DIR', 'infra').rstrip('/')
    fails, warns, kinds = check_local(directory)
    for warning in warns:
        print(f'WARN {warning}')
    for failure in fails:
        print(f'FAIL {failure}')
    if fails:
        print('\nSee standards/10-containers-and-infra/infrastructure-as-code.md.')
        return 1
    print(f'Infrastructure as code OK: {kinds} in {directory}/.')
    return 0


if __name__ == '__main__':
    ax_config.load()
    sys.exit(main())
