## Exploration: fix-auth-recovery-regressions (Part 2a)

### Scope

Part 2a only — Judgment Day ROUND 2 **INTRODUCED** regressions from Part 1 `fix-auth-password-recovery`:

| ID | Severity | Finding |
|----|----------|---------|
| JD2-C2 | CRITICAL/HIGH | `NewPasswordScreen` Error branch hides password form; sole CTA "Solicitar uno nuevo" → `resetRecoveryState()` burns token that `AuthDelegate` kept for retryable failures |
| JD2-C3 | CRITICAL/HIGH | `LoginScreen` `LaunchedEffect(Unit)` always `resetRecoveryState()`; cold-start `startDestination=Login` races recovery `LinkReceived` and clears token |

**Out of scope:** JD2-C1 Log.d tokens, C4 cold-start session race, C5 null intent.data wipe, C6 rol admin, C7 NetworkRetry, C8 PIN logout; suspects prepareOpticaSelection / ON_RESUME / CreatePin / Membership Empty.

**Strict TDD:** yes (`openspec/config.yaml`).

### Current State

Part 1 correctly fixed dispose races and Delegate token taxonomy, but UI/state wiring contradicts the same Part 1 design data flow:

```
retryable → keep token → Error → retry on NewPassword
401/403   → clear token → Error → CTA → reset + Recovery
```

**Delegate (correct):** `AuthDelegate.updatePassword` clears `pendingRecoveryToken` only on HTTP 2xx or 401/403; keeps token on `IOException`/timeout/5xx/other 4xx (`422`, `400`, `429`, …). Covered by `AuthDelegateRecoveryTest`.

**ViewModel (partial):** `updatePassword` maps any non-null String to undifferentiated `RecoveryState.Error(message)`. `resetRecoveryState()` → Idle + `clearPendingRecoveryToken()`. No `isRetryable` on Error. `hasPendingRecoveryToken()` exists on Delegate but is not exposed for UI branching.

**NewPasswordScreen (C2 bug):** `when (recoveryState)` treats **all** `Error` as terminal:
- Hides password fields / submit
- Single CTA "Solicitar uno nuevo" → `resetRecoveryState()` + navigate Recovery
- User cannot retry even when token is still in memory
- Violates spec scenarios *Retryable failure retains token* / *Timeout is retryable* and design "retry on NewPassword"

**LoginScreen (C3 bug):** Part 1 task 3.4 added entry stale guard:

```kotlin
LaunchedEffect(Unit) {
    viewModel.resetRecoveryState()
    // remembered email...
}
```

Cold-start path (`MainActivity`):
1. `startDestination = Route.Login`
2. `onCreate` may `handleRecoveryDeepLink` (async `viewModelScope.launch`)
3. Login composes immediately → entry reset always runs
4. If `LinkReceived` (or Loading + token) wins the race before/during that effect, reset burns token and aborts recovery
5. Spec: reset **only** on explicit exits — not on Login mount

Part 1 design table chose "Login entry resets" as stale-state guard — that decision **introduced** JD2-C3.

**Explicit exits already present (keep):**
- NewPassword Back → reset + Login
- NewPassword PasswordUpdated CTA → reset + Login
- NewPassword terminal Error CTA → reset + Recovery (correct for burned token)
- Recovery Back / EmailSent / Error → Login paths already call reset

### Affected Areas

- `optoapp/.../ui/screens/NewPasswordScreen.kt` — Error UI must split retryable vs terminal
- `optoapp/.../ui/screens/LoginScreen.kt` — remove or gate entry `resetRecoveryState()`
- `optoapp/.../viewmodel/AuthViewModel.kt` — `RecoveryState.Error` shape and/or expose retryability; `updatePassword` mapping
- `optoapp/.../viewmodel/auth/AuthDelegate.kt` — likely **read-only** for taxonomy (already correct); maybe expose `hasPendingRecoveryToken` via VM
- `optoapp/.../MainActivity.kt` — cold-start + `LaunchedEffect(recoveryState)` LinkReceived nav (context; avoid C4/C5 scope creep)
- `optoapp/.../ui/screens/RecoveryScreen.kt` — unchanged for C2/C3; confirm no new dispose reset
- Tests: `AuthViewModelTest.kt` (Error retryable flag / no Login-entry contract); optional Compose/unit helpers; Delegate tests should stay green without taxonomy rewrite
- Spec/design delta under `openspec/changes/fix-auth-recovery-regressions/` (new change; do not silently rewrite Part 1 artifacts)

### Approaches

#### JD2-C2 — NewPassword Error / retry

1. **Error(isRetryable) + split UI (recommended)** — Extend `RecoveryState.Error(message, isRetryable: Boolean)`. ViewModel sets `isRetryable = authDelegate.hasPendingRecoveryToken()` after failed `updatePassword` (and false for blank-token / invalid-link Errors). NewPassword:
   - `isRetryable == true` → keep form visible, show inline error, primary "Reintentar" / re-enable Guardar (no reset)
   - `isRetryable == false` → current terminal UI + "Solicitar uno nuevo" → reset + Recovery
   - Pros: Matches Part 1 design table; single source of truth (token presence after taxonomy); testable without Compose; small surface
   - Cons: Sealed-class shape change; update existing Error equality tests
   - Effort: Low–Medium

2. **UI-only branch on `hasPendingRecoveryToken()`** — Keep `Error(message)`; screen collects `hasPendingRecoveryToken` from VM. Same UX split.
   - Pros: No sealed-class change
   - Cons: UI owns taxonomy inference; Loading/Error edge timing; weaker unit tests at VM layer
   - Effort: Low

3. **Sealed Error.Retryable / Error.Terminal** — Replace flat Error with two subtypes; Delegate returns sealed `UpdatePasswordResult`.
   - Pros: Strongest typing end-to-end
   - Cons: Larger API churn than needed for Part 2a; outsizes 400-line budget if combined with C3
   - Effort: Medium–High

#### JD2-C3 — Login entry reset

1. **Remove Login entry reset entirely (recommended primary)** — Delete `resetRecoveryState()` from `LoginScreen` `LaunchedEffect(Unit)`. Rely on existing explicit-exit handlers. Aligns with spec *Explicit-Exit Recovery State Reset*.
   - Pros: Eliminates cold-start race; matches spec; one-line delete + test/doc update
   - Cons: Stale `EmailSent`/`Error` possible if user leaves Recovery without Back/CTA (accept residual UX; Recovery "Intentar de nuevo" / Back still clear)
   - Effort: Low

2. **Conditional skip when recovery active** — Reset only if `recoveryState` is Idle/EmailSent/Error **and** `!hasPendingRecoveryToken()` **and** not LinkReceived/Loading/PasswordUpdated.
   - Pros: Keeps stale-guard intent for EmailSent
   - Cons: Still races if token set but state not yet LinkReceived in same frame; more branches
   - Effort: Low–Medium

3. **Reset only when navigating Login intentionally from recovery** — Pass a nav arg / shared event; Login never auto-resets.
   - Pros: Precise
   - Cons: Touch every nav call site; easy to miss; higher effort for same outcome as (1)
   - Effort: Medium

### Recommendation

**Ship both fixes together in this change (Part 2a):**

1. **C2:** Adopt approach **Error(isRetryable)** (or equivalent VM-exposed flag derived from `hasPendingRecoveryToken()` after `updatePassword`). NewPassword must keep the password form for retryable Errors and must **not** call `resetRecoveryState` on retry. Terminal CTA stays "Solicitar uno nuevo" → reset + Recovery (spec *Terminal Error CTA*).

2. **C3:** **Remove** unconditional Login entry `resetRecoveryState()`. Optional follow-up (same PR if tiny): when entering Recovery from Login "Olvidaste", call reset only if state is EmailSent/Error without pending token — not required to close JD2-C3.

3. **TDD order:** RED ViewModel contracts for `Error(isRetryable=true|false)` from Delegate keep/clear taxonomy → GREEN VM → GREEN NewPassword/Login wiring → focused unit tests; no Delegate taxonomy rewrite unless a hole appears.

4. **Do not** re-open Part 1 dispose policy, timeouts, or popUpTo(Recovery) — those remain correct.

### Risks

- **Stale EmailSent after removing Login reset** — user backs out of Recovery without CTA; mitigated by Recovery Back handlers and "Intentar de nuevo"; document as accepted residual vs token-burn race
- **Misclassifying 422 as retryable in UI** — Delegate already keeps token for non-auth 4xx; UI must follow token presence, not HTTP code parsing in the screen
- **Blank-token Error** — must be terminal (`isRetryable=false`) so CTA still offers new link
- **Scope creep into C4/C5** — cold-start session/OAuth wipe is adjacent in `MainActivity` but must stay out of Part 2a
- **Test gap** — Part 1 had no screen-level test proving retryable Error keeps form; add VM-level + optional lightweight UI/state tests under strict TDD
- **Regression of Part 1 CTA requirement** — terminal path must still navigate Recovery only on CTA tap (no auto-nav)

### Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `fix-auth-recovery-regressions` with:
- Intent: close JD2-C2 + JD2-C3 only
- Approach: Error retryable split + remove Login entry reset
- Rollback: revert Android UI/VM commits for this change
- Strict TDD: true
- Explicit out-of-scope list matching JD2 C1/C4–C8 + suspects
