#!/usr/bin/env bash
# A pull request names the ticket it works on — or says explicitly that there is none.
#
# Why this exists: when a project is tracked in Jira, Linear, GitHub Issues or Azure Boards, a PR
# that does not name its ticket cannot be found from the ticket, audited against the requirement,
# or rolled into the release notes the client reads. Some work genuinely has no ticket (a typo, a
# dependency bump); saying so explicitly is fine — silence is not.
#
# Passes when TICKET_TRACKER is "none", when a ticket matching TICKET_PREFIX appears in the title,
# body or branch name, or when the body's "## Ticket" section / a "Refs:" line says "None".
#
# Usage: scripts/check-ticket-reference.sh "<pr-title>" "<pr-body>" "<head-branch>"
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$here/lib/config.sh"
. "$here/lib/ticket.sh"

title="${1:-}" body="${2:-}" branch="${3:-}"

if [ "$TICKET_TRACKER" = "none" ] || [ -z "$TICKET_PATTERN" ]; then
    echo "ticket-reference: this repository declares no tracker (TICKET_TRACKER=none)."
    exit 0
fi

if found="$(ticket_in "$title $branch
$body")" && [ -n "$found" ]; then
    echo "ticket-reference: $found. OK."
    exit 0
fi

ticket_section="$(printf '%s\n' "$body" | awk '/^##[[:space:]]+Ticket/{f=1;next} /^##[[:space:]]/{f=0} f' | tr -d '\r')"
if printf '%s\n' "$body" | grep -Eiq "$NO_TICKET_PATTERN" \
   || printf '%s\n' "$ticket_section" | grep -Eiq '^[[:space:]]*(none|n/a|no ticket)'; then
    echo "ticket-reference: PR states it has no ticket. OK."
    exit 0
fi

cat >&2 <<MSG

This repository tracks work in ${TICKET_TRACKER}${TICKET_PREFIX:+ (${TICKET_PREFIX})}, and this pull request names no ticket.

  Put the ticket in the title or branch, e.g.   feat(billing): add proration (${TICKET_EXAMPLE})
  or fill the "## Ticket" section of the body:  ${TICKET_EXAMPLE}
  Working without a project task? Write "None" in the "## Ticket" section, with a reason.

MSG
exit 1
