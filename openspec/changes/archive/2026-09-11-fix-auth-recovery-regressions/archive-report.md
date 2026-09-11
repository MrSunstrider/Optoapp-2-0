# Archive Report — fix-auth-recovery-regressions

**Archived**: 2026-09-11
**Verdict at close**: PASS WITH WARNINGS (`verify-report.md`, evidence_revision `sha256:46567d37dcab096dea88fc45ec2e30fd032be43a09f0f57974c8f19fbea3edf8`)
**Persistence**: hybrid (openspec filesystem + Engram)
**Archive path**: `openspec/changes/archive/2026-09-11-fix-auth-recovery-regressions/`

## Summary

Part 2a auth recovery regressions (JD2-C2/C3): `RecoveryState.Error(isRetryable)` from `hasPendingRecoveryToken()` after failed `updatePassword`; NewPassword keeps form on retryable Error and terminal CTA resets + navigates Recovery; Login entry no longer calls `resetRecoveryState()`. Focused AuthViewModel/RecoveryState/source-lock tests + full suite 2336 green at verify; assembleDebug green. No CRITICAL findings; 0 blockers.

## Final-state authority notes

- **Tasks**: `tasks.md` on disk shows 16/16 `[x]` at archive time (Task Completion Gate passed). Engram observation `#2049` (`sdd/fix-auth-recovery-regressions/tasks`) aligns with all-complete.
- **Verify**: `pass_with_warnings`, blockers 0, critical_findings 0 (observation `#2051`). Orchestrator launch confirms same verdict — no CRITICAL blockers.
- **Apply**: `apply-progress` observation `#2050` reports `applyState: all_done` (16/16).
- **Out of scope at close (unchanged)**: AuthDelegate taxonomy rewrite; MainActivity C4/C5; remaining JD2 items listed below.

## Tasks

16/16 complete in archived `tasks.md` (Phases 1–5). No unchecked implementation tasks.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| android-password-recovery | Updated | Native `gentle-ai sdd-archive-compose` merged delta into main. **ADDED** 2 requirements (4 scenarios); **MODIFIED** 2 requirements (Explicit-Exit + Terminal Error CTA; +1 scenario Login entry; Terminal CTA now bound to `isRetryable=false` + reset). Unrelated Part 1 requirements preserved. |

Requirements after sync (main `openspec/specs/android-password-recovery/spec.md`):
1. Explicit-Exit Recovery State Reset (MODIFIED — Login entry must not reset; + Login entry scenario)
2. NewPassword Navigation Stack Hygiene (preserved)
3. Pending Recovery Token Lifetime (preserved)
4. Terminal Error CTA Navigates to Recovery (MODIFIED — `isRetryable=false` + reset on CTA)
5. Back from NewPassword Clears and Returns to Login (preserved)
6. Recovery Password Update Timeouts (preserved)
7. Recovery Error Retryability Flag (ADDED)
8. Retryable NewPassword Error Keeps Form (ADDED)

Compose invocation:
```text
gentle-ai sdd-archive-compose \
  --canonical "openspec/specs/android-password-recovery/spec.md" \
  --delta "openspec/changes/fix-auth-recovery-regressions/specs/android-password-recovery/spec.md" \
  --output "openspec/specs/android-password-recovery/spec.md.compose-tmp"
```
Exit 0 → `Move-Item` compose-tmp → main spec.

## Mechanical archive move

- Source: `openspec/changes/fix-auth-recovery-regressions` (fully untracked; `git mv` refused “source directory is empty”, status 128)
- Destination: `openspec/changes/archive/2026-09-11-fix-auth-recovery-regressions/`
- Method: pre-move recursive snapshot → plain `Move-Item` fallback after unchanged-source hash check → recursive hash + Python `filecmp` readback snapshot vs destination
- Verbatim `diff -r` readback (Python dircmp): `(empty)`
- `DIFF_STATUS=0`
- Active change directory removed: confirmed absent

## Verification warnings at close (from verify-report; non-blocking)

1. Dispose-no-reset scenario remains PARTIAL — no Compose/NavHost instrumented test (accepted residual from Part 1; source inspection confirms no `DisposableEffect` reset).
2. Working tree still contains Part 1 diffs on `AuthDelegate.kt`, `MainActivity.kt`, and `RecoveryScreen.kt` vs HEAD; Part 2a correctly avoided further edits to those for C4–C8.

## Remaining JD2 confirmed follow-ups (not blocking archive)

Documented for a new SDD change — do **not** reopen this archived change:

| ID | Topic | Notes |
|----|-------|-------|
| **C1** | Log.d tokens | Deep-link URI / token logging via `Log.d` |
| **C4** | Cold-start race | `checkExistingSession` / cold-start race with recovery deep link |
| **C5** | Null intent wipe | Null `intent.data` wiping recovery context |
| **C6** | Rol admin | Membership / `rol` admin selection issues |
| **C7** | NetworkRetry anon | `NetworkRetryHelper` anon/session retry path |
| **C8** | PIN logout | PIN / CreatePin logout interaction |

JD2-C2 and JD2-C3 are **closed** by this change.

## Engram observation IDs read (traceability)

| Artifact | Observation ID | Topic key |
|----------|----------------|-----------|
| explore | #2043 | `sdd/fix-auth-recovery-regressions/explore` |
| proposal | #2045 | `sdd/fix-auth-recovery-regressions/proposal` |
| spec | #2047 | `sdd/fix-auth-recovery-regressions/spec` |
| design | #2048 | `sdd/fix-auth-recovery-regressions/design` |
| tasks | #2049 | `sdd/fix-auth-recovery-regressions/tasks` |
| apply-progress | #2050 | `sdd/fix-auth-recovery-regressions/apply-progress` |
| verify-report | #2051 | `sdd/fix-auth-recovery-regressions/verify-report` |

Filesystem artifacts also read from the change folder before move: `proposal.md`, `design.md`, `tasks.md`, `verify-report.md`, `specs/android-password-recovery/spec.md`, `exploration.md`, `apply-progress.md`.

## Archive contents

- proposal.md ✅
- exploration.md ✅
- design.md ✅
- specs/android-password-recovery/spec.md ✅
- tasks.md ✅ (16/16)
- apply-progress.md ✅
- verify-report.md ✅
- archive-report.md ✅ (additive; this file)

## Intentional archive status

`intentional-with-warnings`: verify `pass_with_warnings` (dispose PARTIAL residual; Part 1 unclean tree files vs HEAD). No CRITICAL blockers. Remaining JD2 C1/C4–C8 tracked as follow-up only.

## Next recommended

New `/sdd-new` (or equivalent) for remaining JD2 confirmed follow-ups: C1 Log.d tokens, C4 cold-start race, C5 null intent wipe, C6 rol admin, C7 NetworkRetry anon, C8 PIN logout. Prefer scoped slices rather than one mega-change.
