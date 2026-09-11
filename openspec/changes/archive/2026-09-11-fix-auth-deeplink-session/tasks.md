# Tasks: Fix Auth Deep-Link Session (Part 2b)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180–280 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | AuthDelegate C5+C1 | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.auth.AuthDelegateRecoveryTest --tests com.example.optoapp.viewmodel.AuthDelegateTest --stacktrace` | N/A — unit-only Intent/auth; no emulator deep-link harness in CI | Revert `AuthDelegate.kt` + Delegate unit tests only |
| 2 | AuthViewModel await + null-data Error | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --stacktrace` | N/A — MockK ViewModel; no device needed | Revert `AuthViewModel.kt` + `AuthViewModelTest.kt` |
| 3 | MainActivity serialize + onNewIntent gate | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --tests com.example.optoapp.viewmodel.auth.AuthDelegateRecoveryTest --stacktrace` | Manual cold-start VIEW then warm `onNewIntent` (optional smoke); not required for unit gate | Revert `MainActivity.kt` only; Manifest untouched |
| 4 | Full suite + scope lock | PR 1 | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | N/A — full unit suite is verify gate | Revert Part 2b Android/test commits as a set |

## Phase 1: AuthDelegate RED→GREEN (C5 + C1)

- [x] 1.1 RED — In `optoapp/src/test/java/com/example/optoapp/viewmodel/auth/AuthDelegateRecoveryTest.kt` (or sibling OAuth deep-link test under `viewmodel/auth/`), assert `handleAuthDeepLinkIntent` with null `data` returns non-null `"Enlace inválido"`; characterization that Uri `Log.d` must not contain full Uri / `access_token` (source lock OK).
- [x] 1.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt`, OAuth null `intent?.data` → `"Enlace inválido"`; delete OAuth+recovery Uri `Log.d` lines; no `resolvePostLogin` path from Delegate.
- [x] 1.3 Confirm focused Delegate tests green (Unit 1 command).

## Phase 2: AuthViewModel RED→GREEN (await APIs)

- [x] 2.1 RED — In `optoapp/src/test/java/com/example/optoapp/viewmodel/AuthViewModelTest.kt`, null OAuth data → `AuthState.Error`; `verify(exactly=0) { resolvePostLogin(...) }` / no Room wipe; RED for suspend `awaitHandle*` join before session if MockK order-testable.
- [x] 2.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt`, extract `awaitHandleAuthDeepLinkIntent` / `awaitHandleRecoveryDeepLink`; Job wrappers keep fire-and-forget; Loading/LinkReceived/CancellationException preserved; Error skips `resolvePostLogin`.
- [x] 2.3 Confirm focused VM tests green (Unit 2 command).

## Phase 3: MainActivity GREEN (C4 + warm gate)

- [x] 3.1 GREEN — In `optoapp/src/main/java/com/example/optoapp/MainActivity.kt`, `lifecycleScope.launch {` if `ACTION_VIEW` await recovery/OAuth handler else skip; **then** `checkExistingSession()` }; never concurrent.
- [x] 3.2 GREEN — Same file: `onNewIntent` requires `ACTION_VIEW` + non-null `data` before handlers; else no-op.
- [x] 3.3 Do not edit `optoapp/src/main/AndroidManifest.xml` (`singleTask` deferred).

## Phase 4: Scope Lock + Verify

- [x] 4.1 Scope lock: no C6–C8, no CreatePin await, no Membership Empty, no ON_RESUME Loading→Idle, no `prepareOpticaSelection`, no Login entry reset reopen, no `SignOutScope.GLOBAL` change.
- [x] 4.2 Focused: Unit 1–3 commands green; Part1/2a recovery suite still green.
- [x] 4.3 Full: `./gradlew :optoapp:testDebugUnitTest --stacktrace` green.
