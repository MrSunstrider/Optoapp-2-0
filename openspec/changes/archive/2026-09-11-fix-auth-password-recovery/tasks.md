# Tasks: Fix Auth Password Recovery (Part 1)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 280–380 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR; 3 work-unit commits |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Delegate token taxonomy + PUT timeouts | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.auth.AuthDelegateRecoveryTest" --stacktrace` | N/A — unit HTTP seam; no device for Part 1 | Revert `AuthDelegate.kt` + `AuthDelegateRecoveryTest.kt` |
| 2 | ViewModel reset clears token; Error/retry | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --stacktrace` | N/A — MockK ViewModel contracts | Revert `AuthViewModel.kt` + ViewModel recovery test deltas |
| 3 | Explicit-exit screens + popUpTo + Login guard | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.*" --stacktrace` | Manual: warm `optoapp://auth` recovery → NewPassword → set password | Revert screen/nav files only; leave Delegate/VM if green |

**Scope lock:** Part 1 only (JD-C1, S4, timeouts, Error UX). No Parts 2–4.

## Phase 1: AuthDelegate — timeouts + token taxonomy (JD-S4)

- [x] 1.1 RED: Create `optoapp/src/test/java/com/example/optoapp/viewmodel/auth/AuthDelegateRecoveryTest.kt` — assert `openRecoveryPasswordConnection` sets connect=10000 / read=15000 (*Timeouts applied on recovery PUT*)
- [x] 1.2 RED: Same test — keep token on `IOException`/timeout and HTTP 5xx/non-auth 4xx; retry still has token (*Retryable failure retains token*, *Timeout is retryable*)
- [x] 1.3 RED: Same test — clear token on HTTP 2xx and on 401/403 (*HTTP success clears token*, *Terminal auth failure clears token*)
- [x] 1.4 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt` — add `clearPendingRecoveryToken()`, `hasPendingRecoveryToken()`, `openRecoveryPasswordConnection`; taxonomy clear-after-status; 10s/15s timeouts
- [x] 1.5 REFACTOR: Tighten Delegate seam only; no Ktor rewrite; never log Bearer/token

## Phase 2: AuthViewModel — reset + Error/retry contracts

- [x] 2.1 RED: Extend `optoapp/src/test/java/com/example/optoapp/viewmodel/AuthViewModelTest.kt` — `resetRecoveryState()` → Idle + delegate token cleared (*resetRecoveryState clears token*, *Explicit exit clears recovery state*)
- [x] 2.2 RED: Same — retryable `updatePassword` failure → Error with token kept; second call still uses token (*Retryable failure retains token*)
- [x] 2.3 RED: Same — terminal 401/403 path → Error suitable for CTA re-request; token cleared (*Terminal auth failure clears token*)
- [x] 2.4 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt` — `resetRecoveryState()` calls `authDelegate.clearPendingRecoveryToken()` after Idle
- [x] 2.5 REFACTOR: Keep ViewModel thin; no screen/nav changes in this phase

## Phase 3: Screens + navigation (JD-C1 + UX)

- [x] 3.1 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/ui/screens/RecoveryScreen.kt` — remove `DisposableEffect` dispose→`resetRecoveryState` (*Dispose during LinkReceived navigation does not reset*)
- [x] 3.2 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/ui/screens/NewPasswordScreen.kt` — remove dispose reset; Error CTA → `resetRecoveryState` + navigate Recovery; Back → Login + reset (*User taps Error CTA*, *Error does not auto-navigate*, *Back clears recovery and opens Login*)
- [x] 3.3 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/MainActivity.kt` — LinkReceived nav `popUpTo(Recovery) { inclusive = true }` (*LinkReceived navigates with popUpTo Recovery*)
- [x] 3.4 GREEN: Modify `optoapp/src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt` — entry `resetRecoveryState()` stale-state guard (*Explicit exit clears recovery state*)
- [x] 3.5 Verify: `./gradlew :optoapp:testDebugUnitTest --stacktrace` green; no Parts 2–4 edits

## Phase 4: Cleanup

- [x] 4.1 Optional only: touch `optoapp/src/test/java/com/example/optoapp/viewmodel/RecoveryStateTest.kt` if equality helpers need sync — not primary coverage
- [x] 4.2 Confirm no dispose-reset left on Recovery/NewPassword; scope lock holds
