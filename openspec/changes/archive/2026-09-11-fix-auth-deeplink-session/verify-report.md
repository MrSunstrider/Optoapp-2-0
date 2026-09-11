```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:4269e8c2608bf328004e1b85ca0cf19365e5ef36965c0497e9fe20bd7ff5e0cf
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 4/4
scenarios: 8/8
test_command: ./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon --rerun-tasks
test_exit_code: 0
test_output_hash: sha256:c77510a1d269a43e86b3607dbdf1ae62ea44b5813fe9c7b3214b182f2b8c111e
build_command: ./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
build_exit_code: 0
build_output_hash: sha256:c417a74e57ca1782741f3965852af643a5fa5e80645e3a65a673e00e83427c83
```

## Verification Report

**Change**: fix-auth-deeplink-session
**Version**: N/A (delta capability `android-auth`, Part 2b JD2-C1/C4/C5)
**Mode**: Strict TDD
**Persistence**: hybrid

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 12 |
| Tasks complete | 12 |
| Tasks incomplete | 0 |

All Phase 1–4 checkboxes in `openspec/changes/fix-auth-deeplink-session/tasks.md` are `[x]`. Apply-progress reports `applyState: all_done`.

### Build & Tests Execution
**Build**: Passed

```text
./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
exit 0
BUILD SUCCESSFUL in 59s
build_output_hash: sha256:c417a74e57ca1782741f3965852af643a5fa5e80645e3a65a673e00e83427c83
```

**Tests**: 2350 tests / 0 failed / 6 skipped / 265 suites

```text
Focused (required evidence):
./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.auth.AuthDelegateDeepLinkTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --tests com.example.optoapp.viewmodel.AuthDeepLinkBootstrapSourceLockTest --stacktrace --no-daemon
exit 0
AuthDelegateDeepLinkTest 5/0 fail; AuthViewModelTest 29/0 fail; AuthDeepLinkBootstrapSourceLockTest 3/0 fail

Full suite (config rules.verify + task 4.3):
./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon --rerun-tasks
exit 0
BUILD SUCCESSFUL in 4m 7s; suites=265 tests=2350 failures=0 errors=0 skipped=6
test_output_hash: sha256:c77510a1d269a43e86b3607dbdf1ae62ea44b5813fe9c7b3214b182f2b8c111e
```

**Coverage**: Not run this phase (`jacocoTestReport` not mandated). Project gate remains 30% instruction via `jacocoCoverageVerification`.

### Spec Compliance Matrix
Authoritative counts from `specs/android-auth/spec.md`: **4 requirements**, **8 scenarios** (`### Requirement:` / `#### Scenario:` headings).

| Requirement | Scenario | Test / Evidence | Result |
|-------------|----------|-----------------|--------|
| Cold Start Awaits Deep Link Before Session Check | VIEW deep link completes before session check | `AuthViewModelTest > awaitAuthDeepLink_thenCheckExistingSession_delegateOrderPreserved`; `AuthDeepLinkBootstrapSourceLockTest > onCreate_awaitsViewDeepLinkBeforeCheckExistingSession` | COMPLIANT |
| Cold Start Awaits Deep Link Before Session Check | Non-VIEW cold start checks session only | Source: `MainActivity.onCreate` gates awaits under `ACTION_VIEW` then always `checkExistingSession()`. Bootstrap source lock asserts session after awaits but does not explicitly assert Non-VIEW skips handlers. | PARTIAL |
| Cold Start Awaits Deep Link Before Session Check | GLOBAL sign-out and launchMode unchanged | `AuthDelegateDeepLinkTest > logout_doesNotClearPendingRecoveryToken`; `AuthDeepLinkBootstrapSourceLockTest > scopeLock_noSingleTaskOrLoginResetReopen`; Manifest has no `launchMode`/`singleTask`; `logout()` still uses `SignOutScope.GLOBAL` | COMPLIANT |
| OAuth Null Deep Link Data Is Fail-Closed | Null data surfaces error without post-login | `AuthDelegateDeepLinkTest > handleAuthDeepLinkIntent_nullData_returnsEnlaceInvalido` (+ null intent); `AuthViewModelTest > awaitHandleAuthDeepLinkIntent_nullDataError_setsAuthErrorWithoutResolvePostLogin` | COMPLIANT |
| OAuth Null Deep Link Data Is Fail-Closed | Null data does not wipe Room | Same Error early-return path; `coVerify(exactly=0) resolvePostLogin`; Delegate returns before session work (wipe only via `logout`/`resetLocalStoreForNewAuthSession`) | COMPLIANT |
| onNewIntent Ignores Non-VIEW Or Null-Data OAuth | Non-VIEW onNewIntent is OAuth no-op | `AuthDeepLinkBootstrapSourceLockTest > onNewIntent_requiresActionViewAndNonNullData`; source gate `ACTION_VIEW && data != null` | COMPLIANT |
| onNewIntent Ignores Non-VIEW Or Null-Data OAuth | Null-data onNewIntent is OAuth no-op | Same source lock + `intent.data != null` gate before handlers | COMPLIANT |
| Deep Link Handlers Do Not Log Secrets | Full URI with tokens is not logged | `AuthDelegateDeepLinkTest > handleRecoveryDeepLink_withTokenUri_doesNotLogFullUriOrAccessToken`; `authDelegateSource_doesNotLogFullDeepLinkUri` | COMPLIANT |

**Compliance summary**: 7/8 COMPLIANT, 1/8 PARTIAL, 0 FAILING, 0 UNTESTED.

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Cold Start Awaits Deep Link Before Session Check | Implemented | `lifecycleScope.launch { if ACTION_VIEW awaitHandle*; checkExistingSession() }`; suspend await APIs + Job wrappers |
| OAuth Null Deep Link Data Is Fail-Closed | Implemented | `intent?.data == null` → `"Enlace inválido"`; VM Error returns before `resolvePostLogin` |
| onNewIntent Ignores Non-VIEW Or Null-Data OAuth | Implemented | Requires `ACTION_VIEW` + non-null `data` else no-op |
| Deep Link Handlers Do Not Log Secrets | Implemented | OAuth/recovery Uri `Log.d` lines removed; remaining Log.d are non-secret |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| Suspend bodies + thin Job wrappers | Yes | `awaitHandle*` + `viewModelScope.launch` wrappers |
| lifecycleScope serialize cold-start | Yes | Await then session; setContent immediate |
| Keep MainActivity branch | Yes | `isRecoveryDeepLink` stays in Activity |
| Null OAuth → `"Enlace inválido"` | Yes | Mirrors recovery |
| onNewIntent ACTION_VIEW+data | Yes | Else no-op |
| Delete Uri Log.d | Yes | No secret Uri logs remain |
| Serialize only; GLOBAL unchanged | Yes | `SignOutScope.GLOBAL` kept; pendingRecoveryToken survives logout |
| Manifest singleTask deferred | Yes | No launchMode change |

### TDD Compliance
| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | Yes | Found in apply-progress TDD Cycle Evidence table |
| All tasks have tests | Yes | 12/12 tasks mapped to AuthDelegateDeepLinkTest / AuthViewModelTest / AuthDeepLinkBootstrapSourceLockTest / suite |
| RED confirmed (tests exist) | Yes | 3 test files verified on disk |
| GREEN confirmed (tests pass) | Yes | Focused + full suite exit 0 |
| Triangulation adequate | Yes | Null data + null intent + log + order + onNewIntent + scope lock |
| Safety Net for modified files | Yes | Recovery/Delegate/VM existing suites + full suite |

**TDD Compliance**: 6/6 checks passed

### Test Layer Distribution
| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit | 37 focused (+ suite) | 3 primary | JUnit 4 + MockK + coroutines-test |
| Integration | 0 | 0 | Room available; not required |
| E2E | 0 | 0 | Emulator harness N/A per tasks |
| **Total** | **37 focused / 2350 suite** | **3+** | |

### Changed File Coverage
Coverage analysis skipped — jacoco not run this phase (informational only).

### Assertion Quality
**Assertion quality**: All assertions verify real behavior (error strings, coVerify order/absence, source locks for Activity/Manifest contracts, Log.d secret absence). No tautologies or ghost loops found.

### Quality Metrics
**Linter**: Not run this phase
**Type Checker**: Passed via Kotlin compile in unit-test and assembleDebug tasks
**Build**: Passed (`assembleDebug`)

### Issues Found
**CRITICAL**: None
**WARNING**:
- Non-VIEW cold-start scenario is PARTIAL: bootstrap source lock proves serialize order and session always runs, but does not explicitly assert that Non-VIEW skips OAuth/recovery await handlers (implementation gate exists in `MainActivity.onCreate`).
**SUGGESTION**:
- Add an explicit source-lock assertion that `onCreate` contains `ACTION_VIEW` before await calls (closes Non-VIEW PARTIAL without Robolectric).
- Manual device smoke (cold-start VIEW then warm `onNewIntent`) remains optional per tasks.

### Verdict
PASS WITH WARNINGS
7/8 scenarios COMPLIANT, 1 PARTIAL (Non-VIEW cold-start explicit assertion); 12/12 tasks complete; focused + full unit suite green (2350/0 fail); assembleDebug green; design checklist satisfied; Strict TDD evidence complete.