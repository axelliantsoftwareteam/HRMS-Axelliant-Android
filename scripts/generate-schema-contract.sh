#!/usr/bin/env bash
# The ONLY way the schema contract file is ever written.
#
#   scripts/generate-schema-contract.sh           rewrite DB_SCHEMA_CONTRACT
#   scripts/generate-schema-contract.sh --check   what CI runs: fail if the committed file differs
#
# Why this exists: the contract (default database/schema.expected.sql) is the reviewable record of
# what the migrations produce. It is only worth anything if a machine wrote it. Hand-built
# contracts — migrations applied to some other local server, parts pasted from an old file, hunks
# patched from a CI diff — differ in column order, sequences and constraint placement even when
# every object is present, and they have hidden real test failures behind a red contract step.
#
# How: starts a throwaway database in Docker (DB_ENGINE: postgres | mysql | sqlserver | sqlite),
# applies every file matching DB_APPLIED_GLOBS in order (glob order, then name order — so
# migrations MUST be named to sort in application order), and dumps the schema deterministically.
# If migrations are code rather than SQL (EF Core, Alembic, Flyway-Java, Prisma), set
# DB_CONTRACT_APPLY_CMD: it runs on the host with DATABASE_URL pointing at the throwaway database.
#
# No Docker, no contract. Do not approximate this file. Stop and say so in the pull request.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
cd "$AX_REPO_ROOT"

mode="${1:-write}"
case "$DB_ENGINE" in
    postgres)  : "${DB_IMAGE:=postgres:16-alpine}"; port=5432 ;;
    mysql)     : "${DB_IMAGE:=mysql:8.4}"; port=3306 ;;
    sqlserver) : "${DB_IMAGE:=mcr.microsoft.com/mssql/server:2022-CU27-ubuntu-22.04}"; port=1433 ;;
    sqlite)    : "${DB_IMAGE:=keinos/sqlite3:3.46.1}"; port=0 ;;
    *) echo "Unsupported DB_ENGINE '$DB_ENGINE' (postgres, mysql, sqlserver, sqlite)." >&2; exit 2 ;;
esac

command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1 || {
    echo "Docker is not running. The schema contract cannot be generated without it." >&2
    echo "Do NOT write $DB_SCHEMA_CONTRACT by hand; say in the PR that it was not regenerated." >&2
    exit 3
}

# Ordered list of scripts to apply.
scripts=()
shopt -s nullglob
for glob in $DB_APPLIED_GLOBS; do
    while IFS= read -r f; do [ -n "$f" ] && scripts+=("$f"); done < <(printf '%s\n' $glob | grep -E '\.sql$' | LC_ALL=C sort)
done
shopt -u nullglob

password="Contract_$(date +%s)_Aa1!"
name="ax-schema-contract-$$"
work="$(mktemp -d)"
cleanup() { docker rm -f "$name" >/dev/null 2>&1 || true; rm -rf "$work"; }
trap cleanup EXIT

wait_until() { local _attempt; for _attempt in $(seq 1 90); do "$@" >/dev/null 2>&1 && return 0; sleep 2; done; echo "Database did not become ready." >&2; return 1; }

start() {
    case "$DB_ENGINE" in
        postgres)  docker run -d --name "$name" -e POSTGRES_PASSWORD="$password" -e POSTGRES_DB=contract -p "127.0.0.1::$port" "$DB_IMAGE" >/dev/null
                   wait_until docker exec "$name" pg_isready -U postgres -d contract
                   sleep 1; wait_until docker exec "$name" pg_isready -U postgres -d contract ;;
        mysql)     docker run -d --name "$name" -e MYSQL_ROOT_PASSWORD="$password" -e MYSQL_DATABASE=contract -p "127.0.0.1::$port" "$DB_IMAGE" >/dev/null
                   wait_until docker exec "$name" mysql -uroot -p"$password" -e 'SELECT 1' contract ;;
        sqlserver) docker run -d --name "$name" -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD="$password" -p "127.0.0.1::$port" "$DB_IMAGE" >/dev/null
                   wait_until docker exec "$name" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$password" -Q 'SELECT 1'
                   docker exec "$name" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$password" -b -Q 'CREATE DATABASE contract' >/dev/null ;;
        # As the caller's UID: the work directory is 0700 (mktemp), and a container user that does
        # not own it cannot create the database there on Linux (Docker Desktop hides this on macOS).
        sqlite)    docker run -d --name "$name" --user "$(id -u):$(id -g)" --entrypoint sleep -v "$work:/work" "$DB_IMAGE" 3600 >/dev/null ;;
    esac
}

apply_sql() {
    local file="$1"
    case "$DB_ENGINE" in
        postgres)  docker exec -i "$name" psql -q -v ON_ERROR_STOP=1 -U postgres -d contract < "$file" >/dev/null ;;
        mysql)     docker exec -i "$name" mysql -uroot -p"$password" contract < "$file" 2>&1 | grep -v 'Using a password' || true
                   [ "${PIPESTATUS[0]}" -eq 0 ] ;;
        sqlserver) docker cp "$file" "$name:/tmp/apply.sql" >/dev/null
                   docker exec "$name" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$password" -d contract -b -i /tmp/apply.sql >/dev/null ;;
        sqlite)    docker exec -i "$name" sqlite3 -bail /work/contract.db < "$file" ;;
    esac
}

database_url() {
    local host_port
    case "$DB_ENGINE" in
        sqlite) echo "sqlite:///$work/contract.db"; return ;;
    esac
    host_port="$(docker port "$name" "$port/tcp" | head -1 | sed 's/.*://')"
    case "$DB_ENGINE" in
        postgres)  echo "postgresql://postgres:$password@127.0.0.1:$host_port/contract" ;;
        mysql)     echo "mysql://root:$password@127.0.0.1:$host_port/contract" ;;
        sqlserver) echo "sqlserver://sa:$password@127.0.0.1:$host_port?database=contract&encrypt=true&trustServerCertificate=true" ;;
    esac
}

dump() {
    case "$DB_ENGINE" in
        postgres)
            # \restrict / \unrestrict carry a random per-dump key (pg_dump 17.6/16.10+); strip them,
            # along with the version banner, so the same schema always dumps the same bytes.
            docker exec "$name" pg_dump -U postgres -d contract --schema-only --no-owner --no-privileges \
                | grep -Ev '^\\(restrict|unrestrict) |^-- Dumped (from|by) ' ;;
        mysql)
            docker exec "$name" mysqldump -uroot -p"$password" --no-data --skip-comments --skip-dump-date \
                --routines --triggers --events contract 2>/dev/null | sed -E 's/ AUTO_INCREMENT=[0-9]+//' ;;
        sqlserver)
            docker cp "$here/lib/sqlserver-schema.sql" "$name:/tmp/contract.sql" >/dev/null
            docker exec "$name" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$password" -d contract \
                -h -1 -W -b -i /tmp/contract.sql ;;
        sqlite)
            docker exec "$name" sqlite3 /work/contract.db '.schema --indent' ;;
    esac
}

start
for file in "${scripts[@]}"; do
    apply_sql "$file" || { echo "Applying $file failed." >&2; exit 1; }
done
if [ -n "${DB_CONTRACT_APPLY_CMD:-}" ]; then
    DATABASE_URL="$(database_url)" bash -c "$DB_CONTRACT_APPLY_CMD"
fi

if ! dump > "$work/contract.sql" 2> "$work/dump.err"; then
    echo "Dumping the schema failed:" >&2
    cat "$work/dump.err" "$work/contract.sql" >&2
    exit 1
fi

if [ "$mode" = "--check" ]; then
    if [ ! -f "$DB_SCHEMA_CONTRACT" ]; then
        echo "FAIL $DB_SCHEMA_CONTRACT does not exist. Run scripts/generate-schema-contract.sh and commit it."
        exit 1
    fi
    if diff -u "$DB_SCHEMA_CONTRACT" "$work/contract.sql" > "$work/diff.txt"; then
        echo "Schema contract matches the scripts (${#scripts[@]} applied, engine $DB_ENGINE)."
        exit 0
    fi
    echo "FAIL $DB_SCHEMA_CONTRACT does not match what the migrations produce:"
    head -200 "$work/diff.txt"
    echo
    echo "Regenerate it — never hand-edit it:  scripts/generate-schema-contract.sh"
    echo "Merge conflict in this file? Resolve by re-running the script after the merge, not by picking hunks."
    exit 1
fi

mkdir -p "$(dirname "$DB_SCHEMA_CONTRACT")"
cp "$work/contract.sql" "$DB_SCHEMA_CONTRACT"
echo "Wrote $DB_SCHEMA_CONTRACT (${#scripts[@]} scripts applied, engine $DB_ENGINE)."
