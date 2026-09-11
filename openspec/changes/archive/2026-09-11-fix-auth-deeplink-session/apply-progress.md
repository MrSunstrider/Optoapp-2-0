# Apply Progress: fix-auth-deeplink-session

**Mode**: Strict TDD  
**Status**: done (12/12 tasks)  
**applyState**: all_done  
**Date**: 2026-09-11  
**Project**: Optoapp

## Completed Tasks

- [x] 1.1 RED — AuthDelegateDeepLinkTest null-data + no secret Uri Log.d
- [x] 1.2 GREEN — AuthDelegate null data → `"Enlace inválido"`; remove OAuth+recovery Uri `Log.d`
- [x] 1.3 Focused Delegate tests green
- [x] 2.1 RED — AuthViewModelTest null-data Error / no resolvePostLogin / await order
- [x] 2.2 GREEN — `awaitHandleAuthDeepLinkIntent` / `awaitHandleRecoveryDeepLink` + Job wrappers
- [x] 2.3 Focused VM tests green
- [x] 3.1 GREEN — MainActivity `lifecycleScope` await VIEW then `checkExistingSession`
- [x] 3.2 GREEN — `onNewIntent` ACTION_VIEW + non-null data gate
- [x] 3.3 Manifest untouched (no `singleTask`)
- [x] 4.1 Scope lock (no C6–C8 / Login reset reopen / GLOBAL / Manifest)
- [x] 4.2 Focused Unit 1–3 + Part1/2a recovery green
- [x] 4.3 Full `testDebugUnitTest` green

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1–1.3 | `AuthDelegateDeepLinkTest.kt` | Unit | ✅ AuthDelegateRecoveryTest + AuthDelegateTest | ✅ Written (4 fail) | ✅ Passed | ✅ null data + null intent + recovery log + source lock + logout token invariant | ✅ unused deepLink var → early null check |
| 2.1–2.3 | `AuthViewModelTest.kt` | Unit | ✅ existing AuthViewModelTest | ✅ Written (unresolved await*) | ✅ Passed | ✅ Error path + Success resolve + recovery await + Job wrapper + coVerifyOrder | ➖ None needed |
| 3.1–3.3 | `AuthDeepLinkBootstrapSourceLockTest.kt` | Unit (source lock) | N/A (new) | ✅ Written (2 fail) | ✅ Passed | ✅ onCreate serialize + onNewIntent gate + scope lock | ➖ None needed |
| 4.1–4.3 | focused + full suite | Unit | ✅ Part1/2a recovery + full suite | N/A (verify) | ✅ Full suite BUILD SUCCESSFUL | ➖ | ➖ |

## Work Unit Evidence

| Evidence | Value |
|----------|-------|
| Focused test command and result | `./gradlew :optoapp:testDebugUnitTest --tests …AuthDelegateDeepLinkTest --tests …AuthDelegateRecoveryTest --tests …AuthDelegateTest --tests …AuthViewModelTest --tests …AuthDeepLinkBootstrapSourceLockTest --tests …RecoveryScreenSourceLockTest --tests …RecoveryStateTest` → BUILD SUCCESSFUL |
| Runtime harness | N/A — unit-only Intent/auth; no emulator deep-link harness in CI (per tasks) |
| Rollback boundary | Revert `AuthDelegate.kt`, `AuthViewModel.kt`, `MainActivity.kt`, and Part 2b unit tests under `viewmodel/` / `viewmodel/auth/`; Manifest untouched |

### Test Summary

- **Total tests written**: 14 new (5 Delegate deep-link, 6 VM await, 3 bootstrap source locks)
- **Total tests passing**: focused suite + full `:optoapp:testDebugUnitTest` green
- **Layers used**: Unit (14), Integration (0), E2E (0)
- **Approval tests**: logout does not clear `pendingRecoveryToken` (invariant characterization)
- **Pure functions created**: 0

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `optoapp/.../auth/AuthDelegate.kt` | Modified | Null OAuth data → `"Enlace inválido"`; removed Uri `Log.d` |
| `optoapp/.../AuthViewModel.kt` | Modified | Suspend await APIs + Job wrappers |
| `optoapp/.../MainActivity.kt` | Modified | Serialize cold-start; gate `onNewIntent` |
| `.../auth/AuthDelegateDeepLinkTest.kt` | Created | Null-data / no-secret-log / logout token invariant |
| `.../AuthViewModelTest.kt` | Modified | Await + Error without resolvePostLogin |
| `.../AuthDeepLinkBootstrapSourceLockTest.kt` | Created | MainActivity serialize + warm gate + scope lock |
| `openspec/.../tasks.md` | Modified | All tasks `[x]` |
| `openspec/.../apply-progress.md` | Created | This artifact |

## Deviations from Design

None — implementation matches design.

## Issues Found

None.

## Remaining Tasks

None.

## Workload / PR Boundary

- Mode: single PR
- Current work unit: Units 1–4 (entire Part 2b)
- Boundary: AuthDelegate C5+C1 → VM await → MainActivity serialize → full suite
- Estimated review budget impact: Low (forecast 180–280 lines)

## Status

12/12 tasks complete. Ready for verify.
