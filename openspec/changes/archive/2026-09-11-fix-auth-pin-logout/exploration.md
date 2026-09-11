## Exploration: fix-auth-pin-logout

### Scope

Judgment Day ROUND 2 **JD2-C8 CRITICAL** only (plus related CreatePin race called out as in-scope suspect):

| ID | Severity | Finding |
|----|----------|---------|
| JD2-C8 | CRITICAL | `logout` / `clearSession` sets `isPinRequired=false` but leaves `SecurityManager` `user_pin` and DataStore `pref_pin_has_been_set` — next account skips PIN or unlocks with previous account's PIN |
| Suspect | HIGH | `CreatePinScreen` fire-and-forget `createPin` then navigates `Main` without awaiting persistence |

**Out of scope**: C6, C7, Membership Empty, `prepareOpticaSelection`, ON_RESUME, deeplink (done in prior changes).

**Constraints**: `strict_tdd: true`; PIN remains optional (do not revive C3-Default-PIN / mandatory PIN); hybrid artifact store.

---

### Current State

PIN is a **device-local** second factor (EncryptedSharedPreferences + DataStore), not keyed by auth uid.

**Storage split**

| Concern | Store | Key / file |
|---------|-------|------------|
| PIN secret | `SecurityManager` EncryptedSharedPreferences | `secure_security_prefs` → `user_pin` |
| `pinHasBeenSet` flag | Shared DataStore `settings` | `pref_pin_has_been_set` (written by `SecurityManager.savePin`) |
| `isPinRequired` | Same DataStore `settings` | `pref_is_pin_required` (via `SessionManager`) |
| Session identity | `SessionManager` EncryptedSharedPreferences | `secure_session_prefs` (optica/email/rol) |

`PinDelegate.pinHasBeenSet` reads **`securityManager.pinHasBeenSet`**. `SettingsViewModel.pinHasBeenSet` reads **`sessionManager.pinHasBeenSet`** — same DataStore key today, but two readers over one flag.

**Logout today** (`AuthDelegate.logout`):

1. `supabase.auth.signOut(SignOutScope.GLOBAL)` (errors ignored except `CancellationException`)
2. `resetLocalStoreForNewAuthSession()` → Room wipe (`wipeLocalAccountData`) — **no PIN wipe**
3. `sessionManager.clearSession()` → clears `secure_session_prefs`; sets `IS_LOGGED_IN=false`, `IS_PIN_REQUIRED=false`; **does not remove `pref_pin_has_been_set`**; **does not touch `secure_security_prefs` / `user_pin`**

`ISecurityManager` exposes only `savePin` — **no clear/wipe API**.

**Cross-account exploit path (confirmed)**

1. Account A enables PIN, `savePin("183729")` → secret + `pinHasBeenSet=true`
2. Logout → `isPinRequired=false`, Room wiped, **PIN secret + flag remain**
3. Account B logs in → `PostLoginNavigation`: `isPinRequired=false` → **Main (skips PIN)** while prior secret remains
4. Account B toggles PIN required in Settings (`setPinRequired(true)` only — does not create a new PIN) → routing sees `isPinRequired && pinHasBeenSet` → **Pin unlock accepts A's PIN**

**CreatePin race (confirmed)**

```kotlin
// AuthViewModel
fun createPin(pin: String) = viewModelScope.launch { pinDelegate.createPin(pin) }

// CreatePinScreen (confirm step)
viewModel.createPin(firstPin)          // fire-and-forget Job
navController.navigate(Route.Main...)  // no await / join
```

Process death or early cold-start collectors can observe `pinHasBeenSet=false` after the user believes PIN was created; or navigate succeeds while persist fails silently (`createPin` returns early on invalid PIN with no UI error path after confirm).

**Specs today** (`android-auth`): Empty PIN invalid; Create PIN only when required ∧ unset; PIN default off. **No requirement** that logout wipes PIN or that Create PIN awaits persistence. `android-auth-onboarding` only references pin-or-main routing.

**Existing tests**: `PinDelegateTest` / characterization cover create/validate/toggle; `SessionManagerTest` fake `clearSession` does not assert PIN flags; `AuthDelegateTest` verifies Room wipe on logout path, **not** PIN wipe; no CreatePin await contract test. No Robolectric — EncryptedSharedPreferences needs fakes or `androidTest`.

---

### Affected Areas

- `optoapp/.../data/SecurityManager.kt` (+ `ISecurityManager`) — add wipe/clear of `user_pin` + `pref_pin_has_been_set` + `_pinFlow`
- `optoapp/.../data/SessionManager.kt` — `clearSession` should also remove `pref_pin_has_been_set` (same key; belt-and-suspenders with SecurityManager wipe)
- `optoapp/.../viewmodel/auth/AuthDelegate.kt` — call PIN wipe on `logout` (same place Room is wiped for cross-account isolation)
- `optoapp/.../viewmodel/auth/PinDelegate.kt` — optional `clearStoredPin()` wrapper; reset input/cooldown on logout if wired
- `optoapp/.../viewmodel/AuthViewModel.kt` — make `createPin` awaitable (`suspend` or joinable Job)
- `optoapp/.../ui/screens/CreatePinScreen.kt` — await create success before `navigate(Main)`
- `openspec/specs/android-auth/spec.md` — delta: logout MUST wipe PIN secret + has-been-set; Create PIN MUST await persist before leave
- Tests: `AuthDelegateTest` / new PIN-logout tests; `PinDelegateTest` fake wipe; `AuthViewModel` createPin await; instrumented `SecurityManager` clear if needed
- Fakes implementing `ISecurityManager` / `ISessionManager` across unit tests — add `clearStoredPin` / clear flag behavior

**Not affected (out of scope)**: deeplink handlers, recovery token taxonomy, membership Empty/Ok flags, `PostLoginNavigation` dest matrix (behavior correct once flags are wiped), ON_RESUME.

---

### Approaches

1. **Wipe PIN on logout + await createPin (recommended)** — Add `ISecurityManager.clearStoredPin()` (remove secret, set `pinHasBeenSet=false`, clear in-memory flow). Call from `AuthDelegate.logout` alongside Room wipe. Also remove `PIN_HAS_BEEN_SET` in `SessionManager.clearSession`. Change `AuthViewModel.createPin` to `suspend fun` (or return `Job` and `join`) and await in `CreatePinScreen` before navigate; only navigate on successful save.
   - Pros: Matches Room cross-account isolation pattern; closes both skip-PIN and unlock-with-prior-PIN; small, reviewable; TDD-friendly via fakes/MockK
   - Cons: Device PIN is not portable across accounts by design (acceptable — PIN is local unlock, not cloud credential); Settings readers must see cleared flag
   - Effort: Low–Medium

2. **Key PIN by auth uid** — Store `user_pin_{uid}` / per-user flags; logout leaves other users' PINs.
   - Pros: Multi-account device convenience
   - Cons: Larger schema migration of prefs; Settings/PinDelegate/PostLogin must resolve current uid; overkill for single-active-session app; higher effort vs JD C8
   - Effort: High

3. **Clear flag only in `clearSession`, leave secret** — Remove `pref_pin_has_been_set` without wiping `user_pin`.
   - Pros: Tiny change
   - Cons: Secret remains; migration/`getSecurePin` / future paths could re-elevate flag; incomplete remediation of CRITICAL
   - Effort: Low (rejected)

4. **UI-only CreatePin await without logout wipe** — Fixes race only.
   - Pros: Tiny
   - Cons: Does not address JD2-C8 cross-account CRITICAL
   - Effort: Low (insufficient alone)

---

### Recommendation

**Approach 1.** Wipe device PIN state on every logout (secret + `pinHasBeenSet`), keep `isPinRequired=false` reset already in `clearSession`, and await `createPin` before leaving `CreatePinScreen`.

**Strict TDD sketch**

| Step | RED | GREEN |
|------|-----|-------|
| 1 | `ISecurityManager` / Fake: after `clearStoredPin`, `userPin` empty and `pinHasBeenSet=false` | Implement `SecurityManager.clearStoredPin` |
| 2 | `AuthDelegate.logout` verifies `securityManager.clearStoredPin()` (or equivalent) called even when signOut throws IOException | Wire call in `logout` after/before Room wipe; order: prefer wipe PIN with session clear regardless of remote signOut |
| 3 | `SessionManager` clear contract: `clearSession` leaves `pinHasBeenSet=false` (fake or characterization) | `prefs.remove(PIN_HAS_BEEN_SET)` in `clearSession` |
| 4 | `AuthViewModel.createPin` is suspend / awaitable; test that invalid PIN does not claim success | Change signature; CreatePinScreen awaits then navigates only on success |
| 5 | Optional instrumented: savePin → clearStoredPin → getStoredPin empty | `SecurityManagerInstrumentedTest` |

Do **not** change `SignOutScope.GLOBAL`, Activity `launchMode`, or optional-PIN product default.

Spec delta target: `android-auth` — new requirements for logout PIN wipe and Create PIN persistence-before-navigate. Onboarding spec unchanged except it continues to rely on correct pin-or-main inputs.

---

### Risks

- Dual readers (`SessionManager.pinHasBeenSet` vs `SecurityManager.pinHasBeenSet`) — wipe must clear the shared DataStore key or Settings UI will lie
- Fakes/MockK across many tests must implement new `ISecurityManager` method
- `createPin` signature change may break call sites (currently AuthViewModel + CreatePinScreen)
- Silent no-op on invalid PIN after confirm — await path should surface failure and stay on CreatePin
- `SettingsViewModel.isPinRequired` `stateIn` default `true` vs SessionManager default `false` is a separate inconsistency (out of scope unless it blocks wipe verification)
- Brute-force cooldown in `PinDelegate` survives logout unless explicitly reset (minor; optional follow-up)

---

### Ready for Proposal

Yes. Next: **sdd-propose** for `fix-auth-pin-logout` locking Approach 1 (wipe PIN on logout + await createPin), strict TDD, spec delta on `android-auth` only, out-of-scope list intact.
