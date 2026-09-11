## Exploration: fix-auth-coldstart-recovery-nav

### Scope

Judgment Day JD3-S1 (Judge-B-only CRITICAL on scoped rejudgment; user-authorized fix): after Part 2b serializes cold-start as `awaitHandleRecoveryDeepLink` THEN `checkExistingSession`, a still-valid GoTrue session causes `OptoAppNavigation`'s cold-start `LaunchedEffect` to navigate Main/Pin/CreatePin with `popUpTo(graph) { inclusive = true }`, destroying `NewPassword` while `pendingRecoveryToken` remains.

**In scope:** cold-start authenticated restore vs recovery navigation coupling; pure logic in `ColdStartNavigation` if feasible; specs/tests for the guard.

**Out of scope:** reopening Part 2b serialization / GLOBAL logout semantics / launchMode; password-update PUT taxonomy; Login dispose/reset contracts (already closed); membership/JWT leftovers.

### Current State

#### Bootstrap order (Part 2b — correct, incomplete)

`MainActivity.onCreate` (~64–73):

1. If `ACTION_VIEW` + recovery fragment → `awaitHandleRecoveryDeepLink(intent)` (sets `pendingRecoveryToken`, then `RecoveryState.LinkReceived` or `Error`).
2. Else if `ACTION_VIEW` OAuth → `awaitHandleAuthDeepLinkIntent`.
3. Then always `checkExistingSession()`.

Recovery deep-link handling does **not** exchange the recovery token into GoTrue as the active session; it only stores the raw fragment `access_token` for a later REST PUT. Any pre-existing GoTrue session therefore remains valid after a successful recovery parse.

`AuthDelegate.logout()` intentionally does **not** clear `pendingRecoveryToken` (cold-start serialize invariant; covered by `AuthDelegateDeepLinkTest`).

#### Race / destroy path (JD3-S1)

`OptoAppNavigation` cold-start `LaunchedEffect(isAuthChecked, isLoggedIn, opticaId, isPinRequired, needsOnboarding, pinHasBeenSet)` (~143–167):

- Ignores `recoveryState` and `pendingRecoveryToken`.
- When `isAuthChecked && isLoggedIn` and PIN flags ready → `ColdStartNavigation.dest` → `navigate(dest) { popUpTo(graph.id) { inclusive = true } }`.
- That inclusive graph pop **wipes** whatever recovery navigation already placed (`NewPassword`).

Parallel recovery `LaunchedEffect(recoveryState)` (~182–187) navigates `NewPassword` with `popUpTo(Recovery)` only when `LinkReceived`.

Because await completes **before** session check, by the time `isAuthChecked` flips true the recovery state is already `LinkReceived` (success) or `Error` (fail). PIN readiness can delay the cold-start effect further, but recovery is already terminal — so when cold-start finally runs it sees a logged-in session **and** active recovery, then destroys NewPassword.

Net: token still pending, UI left on Main/Pin/CreatePin (or stack chaos if effects reorder), user cannot finish password reset.

#### Asymmetry with session-invalidation guard

Session-invalidation `LaunchedEffect(isLoggedIn)` (~169–179) already skips Login wipe when:

- `recoveryState is LinkReceived` OR `recoveryState is PasswordUpdated`

Cold-start restore has **no** equivalent guard.

#### Pure logic today

`ColdStartNavigation` only decides Login vs `postLoginDest` from auth-check/session flags. It has no recovery awareness. Existing unit tests (`ColdStartNavigationTest`) cover incomplete check / logged-in restore / pin readiness — no recovery cases.

`PostLoginNavigation` is unrelated (membership/PIN dest only).

#### Spec gap

- `android-auth`: requires cold-start restore of authenticated routes **and** deep-link-before-session-check serialization — does **not** say recovery `LinkReceived` must suppress authenticated cold-start restore.
- `android-password-recovery`: protects NewPassword usability across dispose / Login-entry; assumes NewPassword remains reachable after `LinkReceived` — does not name the cold-start `popUpTo(graph)` destroyer.

### Affected Areas

- `optoapp/.../MainActivity.kt` (`OptoAppNavigation`) — cold-start `LaunchedEffect` must consult recovery (and likely add `recoveryState` to keys); keep recovery and invalidation effects aligned.
- `optoapp/.../viewmodel/auth/ColdStartNavigation.kt` — preferred home for testable suppress/restore predicate.
- `optoapp/.../viewmodel/auth/ColdStartNavigationTest.kt` — RED/GREEN for recovery-blocking cases.
- `optoapp/.../viewmodel/AuthViewModel.kt` / `AuthDelegate.kt` — read-only for this fix unless a thin `hasPendingRecoveryToken` exposure is chosen; prefer not changing logout/token lifetime.
- Specs: delta likely `android-auth` (cold-start restore exception) and/or `android-password-recovery` (NewPassword must survive cold-start session restore when `LinkReceived`).
- Optional source-lock / characterization test mirroring `AuthDeepLinkBootstrapSourceLockTest` if Compose wiring must stay coupled.

### Approaches

1. **Guard in `ColdStartNavigation` (mirror invalidation)** — Extend pure API, e.g. `recoveryBlocksColdStartRestore(recoveryState)` or `shouldNavigateAwayFromLogin(..., recoveryBlocksRestore)`, returning “stay on current / do not restore session route” when `LinkReceived` or `PasswordUpdated`. Caller sets `coldStartHandled = true` without `navigate` (or treats Login-equivalent as no-op). Wire `recoveryState` into the cold-start `LaunchedEffect` keys.
   - Pros: Matches existing session-invalidation policy; unit-testable without Compose; minimal surface; aligns with Part 2b outcome.
   - Cons: Compose still must pass the flag correctly; `coldStartHandled=true` while suppressing means no later auto-restore after recovery Idle (acceptable: NewPassword exits go Login explicitly).
   - Effort: Low

2. **Compose-only early return** — In `LaunchedEffect`, `if (recoveryState is LinkReceived || PasswordUpdated) { coldStartHandled = true; return }` without changing `ColdStartNavigation`.
   - Pros: Smallest diff.
   - Cons: Logic untested except fragile source locks; duplicates policy already partly encoded for invalidation; drifts from preferred pure-logic home.
   - Effort: Low

3. **Recovery destination as cold-start dest** — Teach `ColdStartNavigation.dest` to return `NewPassword` when recovery active, and navigate there instead of Main/Pin.
   - Pros: Single navigator path.
   - Cons: Overlaps with existing `LaunchedEffect(recoveryState)`; risk of double navigate / wrong `popUpTo`; couples recovery stack hygiene into cold-start; higher regression risk.
   - Effort: Medium

4. **Invalidate GoTrue session on recovery deep link** — Sign out / clear session when accepting recovery token so cold-start sees `isLoggedIn=false`.
   - Pros: Makes session “match” recovery intent.
   - Cons: Touches GLOBAL/logout invariants Part 2b carefully preserved; can race Room wipe / invalidation effects; larger blast radius than a nav guard; not required for PUT (uses raw token).
   - Effort: High

### Recommendation

**Approach 1.** Suppress authenticated cold-start restore when recovery is `LinkReceived` or `PasswordUpdated` (same set as the session-invalidation guard). Implement the predicate in `ColdStartNavigation` for Strict TDD; call it from the cold-start `LaunchedEffect` before inclusive `popUpTo(graph)` navigate; mark cold start handled so PIN/optica churn does not retry into Main later.

Do **not** suppress on `Error` alone: after a failed recovery parse, falling through to normal session restore is desirable. Do **not** need to suppress `Loading` on the serialized cold-start path (await finishes before `isAuthChecked`); warm `onNewIntent` already uses fire-and-forget recovery without waiting on session check — leave that out of scope unless propose expands it.

### Risks

- Setting `coldStartHandled=true` while suppressing leaves a valid GoTrue session without auto-Main until user finishes or aborts recovery — intended, but must match product exit paths (Back / success → Login).
- If only Compose is patched, future cold-start refactors can regress JD3-S1 without unit coverage.
- Over-broad suppress (`Loading`/`Error`/`EmailSent`/`hasPendingRecoveryToken`) could trap users on Login with a healthy session after a failed or aborted link.
- Spec wording must carve an exception into “valid session MUST leave Login” for active recovery, or conflict with `android-auth` cold-start restore.
- Warm recovery + concurrent session-check edge cases remain possible; keep scope to cold-start destroy of NewPassword unless evidence expands.

### Open Questions

1. Confirm suppress set is exactly `LinkReceived` + `PasswordUpdated` (mirror invalidation), not `hasPendingRecoveryToken()` while Idle.
2. After successful password update with old GoTrue session still valid, is “stay off Main until explicit Login” already the product rule? (NewPassword success UI suggests yes.)
3. Should a thin Compose/source-lock assert cold-start keys/guard mention `recoveryState`, or is `ColdStartNavigationTest` enough for verify?

### Ready for Proposal

Yes — recommend `sdd-propose` for Approach 1 with Strict TDD on `ColdStartNavigation` + `OptoAppNavigation` wiring + spec deltas under `android-auth` / `android-password-recovery`.
