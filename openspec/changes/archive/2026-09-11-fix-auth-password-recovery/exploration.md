## Exploration: fix-auth-password-recovery (PART 1)

### Scope

PART 1 of 4 (JD-authorized auth fixes). In scope:

| ID | Severity | Finding |
|----|----------|---------|
| JD-C1 | CONFIRMED CRITICAL | `RecoveryScreen` `DisposableEffect.onDispose` → `resetRecoveryState()` races `LinkReceived` → `NewPassword` navigation |
| JD-S4 | SUSPECT CRITICAL (user-authorized) | `AuthDelegate.updatePassword` clears `pendingRecoveryToken` before HTTP PUT succeeds |
| INFO | timeout | `HttpURLConnection` in `updatePassword` has no connect/read timeout |
| INFO | UX | NewPassword Error "Solicitar uno nuevo" only resets to Idle (token burned; no navigate to Recovery) |

Out of scope: Part 2 (S2/S3/null intent), Part 3 (S1/CreatePin), Part 4 (MembershipFetch/rol/NetworkRetryHelper).

### Current State

**Recovery state machine** (`AuthViewModel.RecoveryState`): `Idle → Loading → EmailSent | LinkReceived | PasswordUpdated | Error`.

**Happy path today:**

1. Login → Recovery → `sendRecoveryEmail` → EmailSent.
2. Email deep link `optoapp://auth#…&type=recovery` → `MainActivity.isRecoveryDeepLink` → `handleRecoveryDeepLink` stores fragment `access_token` in `AuthDelegate.pendingRecoveryToken` → `LinkReceived`.
3. `OptoAppNavigation` `LaunchedEffect(recoveryState)` navigates to `Route.NewPassword`.
4. User submits → `updatePassword` PUT `/auth/v1/user` with Bearer recovery token → `PasswordUpdated`.

**JD-C1 root cause (verified in code):**

```36:40:optoapp/src/main/java/com/example/optoapp/ui/screens/RecoveryScreen.kt
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRecoveryState()
        }
    }
```

NavHost disposes `Recovery` when pushing `NewPassword` (no `popUpTo`). `onDispose` sets `Idle`. Concurrently:

```45:52:optoapp/src/main/java/com/example/optoapp/ui/screens/NewPasswordScreen.kt
    LaunchedEffect(Unit) {
        if (recoveryState !is RecoveryState.LinkReceived && recoveryState !is RecoveryState.Loading &&
            recoveryState !is RecoveryState.Error && recoveryState !is RecoveryState.PasswordUpdated
        ) {
            navController.navigate(Route.Login.route) { … }
        }
    }
```

Result: user never keeps `LinkReceived` long enough to set a password after arriving via Recovery-in-stack (warm deep link). Cold start on Login avoids Recovery dispose but warm path is broken. `NewPasswordScreen` has the **same** dispose→reset pattern (lines 55–59), which is also unsafe for intermediate navigations.

Archived C4 (`2026-06-29-C4-Password-Recovery`) never required dispose-time reset; docs flagged "resetRecoveryState sin cuándo se llama" as low — later filled incorrectly with `onDispose`. Main specs (`android-auth`, `android-auth-onboarding`, `login-screen`) do **not** carry recovery requirements today (C4 archive only).

**JD-S4 root cause (verified):**

```377:380:optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt
    suspend fun updatePassword(newPassword: String): String? {
        val token = pendingRecoveryToken
        pendingRecoveryToken = ""
        if (token.isBlank()) return "Sesión de recuperación no válida."
```

Token cleared **before** PUT. Any HTTP/network failure leaves Error UI with empty token. Retry → "Sesión de recuperación no válida." Error CTA:

```144:150:optoapp/src/main/java/com/example/optoapp/ui/screens/NewPasswordScreen.kt
                        OutlinedButton(
                            onClick = { viewModel.resetRecoveryState() },
                            …
                        ) {
                            Text("Solicitar uno nuevo")
                        }
```

Only sets Idle; does **not** navigate to Recovery (C4 archived scenario required navigate). `resetRecoveryState()` also never clears `pendingRecoveryToken` in the delegate — intentional exit leaves token in memory until process death.

**Timeout INFO:** `UpdateChecker` already sets `connectTimeout`/`readTimeout` (8s/8s or 15s/300s). `updatePassword` sets neither → Loading can hang forever.

**Tests today:** `RecoveryStateTest` is characterization of sealed-class equality/flow transitions + duplicated password validation — **no** coverage of dispose race, token lifetime, timeouts, or Error CTA. No `AuthDelegate`/`AuthViewModel` recovery unit tests for `updatePassword`/`handleRecoveryDeepLink`.

### Affected Areas

- `optoapp/.../ui/screens/RecoveryScreen.kt` — remove/replace dispose reset (JD-C1)
- `optoapp/.../ui/screens/NewPasswordScreen.kt` — dispose reset; Error CTA navigate; guard timing
- `optoapp/.../viewmodel/auth/AuthDelegate.kt` — token clear timing; timeouts; explicit clear API
- `optoapp/.../viewmodel/AuthViewModel.kt` — `resetRecoveryState` should clear delegate token; Error→retry semantics
- `optoapp/.../MainActivity.kt` (`OptoAppNavigation`) — recovery nav already correct; may need `popUpTo` tweak if stack hygiene desired
- `optoapp/src/test/.../RecoveryStateTest.kt` + new AuthDelegate/ViewModel recovery tests — TDD
- Spec delta target: revive/extend recovery requirements under `android-auth` (or dedicated domain) from archived C4 + new failure/timeout scenarios

### Approaches

#### 1) When to reset recovery state (JD-C1)

1. **Explicit-exit only (recommended)** — Remove `DisposableEffect` reset from both Recovery and NewPassword. Call `resetRecoveryState()` only on: back/“Volver a login”, success “Volver a iniciar sesión”, Error “Solicitar uno nuevo” (after navigate), and intentional abort.
   - Pros: Fixes race; matches user intent; minimal code; easy to test at ViewModel level
   - Cons: Stale `LinkReceived`/`EmailSent` if user leaves via system Back without handlers (mitigate: also reset on Login `LaunchedEffect` entry or NavHost `OnDestinationChanged` for non-recovery routes)
   - Effort: Low

2. **Conditional dispose** — Keep dispose but skip reset when destination is `NewPassword` / when state is `LinkReceived|Loading|PasswordUpdated`.
   - Pros: Keeps auto-cleanup habit
   - Cons: Fragile (needs nav destination awareness in Screen); Compose dispose order races remain; Easy to regress
   - Effort: Medium

3. **NavHost `popUpTo(Recovery)` + single owner** — When navigating to NewPassword, pop Recovery so dispose is intentional and state already consumed; still remove blind reset or gate it.
   - Pros: Cleaner back stack (Back from NewPassword ≠ Recovery EmailSent)
   - Cons: Alone does **not** fix C1 if dispose still resets before NewPassword reads state; must combine with (1)
   - Effort: Low–Medium (combine with 1)

#### 2) Token lifetime across failed `updatePassword` (JD-S4 + Error UX)

1. **Clear only on HTTP success; keep on transport/5xx; clear on definitive auth failure (recommended)** — Local copy for request; restore or never clear until `code in 200..299`. On 401/403/expired, clear + Error message guiding re-request. Wire Error CTA to `resetRecoveryState()` + `navigate(Recovery)` (per archived C4). `resetRecoveryState()` MUST call `authDelegate.clearPendingRecoveryToken()`.
   - Pros: Retries work after flaky network; burns token only when server rejected or user exits; aligns UX with C4
   - Cons: Slightly more branching on status codes
   - Effort: Low–Medium

2. **Always keep until PasswordUpdated or explicit reset** — Never clear inside `updatePassword`; only clear in `resetRecoveryState` / success path after state set.
   - Pros: Simplest mental model
   - Cons: Token sits longer in memory after success if caller forgets clear; need discipline on success path
   - Effort: Low

3. **One-shot burn + force re-email on any failure** — Keep current clear-before-PUT; fix CTA to navigate Recovery only.
   - Pros: Smallest behavioral change for “security”
   - Cons: Does **not** fix S4 user pain (network blip = new email); fails authorized S4 intent
   - Effort: Low (rejected for S4)

#### 3) HTTP timeouts (INFO)

1. **Match UpdateChecker short timeouts (recommended)** — `connectTimeout = 10_000`, `readTimeout = 15_000` on recovery PUT; map `SocketTimeoutException` to user-friendly Error (token retained per S4).
   - Pros: Consistent project pattern; Loading cannot hang; testable via injected connection factory or extractable HTTP helper
   - Effort: Low

2. **Ktor/supabase-kt client instead of raw HttpURLConnection** — Reuse shared HTTP stack with timeouts.
   - Pros: One client
   - Cons: Original REST path exists to avoid supabase-kt `bad_jwt` / missing `sub`; high risk reopen; out of PART 1 budget
   - Effort: High (defer)

#### 4) Tests (strict TDD, JUnit 4 + MockK)

1. **Behavior-first unit tests (recommended)**
   - RED then GREEN:
     - `AuthDelegate.updatePassword` keeps token when PUT fails / throws (expose test seam: package-visible getter or `hasPendingRecoveryToken()` / inject `HttpURLConnection` factory).
     - Clears token only on 2xx.
     - Applies connect/read timeouts (verify on mock connection or wrapper).
     - `AuthViewModel.resetRecoveryState` clears delegate token + sets Idle.
     - `AuthViewModel.updatePassword` failure → `Error` and subsequent retry still invokes delegate with token (mock delegate sequential answers).
   - Characterization: document that Screens must not reset on dispose (comment/test naming); optional Compose UI test later — **not** required for PART 1 if ViewModel+Delegate cover contracts.
   - Effort: Medium

2. **UI-only Compose tests for dispose race**
   - Pros: Reproduces C1 literally
   - Cons: Heavy (NavHost + ViewModel); flaky; slow for TDD loop
   - Effort: High (defer; rely on removing dispose + ViewModel contracts)

### Recommendation

**Combine Explicit-exit reset (1.1) + optional `popUpTo(Recovery)` when entering NewPassword + clear-token-on-success (2.1) + 10s/15s timeouts (3.1) + Delegate/ViewModel TDD (4.1).**

Concrete target behavior:

1. Delete dispose→`resetRecoveryState` from `RecoveryScreen` and `NewPasswordScreen`.
2. Reset only on explicit user exits; Error “Solicitar uno nuevo” → clear + navigate `Route.Recovery`.
3. `updatePassword`: do not clear `pendingRecoveryToken` until 2xx; on 401/403 clear; always set timeouts.
4. `resetRecoveryState()` clears both UI state and delegate token.
5. Strict TDD: write failing Delegate/ViewModel tests first; then implement.

This is a focused Android-only change (no Supabase schema/RLS). Fits ~one PR under 400-line review budget if tests stay tight.

### Risks

- Leaving dispose reset on NewPassword alone can still wipe state on unexpected recomposition/nav.
- Status-code taxonomy wrong (e.g. treating 422 as retryable) may confuse UX — prefer: retryable = network/5xx/timeout; terminal = 401/403 + blank token.
- Extracting HttpURLConnection for tests may tempt a larger refactor; keep a small `openRecoveryPasswordConnection()` / factory seam.
- Stale `LinkReceived` if user lands on Login without reset — mitigate with Login-entry clear or destination listener.
- Main specs lack live recovery requirements; propose/spec must ADDED under `android-auth` (or restore domain) or verify will lack anchors.
- PART 2 deep-link races remain; do not “fix” OAuth/cold-start here.

### Open Questions

1. On terminal token failure (401), should CTA copy stay “Solicitar uno nuevo” (navigate Recovery) vs auto-navigate? Recommendation: keep button, navigate Recovery (archived C4).
2. Should Back from NewPassword return to Login (clear stack) or Recovery email form? Recommendation: Login + clear token (recovery link already consumed in UX terms).
3. Exact timeout values: 10s/15s vs 8s/8s like UpdateChecker? Recommendation: 10s connect / 15s read for auth PUT.

### Ready for Proposal

**Yes.** Orchestrator should run **sdd-propose** for `fix-auth-password-recovery` with the recommended combo above, strict TDD, PART 1 scope only, and delta specs covering: no dispose reset; token lifetime; timeouts; Error CTA navigation.
