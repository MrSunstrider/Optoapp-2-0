# Tasks: Fix Auth Recovery Regressions (Part 2a)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 120–220 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | `Error(isRetryable)` + VM mapping from `hasPendingRecoveryToken` | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --tests "com.example.optoapp.viewmodel.RecoveryStateTest" --stacktrace` | N/A — unit-only VM contract | Revert `AuthViewModel.kt`, `RecoveryState`/`Error` site, `AuthViewModelTest.kt`, `RecoveryStateTest.kt` |
| 2 | NewPassword retryable form vs terminal CTA; Login entry no reset | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --stacktrace` | Manual: cold-start recovery deep link → Login → NewPassword still usable (optional) | Revert `NewPasswordScreen.kt`, `LoginScreen.kt` (+ any screen helper tests) |

## Phase 1: AuthViewModel RED (C2 contract)

- [x] 1.1 In `optoapp/src/test/java/com/example/optoapp/viewmodel/AuthViewModelTest.kt`: stub `hasPendingRecoveryToken()`; RED — failed `updatePassword` with token present → `Error(isRetryable=true)` (spec: Retryable failure sets isRetryable true)
- [x] 1.2 Same file RED — failed `updatePassword` after token cleared / blank → `Error(isRetryable=false)` (spec: Terminal or blank-token failure)
- [x] 1.3 Same file RED — retryable path: second `updatePassword` without `resetRecoveryState` / clear (spec: User retries without resetting recovery)
- [x] 1.4 Confirm RED: `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --stacktrace`

## Phase 2: Error shape + RecoveryStateTest + AuthViewModel GREEN

- [x] 2.1 In `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt`: extend `RecoveryState.Error(message, isRetryable: Boolean = false)`
- [x] 2.2 In `optoapp/src/test/java/com/example/optoapp/viewmodel/RecoveryStateTest.kt`: equality/holds `isRetryable` (default false vs true differ)
- [x] 2.3 In `AuthViewModel.kt`: on `updatePassword` failure only, set `isRetryable = authDelegate.hasPendingRecoveryToken()`; other Error sites keep default `false`
- [x] 2.4 Do **not** edit `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt` (read-only)
- [x] 2.5 GREEN: same focused command as 1.4 + `--tests "com.example.optoapp.viewmodel.RecoveryStateTest"`

## Phase 3: NewPasswordScreen GREEN (C2 UI)

- [x] 3.1 In `optoapp/src/main/java/com/example/optoapp/ui/screens/NewPasswordScreen.kt`: `Error(isRetryable=true)` stays on form + banner; Guardar retries without `resetRecoveryState`
- [x] 3.2 Same file: terminal branch only for `Error(!isRetryable)` — CTA "Solicitar uno nuevo" → `resetRecoveryState` + navigate Recovery; no auto-nav on Error alone
- [x] 3.3 Optional lightweight helper test only if needed to lock form vs terminal CTA; no Robolectric

## Phase 4: LoginScreen GREEN (C3)

- [x] 4.1 In `optoapp/src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt`: remove entry `LaunchedEffect` `resetRecoveryState()`; keep remembered-email load
- [x] 4.2 Assert Login compose/entry does not call `resetRecoveryState` (characterization or source lock); Recovery/NewPassword explicit-exit resets unchanged

## Phase 5: Scope lock + verify (C2+C3 only)

- [x] 5.1 Confirm untouched: `AuthDelegate.kt`, `MainActivity.kt`, `RecoveryScreen.kt` (no C1/C4–C8)
- [x] 5.2 Full suite: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
