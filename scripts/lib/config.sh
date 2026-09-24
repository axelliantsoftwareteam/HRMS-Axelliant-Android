#!/usr/bin/env bash
# Load the repository's standards configuration into the environment.
#
# Source this, do not execute it:
#
#     . "$(dirname "${BASH_SOURCE[0]}")/lib/config.sh"
#
# Why this exists: the guard scripts are shared by every Axelliant repository, and the only
# things that genuinely differ between repositories — branch names, source trees, where the
# migrations live, which files are allowed to be large — are data, not code. They live in
# `.axelliant/standards.env`, which each repository owns. The scripts themselves are managed
# by `sync-standards.sh` and must not be edited locally; a local edit is overwritten on the next
# sync and, worse, silently diverges the gate from every other repository until then.
#
# Every key has a default here, so an unconfigured repository still gets the standard
# behaviour. A value set in the environment wins over the file, which is how CI and tests
# override a single setting without touching the committed configuration.

AX_REPO_ROOT="${AX_REPO_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
AX_CONFIG_FILE="${AX_CONFIG_FILE:-$AX_REPO_ROOT/.axelliant/standards.env}"

_ax_load_config() {
    local line key value
    [ -f "$AX_CONFIG_FILE" ] || return 0
    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        case "$line" in ''|'#'*) continue ;; esac
        key="${line%%=*}"
        value="${line#*=}"
        # Strip one layer of matching quotes.
        case "$value" in
            \"*\") value="${value#\"}"; value="${value%\"}" ;;
            \'*\') value="${value#\'}"; value="${value%\'}" ;;
        esac
        # Only well-formed keys; never eval anything from the file.
        [[ "$key" =~ ^[A-Z][A-Z0-9_]*$ ]] || continue
        # The environment wins over the file.
        if [ -z "${!key+x}" ]; then
            printf -v "$key" '%s' "$value"
            export "${key?}"
        fi
    done < "$AX_CONFIG_FILE"
}
_ax_load_config

: "${INTEGRATION_BRANCH:=uat}"
: "${RELEASE_BRANCH:=main}"
: "${EXEMPT_BRANCHES:=main master uat develop development staging production}"
: "${TICKET_PREFIX:=}"
# Where this project's work is tracked: none | jira | linear | github | azure-boards | other.
# Anything but "none" makes a ticket reference (or an explicit "Refs: none") required on every
# commit and pull request. TICKET_PREFIX lists the project keys (space-separated, e.g. "AXE HRMS").
: "${TICKET_TRACKER:=none}"
: "${TICKET_URL:=}"
# The project's folder in the engineering wiki (projects/<name>/), used by eng-wiki-sync.
: "${ENG_WIKI_PROJECT:=}"
# Extended regex of paths the code-documentation gate skips (entity folders, generated clients).
: "${CODE_DOCS_EXCLUDE:=}"
: "${METHOD_LENGTH_LIMIT:=150}"
# Space-separated "<tree>:<baseline-name>" pairs; baseline at .axelliant/method-length-baseline-<name>.txt
: "${METHOD_LENGTH_TREES:=}"
# Space-separated trees that serve requests. Console logging and exception leaks are checked here.
: "${REQUEST_PATH_TREES:=}"
# Extended regex of paths allowed to write to the console (startup code, CLIs, scripts).
: "${CONSOLE_LOGGING_ALLOW:=(^|/)(Program\.cs|Startup\.cs|main\.py|__main__\.py|manage\.py|cli/|scripts/|tools/|bin/|logging/|otel-logger\.ts|otel_logging\.py|AppLog\.kt)}"
# Extended regex of the repo's reporting helper / domain exception, allowed to carry exception text.
: "${EXCEPTION_REPORTING_ALLOW:=ServiceFailure\.|throw new ServiceException\(|raise ServiceError\(|reportFailure\(|report_failure\(}"
# Space-separated globs of applied database scripts. Anything matching on the base branch is immutable.
: "${DB_APPLIED_GLOBS:=database/schema.sql database/migrations/*.sql}"
: "${DB_ENGINE:=postgres}"
: "${DB_SCHEMA_CONTRACT:=database/schema.expected.sql}"
# DB_SCHEMA_CONTRACT=none opts out (EF Core / Alembic repositories that check their model instead); needs a reason.
: "${DB_SCHEMA_CONTRACT_REASON:=}"
# Space-separated extensions permitted in migrations (PostgreSQL). Empty means read from Terraform.
: "${DB_ALLOWED_EXTENSIONS:=}"
: "${DB_EXTENSIONS_TERRAFORM_FILE:=infra/azure/variables.tf}"
: "${DB_EXTENSIONS_TERRAFORM_VAR:=postgres_allowed_extensions}"
# Extended regex of tracked files allowed to exceed the size limit. Each needs a reason in the PR.
: "${LARGE_FILE_ALLOWLIST:=}"
: "${LARGE_FILE_LIMIT_BYTES:=10485760}"
# service | compose-only | exempt
: "${DOCKER_MODE:=service}"
: "${DOCKER_EXEMPT_REASON:=}"
# Space-separated directories that deploy and therefore need a Dockerfile. Empty means "the repo root or any Dockerfile".
: "${DOCKER_DEPLOYABLES:=}"
# Infrastructure as code: local (infra/ here) | external (INFRA_REPO) | exempt (INFRA_EXEMPT_REASON)
: "${INFRA_MODE:=local}"
: "${INFRA_DIR:=infra}"
: "${INFRA_REPO:=}"
: "${INFRA_EXEMPT_REASON:=}"
# Route prefix documented routes are checked against. Set it EMPTY to disable the route check —
# hence `=` not `:=`, which would treat the deliberate empty value as unset.
: "${DOCS_ROUTE_PREFIX=/api/}"
: "${DOCS_DRIFT_EXCLUDE:=}"
