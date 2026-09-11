# Apply Progress: fix-auth-pin-logout

**Mode**: Strict TDD  
**Status**: done (18/18 tasks)  
**applyState**: all_done  
**Date**: 2026-09-11  
**Delivery**: single PR (workload Low; Decision needed before apply: No)

## Completed Tasks

- [x] 1.1–1.4 `ISecurityManager.clearStoredPin` + FakeSecurityManager + SecurityManagerTest
- [x] 2.1–2.3 `SessionManager.clearSession` removes `PIN_HAS_BEEN_SET`
- [x] 3.1–3.3 `AuthDelegate.logout` always calls `clearStoredPin` (even if signOut throws IOException)
- [x] 4.1–4.4 `PinDelegate.createPin(): Boolean`, `AuthViewModel.createPinAwaitingSuccess`, CreatePinScreen gate
- [x] 5.1–5.2 Scope lock (no C6/C7 / SignOutScope / launchMode / mandatory PIN); PostLoginNavigation green
- [x] 6.1–6.3 Focused Units 1–3 green; full suite evidence below; optional instrumented skipped

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1–1.4 | `SecurityManagerTest.kt`, `PinDelegateTest.kt` | Unit | ✅ Unit1 pre-green | ✅ Written | ✅ Passed | ✅ empty + after-save | ➖ None needed |
| 2.1–2.3 | `SessionManagerTest.kt` | Unit | ✅ | ✅ Written | ✅ Passed | ✅ already-unset + isPinRequired | ➖ None needed |
| 3.1–3.3 | `AuthDelegateTest.kt` | Unit | ✅ | ✅ Written | ✅ Passed | ✅ IOException + success paths | ➖ None needed |
| 4.1–4.4 | `PinDelegateTest.kt`, `AuthViewModelTest.kt` | Unit | ✅ | ✅ Written | ✅ Passed | ✅ invalid/weak/valid + screen source gate | ➖ None needed |
| 5.1–5.2 | `PostLoginNavigationTest` | Unit | N/A | ✅ Existing | ✅ Passed | ✅ optional PIN skips create | ➖ None needed |

## Work Unit Evidence

| Unit | Focused command | Result | Runtime harness | Rollback boundary |
|------|-----------------|--------|-----------------|-------------------|
| 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.SecurityManagerTest --tests com.example.optoapp.viewmodel.PinDelegateTest --stacktrace` | BUILD SUCCESSFUL | N/A — unit/ESP prefs | Revert SecurityManager + fakes/tests |
| 2 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.SessionManagerTest --tests com.example.optoapp.viewmodel.AuthDelegateTest --stacktrace` | BUILD SUCCESSFUL | N/A — MockK | Revert SessionManager + AuthDelegate + tests |
| 3 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.PinDelegateTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --stacktrace` | BUILD SUCCESSFUL | N/A — optional CreatePin smoke | Revert PinDelegate + AuthViewModel + CreatePinScreen + tests |
| 4 | Units 1–3 + PostLoginNavigation; full `testDebugUnitTest` | Focused green; full suite: only `MembershipRepositoryDtosTest` (3 failures) from parallel `fix-auth-membership-jwt` — **not touched by this change**. Later full re-runs hit shared-build contention (multiple Kotlin daemons / ASM delete races). | N/A | Revert JD2-C8 Android/test commits as a set |

## Files Changed

| File | Action |
|------|--------|
| `optoapp/.../data/SecurityManager.kt` | Modified — `clearStoredPin` |
| `optoapp/.../data/SessionManager.kt` | Modified — remove `PIN_HAS_BEEN_SET` on clear |
| `optoapp/.../viewmodel/auth/AuthDelegate.kt` | Modified — logout wipe |
| `optoapp/.../viewmodel/auth/PinDelegate.kt` | Modified — `createPin(): Boolean` |
| `optoapp/.../viewmodel/AuthViewModel.kt` | Modified — `createPinAwaitingSuccess` |
| `optoapp/.../ui/screens/CreatePinScreen.kt` | Modified — navigate only on true |
| `optoapp/.../data/SecurityManagerTest.kt` | Modified — clearStoredPin contract |
| `optoapp/.../data/SessionManagerTest.kt` | Modified — clearSession PIN flag |
| `optoapp/.../viewmodel/PinDelegateTest.kt` | Modified — Fake + Boolean createPin |
| `optoapp/.../viewmodel/AuthDelegateTest.kt` | Modified — logout wipe verify |
| `optoapp/.../viewmodel/AuthViewModelTest.kt` | Modified — awaiting success + screen gate |
| `openspec/changes/fix-auth-pin-logout/tasks.md` | Modified — all [x] |
| `openspec/changes/fix-auth-pin-logout/apply-progress.md` | Created |

## Deviations from Design

None — dual wipe (`clearStoredPin` + `clearSession` remove), await Boolean, optional PIN unchanged, `SignOutScope.GLOBAL` retained.

## Issues Found

1. Full suite shows 3 failures in `MembershipRepositoryDtosTest` owned by parallel `fix-auth-membership-jwt` (explicitly out of allowedEditRoots).
2. Concurrent Gradle/Kotlin daemons in the shared workspace caused intermittent ASM/KSP compile failures on later full-suite retries.

## Next

`sdd-verify`
