# Claude instructions

All AI agent instructions for this repository are in **[AIAGENTS.md](AIAGENTS.md)**. Read it at
the start of every session and follow its planning and verification requirements.

Claude-specific notes:

- Use `TodoWrite` for any task with more than three steps, and keep it current — a stale plan is
  worse than none.
- Prefer the file tools over shell equivalents where both would work.
- Run `./scripts/check-before-push.sh` yourself before saying a change is ready. Do not ask the
  user to check what you can check.
- **Never add `Co-Authored-By` trailers, "Generated with" footers or any AI product name** to
  code, comments, commit messages, branch names, PR titles or PR bodies. The `commit-msg` hook
  rejects them. This filename is the one permitted occurrence.
