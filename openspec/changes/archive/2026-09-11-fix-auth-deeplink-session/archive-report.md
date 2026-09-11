# Archive Report — fix-auth-deeplink-session

**Archived**: 2026-09-11
**Verdict at close**: PASS WITH WARNINGS (`verify-report.md`, evidence_revision `sha256:4269e8c2608bf328004e1b85ca0cf19365e5ef36965c0497e9fe20bd7ff5e0cf`)
**Persistence**: hybrid (openspec filesystem + Engram)
**Archive path**: `openspec/changes/archive/2026-09-11-fix-auth-deeplink-session/`

## Summary

Part 2b auth deep-link/session (JD2-C1/C4/C5): cold-start awaits OAuth/recovery deep-link handlers before `checkExistingSession`; OAuth null `intent.data` returns `"Enlace inválido"` without `resolvePostLogin`/Room wipe; `onNewIntent` requires `ACTION_VIEW` + non-null data; secret-bearing deep-link Uri `Log.d` removed. `SignOutScope.GLOBAL` and Activity `launchMode` unchanged. Focused AuthDelegateDeepLinkTest / AuthViewModelTest / AuthDeepLinkBootstrapSourceLockTest + full suite 2350 green at verify; assembleDebug green. No CRITICAL findings; 0 blockers.

## Final-state authority notes

- **Tasks**: `tasks.md` on disk shows 12/12 `[x]` at archive time (Task Completion Gate passed). Engram observation `#2057` (`sdd/fix-auth-deeplink-session/tasks`) aligns with all-complete.
- **Verify**: `pass_with_warnings`, blockers 0, critical_findings 0 (observation `#2059`). Orchestrator launch confirms same verdict — no CRITICAL blockers.
- **Apply**: `apply-progress` observation `#2058` reports `applyState: all_done` (12/12).
- **SDD status**: `dependencies.archive: ready`, `taskProgress.allComplete: true`, `actionContext.mode: repo-local`.
- **Closed by this change**: JD2-C1 (no secret Uri logs), JD2-C4 (cold-start serialize), JD2-C5 (null-data fail-closed).

## Tasks

12/12 complete in archived `tasks.md` (Phases 1–4). No unchecked implementation tasks.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| android-auth | Updated | Native `gentle-ai sdd-archive-compose` merged delta into main. **ADDED** 4 requirements (8 scenarios). Prior requirements preserved byte-for-byte (Cold Start Restores Authenticated Route, Google Cancel Leaves Idle, Empty PIN Is Invalid, Create PIN Only When Required And Unset). |

Requirements after sync (main `openspec/specs/android-auth/spec.md`):
1. Cold Start Restores Authenticated Route (preserved)
2. Google Cancel Leaves Idle (preserved)
3. Empty PIN Is Invalid (preserved)
4. Create PIN Only When Required And Unset (preserved)
5. Cold Start Awaits Deep Link Before Session Check (ADDED)
6. OAuth Null Deep Link Data Is Fail-Closed (ADDED)
7. onNewIntent Ignores Non-VIEW Or Null-Data OAuth (ADDED)
8. Deep Link Handlers Do Not Log Secrets (ADDED)

Compose invocation:
```text
gentle-ai sdd-archive-compose \
  --canonical "openspec/specs/android-auth/spec.md" \
  --delta "openspec/changes/fix-auth-deeplink-session/specs/android-auth/spec.md" \
  --output "openspec/specs/android-auth/spec.md.compose-tmp"
```
Exit 0 → `Move-Item` compose-tmp → main spec.

## Mechanical archive move

- Source: `openspec/changes/fix-auth-deeplink-session` (fully untracked; `git mv` refused “source directory is empty”, status 128)
- Destination: `openspec/changes/archive/2026-09-11-fix-auth-deeplink-session/`
- Method: pre-move recursive snapshot → plain `Move-Item` fallback after unchanged-source `filecmp` check → recursive Python `filecmp.dircmp` readback snapshot vs destination
- Verbatim `diff -r` readback (Python dircmp): `(empty)`
- `DIFF_STATUS=0`
- Active change directory removed: confirmed absent

## Verification warnings at close (from verify-report; non-blocking)

1. Non-VIEW cold-start scenario remains PARTIAL — bootstrap source lock proves serialize order and that session always runs, but does not explicitly assert Non-VIEW skips OAuth/recovery await handlers (implementation gate exists in `MainActivity.onCreate`).
2. Suggestion (not blocking): add explicit source-lock assertion that `onCreate` contains `ACTION_VIEW` before await calls; optional manual device smoke for cold-start VIEW then warm `onNewIntent`.

## Remaining JD2 confirmed follow-ups (not blocking archive)

Documented for a new SDD change — do **not** reopen this archived change. JD2-C1, C4, and C5 are **closed** by this change.

| ID | Topic | Notes |
|----|-------|-------|
| **C6** | Rol admin | Membership / `rol` admin selection issues |
| **C7** | NetworkRetry anon | `NetworkRetryHelper` anon/session retry path |
| **C8** | PIN logout | PIN / CreatePin logout interaction |

### Additional suspects (out of scope for Part 2b; candidates for follow-up)

| Suspect | Notes |
|---------|-------|
| **CreatePin** | Await / CreatePin flow interaction with session bootstrap |
| **Membership Empty** | Empty membership handling after auth |
| **prepareOpticaSelection** | Optica selection preparation after login |
| **ON_RESUME** | Loading→Idle on resume lifecycle path |

Also deferred (explicit out of scope): AndroidManifest `singleTask` / `singleTop` launchMode; Part2a Login entry reset reopen.

## Engram observation IDs read (traceability)

| Artifact | Observation ID | Topic key |
|----------|----------------|-----------|
| explore | #2053 | `sdd/fix-auth-deeplink-session/explore` |
| proposal | #2054 | `sdd/fix-auth-deeplink-session/proposal` |
| spec | #2055 | `sdd/fix-auth-deeplink-session/spec` |
| design | #2056 | `sdd/fix-auth-deeplink-session/design` |
| tasks | #2057 | `sdd/fix-auth-deeplink-session/tasks` |
| apply-progress | #2058 | `sdd/fix-auth-deeplink-session/apply-progress` |
| verify-report | #2059 | `sdd/fix-auth-deeplink-session/verify-report` |

Filesystem artifacts also read from the change folder before move: `proposal.md`, `design.md`, `tasks.md`, `verify-report.md`, `specs/android-auth/spec.md`, `exploration.md`, `apply-progress.md`.

## Archive contents

- proposal.md ✅
- exploration.md ✅
- design.md ✅
- specs/android-auth/spec.md ✅
- tasks.md ✅ (12/12)
- apply-progress.md ✅
- verify-report.md ✅
- archive-report.md ✅ (additive; this file)

## Intentional archive notes

- Verdict `pass_with_warnings` with 0 CRITICAL / 0 blockers — archive allowed under strict policy.
- Non-VIEW PARTIAL scenario remains an accepted residual warning at close (implementation present; explicit assertion optional follow-up).
- `rules.archive` in `openspec/config.yaml` (“Warn before merging destructive deltas”): merge was **ADDED-only**; no REMOVED/MODIFIED destructive delta.

## SDD Cycle Complete

The change has been fully planned, implemented, verified, and archived.
Ready for the next change (JD2-C6/C7/C8 + listed suspects).
