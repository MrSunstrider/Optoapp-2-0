# Apply Progress: fix-auth-recovery-regressions

**Mode**: Strict TDD
**Status**: done (16/16 tasks)
**applyState**: all_done
**Date**: 2026-09-11

## Completed Tasks

- [x] 1.1–1.4 AuthViewModel RED (`isRetryable` true/false + retry without reset); RED confirmed via compile failure on missing `isRetryable`
- [x] 2.1–2.5 `RecoveryState.Error(message, isRetryable)`; VM maps from `hasPendingRecoveryToken()` on `updatePassword` failure only; AuthDelegate untouched
- [x] 3.1–3.3 NewPassword: retryable keeps form+banner; terminal `!isRetryable` CTA reset+Recovery; source locks in `RecoveryScreenSourceLockTest`
- [x] 4.1–4.2 Login entry `LaunchedEffect` no longer calls `resetRecoveryState`; source lock asserts entry block
- [x] 5.1–5.2 Scope lock (AuthDelegate/MainActivity/RecoveryScreen not edited this apply); full suite green

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `AuthViewModelTest.kt` | Unit | ✅ Auth+Recovery baseline pass | ✅ Written (`isRetryable` missing → compile fail) | ✅ Passed after 2.1/2.3 | ✅ true vs false stubs | ➖ None needed |
| 1.2 | `AuthViewModelTest.kt` | Unit | ✅ same | ✅ Written | ✅ Passed | ✅ 401 + 403 blank-token | ➖ None needed |
| 1.3 | `AuthViewModelTest.kt` | Unit | ✅ same | ✅ Written | ✅ Passed | ✅ two `updatePassword` calls, zero clear | ➖ None needed |
| 1.4 | focused gradle | Unit | N/A | ✅ Compile RED | — | — | — |
| 2.1–2.3 | `RecoveryStateTest` + VM | Unit | ✅ | ✅ `isRetryable` equality tests | ✅ Passed | ✅ default false vs true differ | ➖ Clean |
| 2.4 | AuthDelegate read-only | — | N/A | ➖ | ✅ No edits | ➖ | ➖ |
| 2.5 | focused gradle | Unit | N/A | — | ✅ BUILD SUCCESSFUL | — | — |
| 3.1–3.2 | NewPasswordScreen | Screen wiring | N/A (GREEN wiring) | ➖ VM contracts first | ✅ Compile + source locks | ✅ retryable vs terminal branches | ➖ |
| 3.3 | `RecoveryScreenSourceLockTest` | Unit source lock | N/A | ✅ Written | ✅ Passed | ✅ form + CTA locks | ➖ |
| 4.1–4.2 | LoginScreen + source lock | Screen + Unit | N/A | ✅ Source lock | ✅ Passed | ➖ Single | ➖ |
| 5.1–5.2 | full suite | Unit | N/A | — | ✅ BUILD SUCCESSFUL | — | — |

## Work Unit Evidence

| Evidence | Result |
|----------|--------|
| Focused test command | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --tests "com.example.optoapp.viewmodel.RecoveryStateTest" --tests "com.example.optoapp.viewmodel.RecoveryScreenSourceLockTest" --stacktrace` → **BUILD SUCCESSFUL** |
| Orchestrator focused command | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --tests "com.example.optoapp.viewmodel.RecoveryStateTest" --stacktrace` → **BUILD SUCCESSFUL** |
| Runtime harness | N/A — unit-only VM/UI-source contract (manual cold-start optional per tasks) |
| Full suite (5.2) | `./gradlew :optoapp:testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL** (~2m) |
| Rollback boundary | Revert `AuthViewModel.kt`, `NewPasswordScreen.kt`, `LoginScreen.kt` (if dirty), `AuthViewModelTest.kt`, `RecoveryStateTest.kt`, `RecoveryScreenSourceLockTest.kt` |

## Files Changed (this apply)

| File | Action |
|------|--------|
| `optoapp/.../viewmodel/AuthViewModel.kt` | Modified — `Error(isRetryable)`; updatePassword failure maps token |
| `optoapp/.../ui/screens/NewPasswordScreen.kt` | Modified — retryable form vs terminal CTA |
| `optoapp/.../ui/screens/LoginScreen.kt` | Confirmed no entry `resetRecoveryState` (aligned with HEAD / C3) |
| `optoapp/.../viewmodel/AuthViewModelTest.kt` | Modified — stub `hasPendingRecoveryToken`; isRetryable assertions |
| `optoapp/.../viewmodel/RecoveryStateTest.kt` | Modified — isRetryable equality |
| `optoapp/.../viewmodel/RecoveryScreenSourceLockTest.kt` | Created — Login + NewPassword source locks |
| `openspec/.../tasks.md` | All tasks `[x]` |

## Untouched (scope lock)

- `AuthDelegate.kt` — not edited this apply
- `MainActivity.kt` — not edited this apply
- `RecoveryScreen.kt` — not edited this apply

## Deviations from Design

None — implementation matches design.

## Issues Found

None. Note: Login entry reset was already absent in HEAD; apply ensured working tree matches C3 (no entry reset) and locked via source test.

## Workload / PR Boundary

- Mode: single PR
- Current work unit: Part 2a C2+C3 complete
- Estimated review budget impact: Low (~120–220 authored lines)
- Delivery strategy: ask-on-risk (no chain)

## Next

`sdd-verify`
