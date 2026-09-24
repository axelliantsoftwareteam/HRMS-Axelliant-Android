# Axelliant AI Agent Operating Procedures

**This file is canonical.** Every Axelliant repository carries a copy, and every AI coding agent
— Claude, Copilot, Gemini, Cursor, Codex, whatever comes next — reads it at the start of every
session. The tool-specific files (`CLAUDE.md`, `GEMINI.md`, `cursor.md`,
`.github/copilot-instructions.md`, `AGENTS.md`) are pointers to this one. One source of truth;
five copies of the same rules drift within a month.

It is also written for humans. Nothing here is advice for machines only.

> **Repository-local rules** live in the repository's own `AIAGENTS.local.md` (stack versions,
> service names, ports, the ticket prefix). Where the two disagree about something generic,
> this file wins; where they disagree about something local, the local file wins and this file
> should not have had an opinion.

---

## Quick reference

| Decision | Answer |
|---|---|
| Which branch do I start from? | `uat` — always, after `git fetch`. Never `main`, never whatever is checked out |
| How do I name the branch? | `<type>/<short-description>` or `<type>/<TICKET-123>-<description>`, lowercase |
| Commit message | `type(scope): summary` — imperative, ≤72 chars, no trailing period |
| Ticket? | If `TICKET_TRACKER` in `.axelliant/standards.env` is not `none`, every commit carries `Refs: <ID>` (or `Refs: none` when the user confirms there is no project task) and every PR names its ticket |
| Documentation in code? | **Every public type and member you add has a doc comment**: what, parameters, return, errors, side effects. Every module has a README |
| PR title | The same convention. `lint-pr-metadata` rejects anything else before a build runs |
| PR body | Built from `.github/pull_request_template.md`, every section filled or "None" |
| Where does business logic go? | The service/application layer. Never a controller, never a component, never a migration |
| How do I read or write data? | Through the ORM with typed models. Raw SQL only where the ORM genuinely cannot, always parameterised, always with a comment saying why |
| How do I log? | The framework's logger, emitting the **OpenTelemetry log record** (timestamp, severity, constant body, trace/span ids, `correlation.id`, semantic-convention attributes) — as OTLP or as one JSON object per line on stdout. Even a console-only script uses that shape. Never `print`/`console.log`/`Console.WriteLine` as a logger |
| What does the caller see when something fails? | A correlation reference and a safe message. Never an exception string, never a stack trace |
| Where do secrets live? | The platform secret store. Never a file, never a commit, never a log line |
| How long may a function be? | Under 150 lines. This is a hard, enforced limit |
| How much test coverage? | The repo's floor is a ratchet that only rises. New and changed code: 80% line **and** branch |
| Do I need a Dockerfile? | Yes. Every deployable ships as an image, and `compose.yaml` builds and runs the app — **every developer runs the code in Docker on their machine** |
| Infrastructure? | **Every repo has its infrastructure as code** in `infra/` — Terraform, Bicep or idempotent Azure CLI scripts. If the work needs a resource, you write the code for it |
| Services talking to each other? | gRPC for synchronous internal calls, a broker (Service Bus/Kafka/RabbitMQ) for anything that can wait, REST+OpenAPI at the edge, SignalR/WebSockets/SSE to browsers. See [choosing a transport](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/08-api-and-contracts/choosing-a-transport.md) |
| Where do this project's docs live? | In the wiki: `projects/<project>/` in eng-wiki-standards. The repo keeps only its README, ledger and task packs |
| Can I edit an applied migration? | No. Ever. Add a new one |
| Found code that breaks a standard? | Fix it — same PR if you touch it, separate commit/PR if you don't, ledger entry if it is too big. Never walk past it |
| PR labels? | Applied automatically by the `pr-labeler` job. Every repo **must** run it |
| Finished a piece of work? | Update the repo's ledger **in the same commit** |

---

## Before you write anything

1. **Read this file and the repo's `AIAGENTS.local.md`.** Then read the repo's ledger
   (`docs/07-reference/master-ledger.md` or the path the local file names). The ledger is the
   record of what exists; starting without it means re-implementing something that shipped last
   week.
2. **`git fetch origin` and branch from `uat`.** Never continue on whatever branch happens to be
   checked out. Stale checkouts have the wrong layout and dead patterns, and changes made there
   strand.
3. **Find the ticket.** If the repo declares a tracker (`TICKET_TRACKER` in
   `.axelliant/standards.env`), ask the user for the ticket id before starting unless it is in the
   request or the branch name. The `commit-msg` hook prompts humans; it cannot prompt you, so every
   commit you write carries `Refs: <ID>`, or `Refs: none` when the user says the work has no ticket —
   working without a ticket is normal and fine; the answer just has to be explicit.
   Name the branch after it: `feature/<ID>-<description>`.
4. **Say what you are about to do** before doing it, in one short paragraph: the change, the
   files, the tests, the gates it has to pass. For anything larger than a single file, write it
   into the repo's plan location first.
5. **Look at the code around you.** Match its idiom, its naming, its comment density, its test
   style. A correct change written in a foreign style is still a cost to everyone after you.

---

## Project documentation lives in the wiki (MANDATORY)

Every project has a folder in the engineering wiki —
[`projects/<project>/`](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/projects/README.md) in `eng-wiki-standards` — holding its
architecture, features, operations runbooks and reference. The code repository keeps only what
must travel with the code:

| Stays in the repository | Lives in `projects/<project>/` in the wiki |
|---|---|
| `README.md` (get running in 15 minutes, link to the wiki) | Architecture, domain model, locked decisions |
| `AIAGENTS.local.md` (stack, ports, commands, local rules) | Feature pages (shipped behaviour only) |
| The ledger (`docs/07-reference/master-ledger.md` or as the local file says) | Operations: environments, deployment, runbooks |
| Small work instructions and task packs | API and schema reference, ADRs, glossary |

**When your change reaches `main` and alters anything the wiki describes, you update the wiki.**

1. In the same piece of work, open a PR against `eng-wiki-standards` that updates
   `projects/<project>/`: the feature page that is now wrong, the runbook that changed, the ADR
   for a decision you took. Title: `docs(<project>): <what changed>`. Link the code PR in the body.
2. Link the wiki PR from the code PR. A code PR that changes documented behaviour without a wiki
   PR is not done.
3. Separately, the `eng-wiki-sync` workflow opens a PR on every push to `main` appending the merged
   changes to `projects/<project>/CHANGELOG.md`. That keeps the record complete; it does not
   replace step 1, because a changelog line is not a corrected page.

---

## Leave every repository better than you found it (MANDATORY)

Most Axelliant repositories predate these standards, and some belong to clients who wrote them
before we arrived. **When you find code that breaks a standard, you fix it.** Walking past a
violation is choosing to leave it for the next person — and the next agent will copy it, because
it looks like the local idiom.

How, so that fixing does not turn every PR into an unreviewable rewrite:

1. **Code you are changing anyway** — bring it up to standard in the same PR. If you touch a
   function, it leaves under 150 lines, logging through the logger, with no exception text
   returned, with tests covering what you changed. This is not optional.
2. **Violations you find nearby but are not otherwise changing** — fix them in a **separate
   commit** (`refactor:`, `fix:`, `test:` …) so the reviewer can read the feature and the cleanup
   apart. If the cleanup is more than roughly 200 changed lines, put it in its own PR, stacked on
   or beside the feature.
3. **Violations too large to fix now** (a 3,000-line service, a missing test harness, a secret
   in history) — do not ignore them. Record each in the repository's ledger under *Standards debt*
   with file, rule broken, and a rough size, or open an issue labelled `standards-debt`. Say in
   your PR that you found it.
4. **Never "fix" a violation by weakening the gate** — no baseline bump, no allowlist entry, no
   lowered floor. Baselines and allowlists only ever shrink.
5. **A secret found anywhere is an incident, not a cleanup.** Stop, tell the user, and do not
   paste it into a commit, a PR, a log or a chat. It must be rotated; removing it from the file
   does not un-publish it.
6. **Client repositories:** fix within the engagement's scope. If a fix would change behaviour a
   client depends on, write it up and ask first; standards never override a signed contract.

---

## NEVER

These are absolute. Each one is here because its absence cost real time; the incident is in
[the standards wiki's lessons learned](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/12-ai-agents/lessons-learned.md).

- **Never commit a secret, credential, token, private key or connection string.** Not "temporarily",
  not in a test fixture, not in a comment. A secret that reached a remote is burned and must be
  rotated, not deleted.
- **Never start work without fetching and branching from `uat`.**
- **Never open a pull request into the release branch from anything but `uat`.** The
  `promotion-source` gate rejects it. Retarget the PR; renaming the branch does not help.
- **Never open a second pull request off `uat` while another from the same work is open.**
  Stack it on the first (see [Stacked pull requests](#stacked-pull-requests)).
- **Never write a function of 150 lines or more, or add a line to one that already is.** Enforced
  by `check-method-length.sh` in the pre-commit hook and in CI, in every language. A function that
  long cannot be read in one sitting and cannot be reached from a test except through everything
  around it — which is how untestable code and undetected defects get in. Split it into named
  steps; usually "decide whether this may go ahead", "do it", "record what happened".
- **Never raise a method-length or file-length baseline to get a commit through.** The baseline
  records what already existed and is meant to reach zero. It may only shrink.
- **Never lower a coverage floor, and never add code that drops line or branch coverage below the
  floor.** If a change cannot be tested, that is a reason to change the design, not the number.
- **Never open a pull request whose changed code is not covered by its own tests.** Line and branch
  coverage are not the same thing: a test that walks one side of an `if` covers the line and proves
  nothing about the other side, which is usually where the defect is. Cover the success path, both
  sides of every branch, the failure modes (database refuses, provider down, input rejected) and
  the boundaries (null, empty, zero, the limit, one past it). "There are tests in the repository"
  is not the standard; the code you wrote is.
- **Never let exception text reach an API caller.** Failures go through the repo's one reporting
  helper, which returns a correlation reference to the caller and sends the detail to the logger
  and APM. A sentence deliberately written for a human is thrown as an explicit, documented
  exception type.
- **Never log through `print`, `console.log`, `Console.WriteLine`, `System.out` or `NSLog` on a
  request path.** Use the injected framework logger. **Every log line — even console-only — is an
  OpenTelemetry-shaped structured record** ([logging standard](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/06-observability/logging.md)):
  constant `body`, values in semantic-convention attributes, trace context and `correlation.id`
  attached automatically, no secrets or personal data.
- **Never edit, delete or rename an applied database script** — the baseline schema included. The
  migration runner checksums them and refuses to start when one changes. Add a new migration.
- **Never hand-edit a generated file** — the schema contract, an OpenAPI dump, a lockfile, a
  coverage baseline. Run the generator and commit exactly what it writes. If the generator cannot
  be run, stop and say so; do not approximate it.
- **Never create or change infrastructure by hand** (portal clicks, one-off CLI commands nobody
  committed). Write it in `infra/` — Terraform, Bicep or an idempotent Azure CLI script — so it can be
  reviewed, reproduced and rebuilt.
- **Never hardcode configuration.** Bind it, validate it at startup, and fail fast when it is
  missing. A service that starts with a missing setting and fails on the first request has moved
  a deploy-time error into a customer-facing one.
- **Never silence a guardrail instead of fixing what it found.** A suppression, an allowlist entry
  or a baseline line is a statement that you understood the finding and it is genuinely not a
  defect — and it must carry that reason in a comment. Anything else hides a fault from the next
  person.
- **Never check for a race after writing.** Lock the parent row, re-read under the lock, then
  decide and write.
- **Never swallow an exception silently.** Handle it, or let it rise. An empty catch block is a
  defect with a hiding place.
- **Never add AI attribution.** No `Co-Authored-By` trailers naming an assistant, no "Generated
  with" footers, no AI product names in code, comments, branch names, commit messages, PR titles
  or PR bodies. The only permitted occurrence is the filename `CLAUDE.md`. The `commit-msg` hook
  rejects the rest.
- **Never add a public type or member without a doc comment.** What it does, its parameters and
  return, what it throws, its side effects. Code documentation is not optional here.
- **Never report a red check as green.** If something does not pass, say so plainly, with the
  output. A failure reported as success is worse than the original defect, because it spends
  someone else's day.

---

## Branch freshness

Before starting any work: `git fetch origin`, then branch from `uat`.

The `pre-push` hook runs the fast gates (`scripts/check-before-push.sh --fast`) and blocks a push that fails one, or that comes from a branch behind its base. Rebase first
(`git pull --rebase origin uat`). Stacked branches do not need an escape hatch — record the
parent with `git config branch.<name>.stackParent <parent>` and the hook checks against that;
once a PR exists it reads the PR's base automatically.

Escape hatch, for the rare deliberate case: `SKIP_BASE_SYNC_CHECK=1 git push`.

## Stacked pull requests

If you have, or can see, more than one open PR from the same piece of work, **stack them** rather
than opening parallel PRs off `uat`. Parallel PRs each carry their own copy of `uat`, each
regenerate any generated contract, each conflict with the next one to merge, and each need a fresh
CI run after every merge.

1. The first PR targets `uat`. Each next PR targets the **previous PR's branch**
   (`gh pr create --base <previous-branch>`).
2. Record the parent: `git config branch.<branch>.stackParent <previous-branch>`.
3. Change a lower branch → merge it upward into every branch above it, then regenerate any
   generated contract on each branch that has a migration.
4. Merge top-down — each PR into the branch below it — and finally the bottom PR into `uat`.
   One merge to `uat` lands the whole stack with one CI run and one contract.
5. Say in every PR body which PR it is stacked on and which sit on top of it.

## Branching and promotion

`uat` is the integration branch: every change lands there first, and a PR into `uat` needs no
approval — the required status checks are the gate. `main` is the protected release branch; it
accepts pull requests **from `uat` and nothing else**, enforced by the `promotion-source` gate.

`main` is updated by a single promotion pull request from `uat`, merged **with a merge commit,
never a squash**. Squashing a promotion leaves `uat` and `main` without shared history, so every
later promotion re-proposes commits that already shipped.

Branch names: `<type>/<short-description>`, optional uppercase ticket key before the description.

```
feature/warehouse-picking
fix/chat-product-search
docs/api-contract-refresh
chore/AXE-412-bump-deps
hotfix/po-serial-crash
```

Types: `feature fix bugfix hotfix chore docs refactor test perf ci build release infra`.
No bare names (`bug-verification`), no personal long-lived branches (`main_ali_v2`).
Exempt: `main`, `master`, `uat`, `develop`, `staging`, `production`.
Escape hatch: `SKIP_BRANCH_NAME_CHECK=1 git push`.

Full detail: [branching and promotion](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/01-ways-of-working/branching-and-promotion.md).

---

## The gates, and how to pass them locally

One command runs everything CI will run:

```bash
./scripts/check-before-push.sh          # macOS / Linux / Git Bash
pwsh ./scripts/check-before-push.ps1    # Windows PowerShell
```

Install the hooks once per clone — they are not installed by cloning:

```bash
./scripts/install-git-hooks.sh
```

| Gate | Catches | Runs |
|---|---|---|
| `check-repo-hygiene.sh` | tracked build output, secret-shaped files, files over 10 MB | hook + CI |
| `check-branch-name.sh` | branch names outside the convention | pre-push + CI |
| `check-promotion-source.sh` | a PR into the release branch from anything but `uat` | CI |
| `check-method-length.sh` | functions ≥150 lines, and growth against the baseline | hook + CI |
| `check-file-length-budget.sh` | files growing past their budget | hook + CI |
| `check-exception-leaks.sh` | raw exception text on an API response path | CI |
| `check-console-logging.sh` | `print` / `console.log` / `Console.*` on a request path | CI |
| `check-applied-scripts.py` | an edit, rename or delete of an applied migration | CI |
| `check-schema-drift.py` | committed schema contract not matching the scripts | CI |
| `check-migration-extensions.py` | a migration needing a database extension nobody allow-listed | CI |
| `check-docs-drift.py` | a broken relative link, or a documented route that does not exist | CI |
| `check-naming-conventions.py` | identifiers outside the language's casing rules | CI |
| `check-docker.py` | no compose file, a deployable with no Dockerfile, an unpinned base image, a root container, a baked-in secret | CI |
| `lint-pr-metadata` | a PR title or body outside the convention | CI |
| `check-code-docs.py` | a public declaration added without a doc comment | hook + CI |
| `check-ticket-reference.sh` + `commit-msg` | a commit or PR with no ticket and no explicit `none`, where the repo has a tracker | hook (asks) + CI |
| `pr-labeler` | (not a gate — applies `type:`, `area:`, `size:` and `breaking` labels automatically) | CI, every PR |
| `check-infra-as-code.py` | no Terraform/Bicep/Azure CLI in `infra/`, no infra README, committed state | CI |
| `check-coverage.py` | coverage below the floor, or a floor lowered | CI |
| `sync-standards.sh --check` | a company-wide managed file edited locally | CI |

Gates judge **what your change adds** against a baseline of what already existed; a promotion PR
(`uat` → `main`) is judged on everything since the last release. So a legacy repository passes today,
and every new line you write must meet the standard.

A guard nobody tests is a guard that silently stops guarding: **every guard script has its own
unit test**, and those tests run in CI.

---

## Definition of done

A change is done when **all** of these are true. Not most.

- [ ] Formatter clean, linter clean at zero warnings, type check clean
- [ ] Build succeeds and adds no new warnings
- [ ] Tests pass, and the changed code is covered by its own new tests — success path, both sides
      of every branch, failure modes, boundaries
- [ ] Coverage floor not lowered; the ratchet moved up if the change allows it
- [ ] No secret, no generated artifact, no file over 10 MB added to git
- [ ] Every generated file regenerated by its generator, not by hand
- [ ] A Dockerfile exists for anything deployable, the image runs as a non-root user, and the app runs
      under `docker compose --profile app up` — not only from the IDE
- [ ] Any infrastructure the change needs is in `infra/` as Terraform, Bicep or Azure CLI — never portal clicks
- [ ] Logs follow the OpenTelemetry record shape (even console output), carry trace context and a
      correlation id, and contain no secrets or personal data; no raw exception text leaves the API
- [ ] Every public type and member added has a doc comment; new modules have a README
- [ ] Ticket referenced on every commit and the PR (or explicitly `none`) where the repo has a tracker
- [ ] Documentation updated — the page that is now wrong, not a new page. Project docs are in the
      wiki: open the `docs(<project>): …` PR against `eng-wiki-standards` and link it
- [ ] **The repo's ledger updated in the same commit**: what changed, why, new status, date
- [ ] Commit messages and PR title follow the convention, with no AI attribution
- [ ] PR body built from the template with every section filled
- [ ] Standards violations in code you touched are fixed; ones you found elsewhere are fixed in a
      separate commit/PR or recorded as standards debt
- [ ] PR carries its automatic labels (type, area, size) — if the labeler did not run, fix the workflow
- [ ] Branch is fresh against `uat`

---

## Writing the pull request

`lint-pr-metadata` fails a PR whose title is freeform or whose body still holds the template's
italic prompts — before any build or test runs, so it wastes a full CI cycle.

1. Title: `type(scope): summary`, matching
   `^(feat|fix|docs|test|refactor|perf|chore|ci|build|infra|style|revert)(\([a-z0-9._/-]+\))?!?: .{10,}$`,
   whole title ≤100 characters, no trailing period.
2. Body: copy `.github/pull_request_template.md` **verbatim**, replace every italic prompt with
   real content, or the literal word "None" where a section genuinely does not apply.
3. Open with `gh pr create --title "..." --body-file <path>`. Never `--body "..."` typed freehand.

Required sections: `## Summary`, `## API / Endpoint Changes`, `## Data Model Changes`,
`## Tests` (with `**Success path:**` / `**Edge cases:**` / `**Failure modes:**` /
`**Boundaries:**`), `## Risk & Rollout`, `## Checklist`.

---

## Planning larger work

Anything beyond a single file, before implementing:

1. Write a short plan in the repo's plan location: problem statement, approach, affected files,
   verification checklist, what could go wrong.
2. Update it as work progresses. Mark it done when it is done.
3. One ledger and one plan index per repo. **Never create a second ledger, status board or plan
   index** — extend the one that exists.

---

## Standards you are expected to know

These live in the wiki and apply to every repository:

| Topic | Page |
|---|---|
| Naming — variables, functions, classes, files, tables, endpoints, branches | [naming-conventions.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/03-code-quality/naming-conventions.md) |
| Code shape — god files, god functions, nesting, dead code | [code-shape.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/03-code-quality/code-shape.md) |
| Error handling — what the caller sees, what the log sees | [error-handling.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/03-code-quality/error-handling.md) |
| Testing and the coverage ratchet | [testing.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/03-code-quality/testing.md) |
| Logging, metrics, tracing, audit | [06-observability/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/06-observability/README.md) |
| Databases, per engine, and migrations | [05-data/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/05-data/README.md) |
| APIs, gRPC, WebSockets/SignalR/SSE, messaging and events, versioning, error envelopes | [08-api-and-contracts/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/08-api-and-contracts/README.md) |
| Security, secrets, authn/authz, dependencies | [07-security/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/07-security/README.md) |
| Docker — mandatory — and infrastructure | [10-containers-and-infra/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/10-containers-and-infra/README.md) |
| Your language's specifics | [04-languages/](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/04-languages/README.md) |

---

## Working with an agent: the human's side

- **Review agent output like a stranger's PR, because that is what it is.** The gates catch shape;
  they do not catch "this is the wrong feature".
- **An agent that cannot run a gate must say so**, not approximate its result. "Docker is not
  available so the schema contract was not regenerated" is a useful sentence. A hand-built
  contract is a broken build two days later.
- **Ask for the failing output, not the summary.** A model can be confidently wrong about whether
  a suite passed.

---

## Lessons learned

The compressed reasoning behind these rules — every one a real incident — is in
[standards/12-ai-agents/lessons-learned.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/standards/12-ai-agents/lessons-learned.md).
**Read it before touching a database, a migration, a test fixture, a concurrency path or an app
bootstrap.**

When a new incident teaches something, add it there. Do not start a separate lessons file.
