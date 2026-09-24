# <Project name> — local agent instructions

The company-wide rules are in [AIAGENTS.md](AIAGENTS.md) — read that first. This file adds only what
is specific to this repository. Where the two disagree about something generic, AIAGENTS.md wins.

## Where things are

| | |
|---|---|
| Project documentation (architecture, features, runbooks) | https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/projects/<project> |
| Ledger (update in the same commit as your work) | `docs/ledger.md` |
| Task packs | `docs/tasks/` |
| Ticket tracker | <Jira project AXE / Linear team ENG / GitHub Issues / none> — see `.axelliant/standards.env` |

## Stack

| Layer | Technology and version |
|---|---|
| API | <e.g. .NET 10 minimal API + EF Core 10> |
| UI | <e.g. Angular 20 standalone> |
| Database | <e.g. PostgreSQL 16> |
| Hosting | <e.g. Azure Container Apps> |

## Commands

```bash
docker compose up -d                 # local dependencies
<run the app>                        # e.g. dotnet run --project src/Acme.Api
./scripts/check-before-push.sh       # everything CI runs
```

## Local rules

<Only what genuinely differs here: service boundaries, owners to route contract changes to, locked
decisions, known sharp edges. Link to the wiki folder rather than repeating it.>
