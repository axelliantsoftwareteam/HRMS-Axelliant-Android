# Copilot instructions

All AI agent instructions for this repository are in **[AIAGENTS.md](../AIAGENTS.md)**.

**Start every session by running `scripts/link-standards.sh`, then read [AIAGENTS.md](../AIAGENTS.md)
and the Axelliant standards pages for the work in `.axelliant/standards/`** (the current wiki,
[online](https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/standards)). The
standards change in the wiki; always work to today's version.

This file must stay at `.github/copilot-instructions.md` — it is the path GitHub Copilot loads
automatically — so it serves as the redirect entrypoint and carries nothing else.

Key rules, repeated here because Copilot loads this file without being asked:

- Business logic belongs in the service layer, never in a controller or a component.
- Functions stay under 150 lines. Enforced in CI.
- Data access goes through the ORM with typed models; raw SQL is parameterised and justified.
- Log through the injected framework logger, never `print` / `console.log` / `Console.*`.
- Never commit a secret. Never hand-edit a generated file. Never edit an applied migration.
- Every log follows the OpenTelemetry record shape (`.axelliant/standards/06-observability/`).
- No AI attribution in code, comments, commit messages, PR titles or PR bodies.
