# Design: Fix Auth Deep-Link Session (Part 2b)

## Technical Approach

Serialize cold-start OAuth/recovery handling before `checkExistingSession` (JD2-C4), fail-closed null OAuth `intent.data` (JD2-C5), and delete secret-bearing deep-link `Log.d` (JD2-C1). Maps to locked Approach 1 and `android-auth` delta. No `SignOutScope.GLOBAL` change, no `launchMode`, no C6–C8, no Part2a Login reopen.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice |
|----------|---------|----------|--------|
| Await API | Join existing `Job` returns vs suspend wrappers vs single `coldStartDeepLink` | Jobs already return from `viewModelScope.launch` but body is not re-entrant; single gate hides recovery/OAuth; wrappers keep warm path + testable suspend | **Suspend bodies + thin Job wrappers** |
| Cold-start host | Block `onCreate` vs `lifecycleScope.launch` sequence | Block freezes UI; scope keeps Loading UX and `setContent` immediate | **`lifecycleScope.launch { await…; checkExistingSession() }`** |
| Branching | Move `isRecoveryDeepLink` into VM vs keep MainActivity | VM gate needs Intent helpers; Activity already owns filter | **Keep MainActivity branch**; call matching await |
| Null OAuth data | Error string vs throw vs keep `null` success | Recovery already returns `"Enlace inválido"` | **Same non-null error string**; VM Error path skips `resolvePostLogin` |
| `onNewIntent` | ACTION_VIEW+data gate vs recovery-only gate | Null data today misroutes to OAuth success | **Require `ACTION_VIEW` + non-null `data`**; else no-op |
| Secret logs | Remove vs DEBUG+redact | Redact still risks leaks; updatePassword already avoids Bearer logs | **Delete both Uri `Log.d` lines** |
| Post-await logout | Skip logout when recovery token pending vs serialize only | Skip changes GLOBAL semantics (out of scope) | **Serialize only**; keep `logout()` not clearing `pendingRecoveryToken` |

## Data Flow

```
onCreate
  ├─ setContent (immediate)
  └─ lifecycleScope
       ├─ if ACTION_VIEW
       │    ├─ recovery → awaitHandleRecoveryDeepLink → Loading → LinkReceived|Error
       │    └─ else    → awaitHandleAuthDeepLinkIntent → Loading → Success|Error
       └─ then checkExistingSession()  // never concurrent with deep-link Job
```

```mermaid
sequenceDiagram
  participant MA as MainActivity
  participant VM as AuthViewModel
  participant D as AuthDelegate
  MA->>VM: awaitHandle*(intent)
  VM->>D: handle*(intent)
  alt null data OAuth
    D-->>VM: "Enlace inválido"
    VM-->>VM: AuthState.Error (no resolvePostLogin)
  else recovery OK
    D-->>VM: null (token kept in memory)
    VM-->>VM: LinkReceived
  end
  MA->>VM: checkExistingSession()
  Note over D: null session → logout GLOBAL+wipe; pendingRecoveryToken unchanged
```

**Ordering invariant:** After recovery await, null Supabase session still triggers `logout()` wipe — acceptable. `logout()` must **not** call `clearPendingRecoveryToken` / `resetRecoveryState` (today’s invariant; do not regress).

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/auth/AuthDelegate.kt` | Modify | OAuth null data → `"Enlace inválido"`; remove OAuth+recovery Uri `Log.d` |
| `optoapp/.../viewmodel/AuthViewModel.kt` | Modify | Extract suspend `awaitHandleAuthDeepLinkIntent` / `awaitHandleRecoveryDeepLink`; Job wrappers call them; preserve Loading/LinkReceived/CancellationException |
| `optoapp/.../MainActivity.kt` | Modify | Cold-start: await VIEW path then `checkExistingSession`; non-VIEW: session only; `onNewIntent`: ACTION_VIEW+non-null data else no-op |
| `AndroidManifest.xml` | Unchanged | `singleTask` deferred |
| `.../viewmodel/auth/AuthDelegateRecoveryTest.kt` or OAuth deep-link test | Modify/Create | Null-data fail-closed; assert no secret `Log.d` |
| `.../viewmodel/AuthViewModelTest.kt` | Modify | Null data → Error, never `resolvePostLogin`; await-then-session order if testable via MockK verify order |

## Interfaces / Contracts

```kotlin
// AuthDelegate — OAuth null-data fail-closed (mirror recovery)
suspend fun handleAuthDeepLinkIntent(intent: Intent?): String? {
    val deepLink = intent?.data ?: return "Enlace inválido"
    // no Log.d of deepLink
    ...
}

// AuthViewModel — suspend for cold-start join; Job for warm onNewIntent
suspend fun awaitHandleAuthDeepLinkIntent(intent: Intent?) { /* existing body */ }
suspend fun awaitHandleRecoveryDeepLink(intent: Intent?) { /* existing body */ }
fun handleAuthDeepLinkIntent(intent: Intent?) = viewModelScope.launch {
    awaitHandleAuthDeepLinkIntent(intent)
}
fun handleRecoveryDeepLink(intent: Intent?) = viewModelScope.launch {
    awaitHandleRecoveryDeepLink(intent)
}

// MainActivity.onCreate (sketch)
lifecycleScope.launch {
    if (intent?.action == Intent.ACTION_VIEW) {
        if (isRecoveryDeepLink(intent)) authViewModel.awaitHandleRecoveryDeepLink(intent)
        else authViewModel.awaitHandleAuthDeepLinkIntent(intent)
    }
    authViewModel.checkExistingSession()
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit RED→GREEN | OAuth null `data` → non-null error; no wipe/`resolvePostLogin` | `AuthDelegate` + `AuthViewModelTest` MockK |
| Unit | No `Log.d` with full Uri / `access_token` | MockK `verify(exactly=0)` on secret-bearing messages (source assert OK) |
| Unit | Await completes before `checkExistingSession` starts | VM/bootstrap coVerify order; no Robolectric |
| Regression | Part1/2a recovery suite stays green | Existing `AuthDelegateRecoveryTest` / VM recovery tests |

Strict TDD: RED null-data + no resolvePostLogin → RED no-secret logs → RED await order → GREEN Delegate/VM/MainActivity.

## Threat Matrix

N/A — Android Intent deep-link auth only; no shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary from the design threat matrix.

## Migration / Rollout

No migration required. Client-only. Rollback: revert this change’s Android/test commits. Manifest unchanged.

## Open Questions

- None blocking. Residual: default `standard` launchMode still allows multiple Activity instances — serialize per instance only (accepted; `singleTask` deferred).
