#!/usr/bin/env bash
# Ticket-reference pattern for this repository, shared by the commit-msg hook and CI.
#
# Source after lib/config.sh. Sets TICKET_PATTERN (extended regex) and TICKET_EXAMPLE, from
# TICKET_TRACKER and TICKET_PREFIX. Kept in one place so the local hook and the PR check can never
# disagree about what a ticket looks like.

# shellcheck disable=SC2034  # TICKET_EXAMPLE / NO_TICKET_PATTERN are consumed by the scripts that source this
ticket_pattern() {
    local keys=""
    if [ -n "${TICKET_PREFIX:-}" ]; then
        keys="$(printf '%s' "$TICKET_PREFIX" | tr -s ' ,' '|' | sed 's/^|//; s/|$//')"
    fi
    case "${TICKET_TRACKER:-none}" in
        jira|linear|other)
            if [ -n "$keys" ]; then
                TICKET_PATTERN="(^|[^A-Za-z0-9])(${keys})-[0-9]+([^0-9]|$)"
                TICKET_EXAMPLE="${TICKET_PREFIX%% *}-123"
            else
                TICKET_PATTERN="(^|[^A-Za-z0-9])[A-Z][A-Z0-9]+-[0-9]+([^0-9]|$)"
                TICKET_EXAMPLE="ABC-123"
            fi ;;
        github)        TICKET_PATTERN="(^|[^A-Za-z0-9&])(#|GH-)[0-9]+([^0-9]|$)"; TICKET_EXAMPLE="#123" ;;
        azure-boards)  TICKET_PATTERN="AB#[0-9]+"; TICKET_EXAMPLE="AB#123" ;;
        *)             TICKET_PATTERN=""; TICKET_EXAMPLE="" ;;
    esac
}
ticket_pattern

# The explicit "this work has no ticket" answer.
# shellcheck disable=SC2034
NO_TICKET_PATTERN='^(Refs|Ticket):[[:space:]]*(none|n/a|no[[:space:]-]?ticket)'

# Print the first ticket reference found in the given text, if any.
ticket_in() {
    [ -n "$TICKET_PATTERN" ] || return 1
    printf '%s' "$1" | grep -Eo "$TICKET_PATTERN" | head -1 | grep -Eo '(AB#|GH-|#)?[A-Z0-9]*-?[0-9]+' | tail -1
}
