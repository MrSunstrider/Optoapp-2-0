# Archive Report — fix-auth-pin-logout

**Archived**: 2026-09-11
**Verdict at close**: PASS WITH WARNINGS (`verify-report.md`, evidence_revision `sha256:a6c9dbdba36b7c439e1edb3bfd0f5465f989ed671116abfbd6cdc9921714de18`)
**Persistence**: hybrid (openspec filesystem + Engram)
**Archive path**: `openspec/changes/archive/2026-09-11-fix-auth-pin-logout/`

## Summary

JD2-C8 PIN logout wipe + CreatePin await gate: `ISecurityManager.clearStoredPin` empties `user_pin` / flag / `_pinFlow`; `SessionManager.clearSession` removes `PIN_HAS_BEEN_SET`; `AuthDelegate.logout` always wipes after Room wipe despite non-cancellation remote signOut failures; `PinDelegate.createPin` → `Boolean` with `AuthViewModel.createPinAwaitingSuccess`; `CreatePinScreen` navigates Main only on success. Optional PIN product default unchanged. `SignOutScope.GLOBAL` and Activity `launchMode` unchanged. Focused PIN suite 113/0; assembleDebug exit 0. No CRITICAL findings; 0 blockers.

## Final-state authority notes

- **Tasks**: archived `tasks.md` shows 18/18 `[x]` at archive time (Task Completion Gate passed; 0 unchecked `- [ ]`). Engram observation `#2069` (`sdd/fix-auth-pin-logout/tasks`) aligns with all-complete. Native status `taskProgress.allComplete: true` (19/19 counting including non-checkbox rows).
- **Verify**: `pass_with_warnings`, blockers 0, critical_findings 0 (observation `#2073`). Orchestrator launch confirms same verdict — no CRITICAL blockers.
- **Apply**: `apply-progress` observation `#2071` reports `applyState: all_done` (18/18).
- **SDD status**: `dependencies.archive: ready`, `actionContext.mode: repo-local`, `allowedEditRoots` includes Optoapp repo.
- **Closed by this change**: JD2-C8 (logout must wipe device PIN secret + has-been-set; CreatePin must await successful persist before Main).

## Observation IDs (traceability)

| Artifact | Engram ID | topic_key |
|----------|-----------|-----------|
| explore | #2062 | `sdd/fix-auth-pin-logout/explore` |
| proposal | #2063 | `sdd/fix-auth-pin-logout/proposal` |
| spec | #2065 | `sdd/fix-auth-pin-logout/spec` |
| design | #2067 | `sdd/fix-auth-pin-logout/design` |
| tasks | #2069 | `sdd/fix-auth-pin-logout/tasks` |
| apply-progress | #2071 | `sdd/fix-auth-pin-logout/apply-progress` |
| verify-report | #2073 | `sdd/fix-auth-pin-logout/verify-report` |
| archive-report | (this save) | `sdd/fix-auth-pin-logout/archive-report` |

Filesystem artifacts also read from `openspec/changes/fix-auth-pin-logout/` before the mechanical move (proposal, design, tasks, verify-report, specs/android-auth/spec.md, exploration, apply-progress).

## Tasks

18/18 complete in archived `tasks.md` (Phases 1–6). No unchecked implementation tasks. No archive-time checkbox reconciliation required.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| android-auth | Updated | Native `gentle-ai sdd-archive-compose` merged delta into main. **ADDED** 2 requirements (5 scenarios). **MODIFIED** 1 requirement (Create PIN Only When Required And Unset — 3 scenarios retained; wording now forbids mandatory-PIN-for-all). No REMOVED/RENAMED. Prior unrelated requirements preserved. |

Requirements after sync (main `openspec/specs/android-auth/spec.md`):
1. Cold Start Restores Authenticated Route (preserved)
2. Google Cancel Leaves Idle (preserved)
3. Empty PIN Is Invalid (preserved)
4. Create PIN Only When Required And Unset (MODIFIED)
5. Cold Start Awaits Deep Link Before Session Check (preserved)
6. OAuth Null Deep Link Data Is Fail-Closed (preserved)
7. onNewIntent Ignores Non-VIEW Or Null-Data OAuth (preserved)
8. Deep Link Handlers Do Not Log Secrets (preserved)
9. Logout Clears Device PIN State (ADDED)
10. Create PIN Persists Before Navigate (ADDED)

Compose invocation:
```text
gentle-ai sdd-archive-compose \
  --canonical "openspec/specs/android-auth/spec.md" \
  --delta "openspec/changes/fix-auth-pin-logout/specs/android-auth/spec.md" \
  --output "openspec/specs/android-auth/spec.md.compose-tmp"
```
Exit 0 → `Move-Item` compose-tmp → main spec.

Destructive-delta check (`rules.archive`): no REMOVED sections; merge is additive + one MODIFIED requirement — no destructive warning needed.

## Mechanical archive move

- Source: `openspec/changes/fix-auth-pin-logout` (fully untracked; `git mv` refused “source directory is empty”, status 128)
- Destination: `openspec/changes/archive/2026-09-11-fix-auth-pin-logout/`
- Method: pre-move recursive snapshot → unchanged-source `diff -r` vs snapshot → plain `Move-Item` fallback → `diff -r` snapshot vs destination
- Verbatim `diff -r` readback:

```text
=== DIFF -R START ===
=== DIFF -R END status=0 ===
```

- `DIFF_STATUS=0` (empty output — byte-identical)
- Active change directory removed: confirmed absent (`openspec/changes/fix-auth-pin-logout` → False)

## Archive contents

- proposal.md ✅
- exploration.md ✅
- design.md ✅
- specs/android-auth/spec.md ✅
- tasks.md ✅ (18/18 complete)
- apply-progress.md ✅
- verify-report.md ✅
- archive-report.md ✅ (additive; excluded from move diff)

## Verification warnings at close (from verify-report; non-blocking)

Attributed to verify-report observation `#2073` at verification time — not current blockers:

1. Full `testDebugUnitTest` suite exit 1 under parallel `fix-auth-membership-jwt` Gradle contention (`NoSuchFileException` on test-results binary). Not a PIN assertion failure; focused PIN suite 113/0 green. Shared-workspace WARNING.
2. Optional instrumented `SecurityManagerInstrumentedTest` ESP wipe not run (tasks 6.3 optional).

Suggestions (non-blocking, out of cycle):
1. Serialize Gradle across concurrent SDD applies in the same workspace.
2. Design open items remain: PinDelegate cooldown reset on logout; SettingsViewModel `isPinRequired` stateIn default.

## Source of Truth Updated

- `openspec/specs/android-auth/spec.md` now includes Logout Clears Device PIN State, Create PIN Persists Before Navigate, and the MODIFIED optional-PIN wording.

## SDD Cycle Complete

The change has been fully planned, implemented, verified (`pass_with_warnings`), and archived.
Ready for the next change. `next_recommended`: none (this change is complete). Follow-ups such as JD2-C6/C7 membership/JWT belong to separate changes (e.g. `fix-auth-membership-jwt`), not a reopen of this archive.
