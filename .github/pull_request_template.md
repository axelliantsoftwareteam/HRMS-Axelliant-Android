<!--
Title must be:  type(scope): summary      e.g.  fix(billing): stop double-charging on retry
Enforced by lint-pr-metadata. See .gitmessage for the valid types. No AI product names or attribution.

Replace every italic prompt below with real content, or write "None" where a section genuinely does
not apply. The check fails on unfilled prompts. Open with: gh pr create --body-file <this file, filled>
-->

## Summary

_What changed and why. Explain the reasoning, not the diff. If stacked, say which PR this sits on and which sit on top._

## Ticket

_The ticket this implements (e.g. AXE-412, ENG-88, #123), or "None — <reason>" if the work has no ticket._

## API / Endpoint Changes

_New, changed or removed routes, RPCs, hub methods or events, with method and path. Say whether each is backward compatible. Write "None" if there are none._

## Data Model Changes

_Entity, schema or migration changes, by migration filename. Backward compatible with the previous release? Backfill needed? Write "None" if there are none._

## Tests

_What you covered, not just that tests exist:_

- **Success path:**
- **Edge cases:**
- **Failure modes:**
- **Boundaries:** _(nulls, empty collections, limits, concurrency)_

## Risk & Rollout

_Deployment order, feature flags, config or secret changes, extension allow-lists, and how to roll back. Write "None" if this ships with no special handling._

## Documentation

_Link to the docs(<project>) PR on eng-wiki-standards if documented behaviour changed, or "None". The ledger is updated in this PR._

## Checklist

- [ ] Every public type and member added has a doc comment; new modules have a README
- [ ] Ledger updated in this PR; wiki PR linked above if behaviour changed
- [ ] No secrets, credentials or generated artifacts committed
- [ ] Coverage floor not lowered to make the build pass; new code covered (line and branch)
- [ ] Schema contract, if changed, was written by `scripts/generate-schema-contract.sh`, not by hand
- [ ] No applied migration edited, renamed or deleted
- [ ] Standards violations in touched code fixed (or recorded as standards debt)
