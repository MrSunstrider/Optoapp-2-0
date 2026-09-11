# Tasks: Fix Auth PIN Logout (JD2-C8)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 220–360 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR (work-unit commits) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | `clearStoredPin` + fakes | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.SecurityManagerTest --tests com.example.optoapp.viewmodel.PinDelegateTest --stacktrace` | N/A — unit/ESP prefs; no emulator required for gate | Revert `SecurityManager.kt` + SM/PinDelegate fake/test updates |
| 2 | `clearSession` + logout wipe | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.SessionManagerTest --tests com.example.optoapp.viewmodel.AuthDelegateTest --stacktrace` | N/A — MockK logout; no device | Revert `SessionManager.kt` + `AuthDelegate.kt` + matching tests |
| 3 | Await createPin + CreatePin gate | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.PinDelegateTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --stacktrace` | Manual CreatePin confirm smoke optional; not CI gate | Revert `PinDelegate.kt` + `AuthViewModel.kt` + `CreatePinScreen.kt` + tests |
| 4 | Scope lock + full suite | PR 1 | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | N/A — full unit suite is verify gate | Revert JD2-C8 Android/test commits as a set |

## Phase 1: SecurityManager `clearStoredPin` (RED→GREEN)

- [x] 1.1 RED — In `optoapp/src/test/java/com/example/optoapp/data/SecurityManagerTest.kt`, after `savePin`, assert `clearStoredPin` → empty `userPin` and `pinHasBeenSet=false` (spec: Logout empties PIN secret).
- [x] 1.2 RED — Update `FakeSecurityManager` in `optoapp/src/test/java/com/example/optoapp/viewmodel/PinDelegateTest.kt` (+ any other `ISecurityManager` fakes/mocks) so compile fails until interface grows `clearStoredPin`.
- [x] 1.3 GREEN — Add `suspend fun clearStoredPin()` to `ISecurityManager` / `SecurityManager.kt`: remove `user_pin`, `_pinFlow=""`, DataStore flag false.
- [x] 1.4 Confirm Unit 1 focused command green.

## Phase 2: SessionManager clears `PIN_HAS_BEEN_SET`

- [x] 2.1 RED — In `optoapp/src/test/java/com/example/optoapp/data/SessionManagerTest.kt`, after setting pin-has-been-set, `clearSession` → `pinHasBeenSet=false` / key removed.
- [x] 2.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/data/SessionManager.kt`, `clearSession` `prefs.remove(PIN_HAS_BEEN_SET)` (keep `isPinRequired=false`).
- [x] 2.3 Confirm SessionManager focused tests green.

## Phase 3: AuthDelegate logout wipe (despite signOut fail)

- [x] 3.1 RED — In `optoapp/src/test/java/com/example/optoapp/viewmodel/AuthDelegateTest.kt`, logout with signOut `IOException` → `coVerify { securityManager.clearStoredPin() }` and session cleared (spec: wipe despite remote failure).
- [x] 3.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt`, after Room wipe call `clearStoredPin()` then `clearSession()`; ignore non-cancellation signOut errors; rethrow `CancellationException` only; no `SignOutScope`/`launchMode` change.
- [x] 3.3 Confirm Unit 2 focused command green.

## Phase 4: Awaitable Create PIN + screen gate

- [x] 4.1 RED — In `PinDelegateTest.kt` / `AuthViewModelTest.kt`: invalid PIN → `false` (no persist); valid → `true` + flag set; `createPinAwaitingSuccess` returns Boolean (spec: Failure stays / Success after persist).
- [x] 4.2 GREEN — `PinDelegate.createPin` → `Boolean`; `AuthViewModel.createPinAwaitingSuccess(pin): Boolean`; keep fire-and-forget `createPin` if callers need it.
- [x] 4.3 GREEN — In `optoapp/src/main/java/com/example/optoapp/ui/screens/CreatePinScreen.kt`, navigate Main only when await returns `true`; on `false` stay + show error.
- [x] 4.4 Confirm Unit 3 focused command green.

## Phase 5: Scope Lock (optional PIN; no C6/C7)

- [x] 5.1 Scope lock: no C6/C7 membership/JWT, deeplink, recovery, ON_RESUME, mandatory PIN, per-uid keying, `SignOutScope.GLOBAL`, or `launchMode` edits.
- [x] 5.2 Confirm optional-PIN PostLoginNavigation / existing nav tests still green (spec: Optional PIN skips create).

## Phase 6: Verify

- [x] 6.1 Focused: Unit 1–3 commands all green.
- [x] 6.2 Full: `./gradlew :optoapp:testDebugUnitTest --stacktrace` green.
- [x] 6.3 Optional only: instrumented ESP wipe in `SecurityManagerInstrumentedTest.kt` — not required for unit gate.
