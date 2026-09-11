```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:46567d37dcab096dea88fc45ec2e30fd032be43a09f0f57974c8f19fbea3edf8
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 4/4
scenarios: 9/9
test_command: ./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon --rerun-tasks
test_exit_code: 0
test_output_hash: sha256:ff42a048ca27af96be4d403b3e72f3b1f4c9de1c045a7de8b419473d3c9980da
build_command: ./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
build_exit_code: 0
build_output_hash: sha256:f6ebf8e3620f92d2b66150712a85cdb67b6aba326e5d0ffcab98dde41561104d
```

## Verification Report

**Change**: fix-auth-recovery-regressions
**Version**: N/A (delta capability `android-password-recovery`, Part 2a JD2-C2/C3)
**Mode**: Strict TDD
**Persistence**: hybrid

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 16 |
| Tasks complete | 16 |
| Tasks incomplete | 0 |

All Phase 1–5 checkboxes in `openspec/changes/fix-auth-recovery-regressions/tasks.md` are `[x]`. Apply-progress reports `applyState: all_done`.

### Build & Tests Execution
**Build**: Passed

```text
./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
exit 0
BUILD SUCCESSFUL in 41s
build_output_hash: sha256:f6ebf8e3620f92d2b66150712a85cdb67b6aba326e5d0ffcab98dde41561104d
```

**Tests**: 2336 passed suites aggregate / 0 failed / 6 skipped / 263 suites

```text
Focused (required evidence):
./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.AuthViewModelTest" --tests "com.example.optoapp.viewmodel.RecoveryStateTest" --tests "com.example.optoapp.viewmodel.RecoveryScreenSourceLockTest" --stacktrace --no-daemon --rerun-tasks
exit 0
AuthViewModelTest 23/0 fail; RecoveryStateTest 24/0 fail; RecoveryScreenSourceLockTest 3/0 fail

Full suite (config rules.verify + task 5.2):
./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon --rerun-tasks
exit 0
BUILD SUCCESSFUL in 3m 49s; suites=263 tests=2336 failures=0 errors=0 skipped=6
test_output_hash: sha256:ff42a048ca27af96be4d403b3e72f3b1f4c9de1c045a7de8b419473d3c9980da
```

**Coverage**: Not run this phase (`jacocoTestReport` not mandated). Project gate remains 30% instruction via `jacocoCoverageVerification`.

### Spec Compliance Matrix
Authoritative counts from `specs/android-password-recovery/spec.md`: **4 requirements**, **9 scenarios** (`### Requirement:` / `#### Scenario:` headings).

| Requirement | Scenario | Test / Evidence | Result |
|-------------|----------|-----------------|--------|
| Recovery Error Retryability Flag | Retryable failure sets isRetryable true | `AuthViewModelTest > updatePassword_retryableFailure_setsIsRetryableTrue` | COMPLIANT |
| Recovery Error Retryability Flag | Terminal or blank-token failure sets isRetryable false | `AuthViewModelTest > updatePassword_blankTokenFailure_setsIsRetryableFalse`, `updatePassword_terminal403_setsIsRetryableFalse` | COMPLIANT |
| Retryable NewPassword Error Keeps Form | Retryable Error keeps form and permits retry | `RecoveryScreenSourceLockTest > newPasswordScreen_retryableErrorKeepsFormBranch`; source: `NewPasswordScreen` `Error if !isRetryable` terminal vs form + banner | COMPLIANT |
| Retryable NewPassword Error Keeps Form | User retries without resetting recovery | `AuthViewModelTest > updatePassword_retryablePath_secondAttemptWithoutReset` (2x updatePassword, 0 clear) | COMPLIANT |
| Explicit-Exit Recovery State Reset | Dispose during LinkReceived navigation does not reset | Code inspection: no `DisposableEffect` in `RecoveryScreen.kt` / `NewPasswordScreen.kt`. No Compose instrumented dispose test (same residual as Part 1). | PARTIAL |
| Explicit-Exit Recovery State Reset | Explicit exit clears recovery state | `AuthViewModelTest > resetRecoveryState_setsIdleAndClearsDelegateToken` | COMPLIANT |
| Explicit-Exit Recovery State Reset | Login entry does not reset recovery | `RecoveryScreenSourceLockTest > loginScreen_entryEffect_doesNotCallResetRecoveryState`; LoginScreen has zero `resetRecoveryState` calls | COMPLIANT |
| Terminal Error CTA Navigates to Recovery | User taps terminal Error CTA | `RecoveryScreenSourceLockTest > newPasswordScreen_terminalErrorCtaResetsAndNavigatesRecovery`; CTA calls `resetRecoveryState` + `Route.Recovery` | COMPLIANT |
| Terminal Error CTA Navigates to Recovery | Terminal Error does not auto-navigate | Source: NewPassword terminal branch navigates only on CTA `onClick`; `MainActivity` `LaunchedEffect(recoveryState)` navigates only on `LinkReceived` | COMPLIANT |

**Compliance summary**: 8/9 COMPLIANT, 1/9 PARTIAL, 0 FAILING, 0 UNTESTED.

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Recovery Error Retryability Flag | Implemented | `RecoveryState.Error(message, isRetryable=false)`; `updatePassword` failure sets `isRetryable = hasPendingRecoveryToken()`; other Error sites keep default false |
| Retryable NewPassword Error Keeps Form | Implemented | Retryable Error stays on form branch with banner; Guardar calls `updatePassword` without reset |
| Explicit-Exit Recovery State Reset | Implemented | Login entry `LaunchedEffect` loads remembered email only; no Login `resetRecoveryState`; Recovery/NewPassword dispose→reset absent |
| Terminal Error CTA Navigates to Recovery | Implemented | Terminal `!isRetryable` CTA "Solicitar uno nuevo" → reset + navigate Recovery; no auto-nav on Error |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| `Error(message, isRetryable)` shape | Yes | AuthViewModel sealed class |
| Post-fail `hasPendingRecoveryToken()` | Yes | updatePassword failure path only |
| Blank / non-updatePassword Errors default false | Yes | sendRecoveryEmail / deep-link Error omit flag |
| Delete Login entry reset | Yes | LoginScreen entry effect email-only |
| AuthDelegate read-only for Part 2a | Yes | No Part 2a isRetryable edits required in Delegate; working tree still carries Part 1 taxonomy diffs vs HEAD |
| MainActivity untouched for C4/C5 | Yes | Part 2a did not add C4/C5 fixes; existing Part 1 `popUpTo(Recovery)` remains |

### TDD Compliance
| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | Yes | apply-progress TDD Cycle Evidence table present |
| All tasks have tests | Yes | 1.x/2.x AuthViewModel+RecoveryState; 3.x/4.x source locks; 5.x suite |
| RED confirmed (tests exist) | Yes | AuthViewModelTest isRetryable cases; RecoveryStateTest equality; RecoveryScreenSourceLockTest |
| GREEN confirmed (tests pass) | Yes | Focused 50/0 fail; full suite 2336/0 fail |
| Triangulation adequate | Yes | true vs false stubs; 401/403 blank-token; dual updatePassword retry |
| Safety Net for modified files | Yes | apply-progress Safety Net marked for Auth+Recovery baseline |

**TDD Compliance**: 6/6 checks passed

### Test Layer Distribution
| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit | 50 focused (23+24+3); suite 2336 | AuthViewModelTest, RecoveryStateTest, RecoveryScreenSourceLockTest | JUnit4 + MockK |
| Integration | 0 | — | Compose UI not required (no Robolectric) |
| E2E | 0 | — | N/A |
| **Total** | **50 focused / 2336 suite** | **3 primary** | |

### Changed File Coverage
Coverage analysis skipped — jacoco not run this verify phase (informational only; project threshold 30%).

### Assertion Quality
**Assertion quality**: All assertions verify real behavior

Focused contracts assert `isRetryable` values, dual `updatePassword` without clear, Login entry block exclusion of `resetRecoveryState`, and NewPassword branch/CTA source locks. No tautologies, ghost loops, or type-only-only assertions found in Part 2a test deltas.

### Quality Metrics
**Linter**: Not run (not mandated by rules.verify)
**Type Checker**: Passed via Kotlin compile in unit-test and assembleDebug tasks

### Issues Found
**CRITICAL**: None

**WARNING**:
1. Dispose-no-reset scenario remains PARTIAL — no Compose/NavHost instrumented test (accepted residual from Part 1; source inspection confirms no `DisposableEffect` reset).
2. Working tree still contains Part 1 diffs on `AuthDelegate.kt`, `MainActivity.kt`, and `RecoveryScreen.kt` vs HEAD; Part 2a apply correctly avoided further edits, but those files are not clean relative to committed main.

**SUGGESTION**:
1. Optional follow-up: one Compose/NavHost test locking dispose-no-reset if stronger gate desired before Parts 2b+.
2. Commit or archive Part 1 + Part 2a together so scope-lock files are not ambiguous across changes.

### Verdict
PASS WITH WARNINGS
Part 2a JD2-C2/C3 contracts are implemented and covered by passing unit/source-lock tests (8/9 COMPLIANT, 1/9 PARTIAL dispose residual); full suite and assembleDebug green.