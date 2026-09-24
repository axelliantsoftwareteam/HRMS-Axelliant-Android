# Copilot instructions

All AI agent instructions for this repository are in **[AIAGENTS.md](../AIAGENTS.md)**.

This file must stay at `.github/copilot-instructions.md` — it is the path GitHub Copilot loads
automatically — so it serves as the redirect entrypoint and carries nothing else.

Key rules, repeated here because Copilot loads this file without being asked:

- Business logic belongs in the service layer, never in a controller or a component.
- Functions stay under 150 lines. Enforced in CI.
- Data access goes through the ORM with typed models; raw SQL is parameterised and justified.
- Log through the injected framework logger, never `print` / `console.log` / `Console.*`.
- Never commit a secret. Never hand-edit a generated file. Never edit an applied migration.
- No AI attribution in code, comments, commit messages, PR titles or PR bodies.
