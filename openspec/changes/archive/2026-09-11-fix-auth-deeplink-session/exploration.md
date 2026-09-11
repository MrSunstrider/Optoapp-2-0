## Exploration: fix-auth-deeplink-session (Part 2b)

### Scope

Part 2b — Judgment Day ROUND 2 **preexisting** deep-link / session findings remaining after Part1 (`fix-auth-password-recovery`) + Part2a (`fix-auth-recovery-regressions`):

| ID | Severity | Finding |
|----|----------|---------|
| JD2-C1 | CRITICAL/HIGH | `AuthDelegate` `Log.d` logs full OAuth/recovery deep-link URIs including `access_token` in fragment |
| JD2-C4 | CRITICAL/HIGH | Cold-start `ACTION_VIEW` launches `handleAuthDeepLinkIntent` / `handleRecoveryDeepLink` **concurrently** with `checkExistingSession`; null session → `logout()` `SignOutScope.GLOBAL` can kill in-flight OAuth |
| JD2-C5 | CRITICAL | `handleAuthDeepLinkIntent` treats `intent?.data == null` as success (`return null`) → VM `resolvePostLogin` → `wipeLocalAccountData`; `onNewIntent` else-branch has no `ACTION_VIEW` / data guard |

**Out of scope:** JD2-C6 rol admin, C7 NetworkRetry, C8 PIN logout; suspects CreatePin await / Membership Empty / ON_RESUME Loading→Idle / prepareOpticaSelection; password-recovery UI (closed in Part1/2a).

**Strict TDD:** yes (`openspec/config.yaml`).

### Current State

#### Cold-start concurrency (C4)

`MainActivity.onCreate` (no `launchMode` → default **standard**):

```kotlin
if (intent?.action == Intent.ACTION_VIEW) {
    if (isRecoveryDeepLink(intent)) authViewModel.handleRecoveryDeepLink(intent)
    else authViewModel.handleAuthDeepLinkIntent(intent)
}
authViewModel.checkExistingSession()  // always, same frame
```

Both VM entry points are `viewModelScope.launch { ... }` — fire-and-forget, no join. Race:

1. `checkExistingSession` → `AuthDelegate.checkExistingSession` sees `currentSessionOrNull() == null`
2. Calls `logout()` → `signOut(SignOutScope.GLOBAL)` + `wipeLocalAccountData()` + `clearSession()`
3. Concurrently `handleAuthDeepLinkIntent` runs `supabase.handleDeeplinks` + polls up to ~6s for session
4. GLOBAL sign-out can invalidate the OAuth session mid-flight; user lands Error / stuck Loading / wiped Room

Recovery path is less sensitive (raw `pendingRecoveryToken` in memory, no `handleDeeplinks`), but still races Room wipe and `isAuthChecked` / Login nav. Part2a already guards Login↔recovery UI; this change must not reopen C2/C3.

#### Null data as OAuth success (C5)

`AuthDelegate.handleAuthDeepLinkIntent`:

```kotlin
val deepLink = intent?.data ?: return null  // null = "success"
```

Contrast recovery: `intent?.data ?: return "Enlace inválido"` (fail-closed).

`AuthViewModel.handleAuthDeepLinkIntent`:

```kotlin
val error = authDelegate.handleAuthDeepLinkIntent(intent)
if (error != null) { Error; return }
applyPostLogin(authDelegate.resolvePostLogin())  // null → Success path
```

`resolvePostLogin` always calls `resetLocalStoreForNewAuthSession()` → `repository.wipeLocalAccountData()` before membership fetch. Null/`empty` intent therefore can wipe Room without a real session.

`MainActivity.onNewIntent`:

```kotlin
if (isRecoveryDeepLink(intent)) handleRecovery...
else handleAuthDeepLinkIntent(intent)  // no ACTION_VIEW, no data check
```

With default `standard` launchMode, warm deep links usually create a new Activity (`onCreate`), so `onNewIntent` is rare today — but the else-branch is still a latent wipe vector (and becomes hot if `singleTask`/`singleTop` is added).

#### Secret logging (C1)

Unconditional (not even `BuildConfig.DEBUG`):

- OAuth: `Log.d(TAG, "Recibido deeplink OAuth: $deepLink")` — Uri `toString()` includes `#access_token=...&refresh_token=...`
- Recovery: `Log.d(TAG, "Recibido deeplink recovery: $deepLink")` — same fragment leak

Nearby code already redacts secrets for sync errors (`SyncErrorSanitizer`) and avoids Bearer logging in `updatePassword`. Auth deep-link logs are the outlier. `BuildConfig.DEBUG`-only gate is insufficient for shared debug devices / bugreport logcat — prefer remove or always-redact (scheme/host/path only, strip fragment/query).

#### Spec / manifest gaps

- `openspec/specs/android-auth/spec.md` covers cold-start route restore, Google cancel, PIN — **no** deep-link serialization, null-data fail-closed, or log redaction requirements.
- `AndroidManifest.xml`: VIEW intent-filter `optoapp` scheme/host via build placeholders; **no** `android:launchMode`.
- AGENTS.md Auth: `site_url = "optoapp://auth"`; `enableLifecycleCallbacks = false` (P0-T4) — session clearing in background is separate from this cold-start race.

#### Existing tests

- Recovery deep-link + token taxonomy: `AuthDelegateRecoveryTest`, `AuthViewModelTest` (Part1/2a) — keep green.
- No behavioral tests for `handleAuthDeepLinkIntent` null-data, log redaction, or cold-start serialization.
- `checkExistingSession` VM tests only assert no double `logout()` when Delegate returns false.

### Affected Areas

- `optoapp/.../viewmodel/auth/AuthDelegate.kt` — C1 logs; C5 null-data return; C4 `checkExistingSession`/`logout` coupling (read carefully; prefer serialize callers over changing GLOBAL semantics unless needed)
- `optoapp/.../viewmodel/AuthViewModel.kt` — awaitable cold-start APIs / serialization; ensure null-data → Error not `resolvePostLogin`
- `optoapp/.../MainActivity.kt` — onCreate order; onNewIntent ACTION_VIEW+data guards; optional await via `lifecycleScope`
- `optoapp/src/main/AndroidManifest.xml` — optional `singleTask`/`singleTop` (Approach C only)
- `optoapp/.../util/SyncErrorSanitizer.kt` or small Uri redactor — reuse pattern for safe debug logs if any remain
- Specs: delta under `android-auth` (and/or thin deep-link scenarios); do not reopen `android-password-recovery` UI contracts
- Tests: new AuthDelegate OAuth deep-link null-data; VM does not call `resolvePostLogin` on null data; cold-start serialize helper or VM join contract; log redactor unit test if extracted

### Approaches

1. **Serialize deep-link then session check + fail-closed null data + redact/remove logs (recommended)**
   - **C4:** In `onCreate`, if `ACTION_VIEW` with usable data: **await** recovery or OAuth handler to completion, **then** run `checkExistingSession`. If not a VIEW deep link: `checkExistingSession` only (today). Prefer exposing joinable suspend/`Job` APIs on VM (or a tiny `ColdStartAuthGate` helper) so MainActivity can sequence without racing two launches.
   - **C5:** `handleAuthDeepLinkIntent`: `data == null` → non-null error string (mirror recovery). `onNewIntent`: require `ACTION_VIEW` and non-null `data` (and recovery vs OAuth branch) before calling handlers; otherwise no-op.
   - **C1:** Delete both full-URI `Log.d` lines, or DEBUG-only log of redacted Uri (no fragment/query). Never log tokens in release or debug.
   - Pros: Minimal surface; fixes all three JD IDs; preserves recovery Part1/2a; testable without instrumented Activity; no manifest lifecycle churn.
   - Cons: Need awaitable VM APIs; must not block UI thread (use `lifecycleScope` + existing Loading states).
   - Effort: Low–Medium

2. **In-Delegate session mutex / "deepLinkInFlight" skip-logout**
   - Flag set around deep-link handling; `checkExistingSession` skips `logout()` while in flight or if recovery token pending.
   - Pros: Callers stay fire-and-forget.
   - Cons: Easy to leak flag; still need C5/C1 fixes separately; harder to reason about overlapping warm intents; weaker than true serialization.
   - Effort: Medium

3. **`singleTask` + warm `onNewIntent` only + fail-closed + redact**
   - Manifest `launchMode="singleTask"` so OAuth returns to existing Activity via `onNewIntent`; tighten guards; still serialize relative to any session check.
   - Pros: One Activity instance; cleaner OAuth return UX.
   - Cons: Broader lifecycle/nav side effects (task affinity, back stack); still need C4 sequencing on cold VIEW start; higher regression risk; overkill for JD close.
   - Effort: Medium–High — **defer** unless product wants single-instance after this change.

### Recommendation

Ship **Approach 1** only for Part 2b:

1. Fail-closed null/missing `intent.data` in OAuth deep-link handler (C5).
2. Guard `onNewIntent` with `ACTION_VIEW` + non-null data (C5).
3. Serialize cold-start: await deep-link handler before `checkExistingSession` when VIEW intent present (C4).
4. Remove or always-redact deep-link Uri logs — never emit `access_token` / fragment (C1).
5. Leave `SignOutScope.GLOBAL` and PIN/logout (C8) untouched.
6. Optional `singleTask` out of scope unless a follow-up change.

Strict TDD order: RED null-data + no `resolvePostLogin`; RED redactor/no-secret log helper if extracted; RED serialize/join contract; then GREEN MainActivity/VM/Delegate; keep Part1/2a recovery suite green.

### Risks

- **Await API shape** — converting fire-and-forget to joinable must preserve Loading/`LinkReceived` UX and CancellationException rules.
- **Recovery + checkExistingSession after await** — null Supabase session still triggers `logout()` wipe; usually OK for unauthenticated recovery, but confirm it does not clear `pendingRecoveryToken` (today `logout()` does not clear it — keep that invariant).
- **Part2a Login entry** — do not reintroduce entry `resetRecoveryState`; deep-link serialize must coexist with no Login-mount reset.
- **`standard` launchMode** — multiple MainActivity instances possible; serialize per instance; do not assume `onNewIntent` alone.
- **Over-scoping to C6–C8** — keep this change deep-link/session only.
- **Test seam for MainActivity** — prefer extractable gate/helper for unit tests over Robolectric Activity tests.

### Ready for Proposal

Yes — orchestrator should run **sdd-propose** for `fix-auth-deeplink-session` with Approach 1 (serialize + fail-closed null data + redact/remove logs; optional singleTask deferred).
