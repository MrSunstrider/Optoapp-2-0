# Feature: gentle-ai-cursor-cycle

## Objective

Make the full Gentle AI cycle (ODD default flow, optional SDD, Strict TDD, RDD, GGA, Engram, Judgment Day) actually run inside Cursor for this repository, using only Cursor-native agents.

## Problem

An audit of the Cursor install (gentle-ai 3.7.0, engram 2.2.1, gga 2.10.1) found the cycle was only partially active:

1. `~/.cursor/rules/gentle-ai.mdc` (the orchestrator: ODD protocol, SDD preflight, delegation triggers, RDD assess) has no YAML frontmatter, so Cursor never applies it. The upstream Cursor adapter writes it with `StrategyFileReplace` and no `alwaysApply`, so every `gentle-ai sync` would strip a manual fix.
2. A byte-identical workspace copy of the orchestrator, agents, skills, and MCP servers lives in the gitignored `.cursor/`, duplicating every global asset (and would double-load the 85 KB orchestrator once it is applied).
3. Engram's MCP process runs with cwd `C:\Users\usuario`, so cwd detection resolves project `usuario`. The canonical project `optoapp` (holds `sdd-init/optoapp`, Strict TDD) rejected every write because session `manual-save-optoapp` was owned by `usuario`.
4. `gentle-ai review assess` reports `unassessable` while local build/verification artifacts sit untracked in the repo root.
5. Cursor tooling injects `Co-authored-by: Cursor <cursoragent@cursor.com>` into agent commits, violating the no-AI-attribution rule; rules cannot stop it.

## Scope

- In: Cursor rules, user-level Cursor hook, Engram session ownership, `.gitignore` hygiene, a `commit-msg` hook that strips Cursor attribution, Cursor CLI attribution config, skill registry refresh.
- Out: Upstream gentle-ai fix (Cursor adapter frontmatter), Engram cloud sync targets, the GGA infra-failure tolerance follow-up (RDD advisory R4).

## Constraints

- Cursor-only runtime; never delegate to OpenCode/Claude/Codex.
- Conventional commits, no AI attribution; GGA + full tests before push; push/PR remain the user's decision.
- TDD resolved from project config: Strict TDD on (`sdd-init/optoapp`); hook runner `bash .githooks/test_pre_commit.sh`.

## Delivery

- Forecast: about 150 authored changed lines in tracked files. Strategy: `ask-on-risk` (under budget, single slice).
- Branch: `chore/gga-cursor-precommit`.

## Tasks

- [x] T1 Engram: fix `manual-save-optoapp` ownership (`project=optoapp`, `shared`) after a `VACUUM INTO` backup; verify `mem_save(project: optoapp)` succeeds. Route: inline (bounded state fix). Evidence: save returned `project_source: explicit_override`, id 2199.
- [x] T2 Orchestrator loads: add `alwaysApply` frontmatter to `~/.cursor/rules/gentle-ai.mdc`; remove workspace duplicates (rules/gentle-ai.mdc, agents, skills, mcp.json); keep a project-specific repo rule. Route: inline (mechanical local config). Evidence: duplicates backed up to `~/.gentle-ai/backups/optoapp-workspace-dup-20260930-212311`; repo rule holds Optoapp-only bindings.
- [x] T3 Durable frontmatter: user hook (`sessionStart` + `afterShellExecution` on `gentle-ai sync|upgrade|install`) that re-applies the frontmatter idempotently. Route: inline. Evidence: `~/.cursor/hooks/gentle-ai-rule-frontmatter.mjs` patched then reported unchanged; it also resets the global GGA provider to `cursor`, which `gentle-ai sync` rewrites to `claude`.
- [x] T4 `.gitignore` local build/verification artifacts so `review assess` is assessable. Route: inline. Commit 117760c5.
- [x] T5 `commit-msg` hook strips Cursor attribution trailers, with tests first (RED → GREEN). Route: inline (one hook + its test). Commit 383938f5; `test_commit_msg.sh` 8/8; the commit itself landed without the trailer.
- [x] T6 Disable Cursor CLI commit/PR attribution; refresh skill registry. Route: inline. Evidence: `~/.cursor/cli-config.json` attribution false; `.cursor/skills` junction to `~/.cursor/skills` so the registry resolves Cursor paths (13 skills); Engram `skill-registry` mirror refreshed.
- [x] T7 Verify end to end: hook tests, `review assess`, GGA, RDD on the new commits. Evidence: `gentle-ai doctor` healthy (7/7); `test_pre_commit.sh` 15/15 and `test_commit_msg.sh` 8/8 under Git Bash; GGA ran with provider `cursor` on ce241961, which landed without a trailer; RDD `review-4df030caf66aa05e` (bf43cd2d..ce241961, high risk) approved and acknowledged after human consent.
- [x] T8 Close Claude leakage and RDD advisories: point the Optoapp section of `~/CLAUDE.md` (always loaded by Cursor, including `cursor-agent`) at `AGENTS.md` instead of stale `:app`/Retrofit facts; make `commit-msg` keep non-UTF-8 bodies (`LC_ALL=C`, `grep -a`) with byte-exact tests and the attribution-only case. Route: inline, TDD (scenario 5 RED showed the whole message replaced by "Binary file ... matches"). Commit 4b6a3b6d; RDD `review-85aa51e697a0b7f5` approved and acknowledged.

## Acceptance criteria

- `gentle-ai.mdc` starts with `alwaysApply: true` frontmatter and survives `gentle-ai sync`.
- Exactly one copy of each Gentle AI asset is loaded (global).
- `mem_save(project: "optoapp")` succeeds.
- `gentle-ai review assess --base-ref main --committed-only` returns a real tier, not `unassessable` for untracked files.
- A commit message containing a Cursor attribution trailer is committed without it; hook tests pass.

## Progress

- T1-T8 done. RDD advisories from the first review (locale-dependent grep, unasserted trailing blank lines, attribution-only message, readability nits) are fixed in 4b6a3b6d.
- Open, optional: scenario 5 of `test_commit_msg.sh` only discriminates on hosts that honor `C.UTF-8` (true on this machine: it failed before the fix).

## Next step

User: turn off Cursor Settings > Agents > Third-Party Imports ("Include Third-Party Plugins, Skills, and Other Configs") so the IDE stops loading the Claude Code engram/vercel plugins and their hooks.
