# Proposal: Fix Auth Deep-Link Session (Part 2b)

## Intent

Close Judgment Day ROUND 2 preexisting deep-link/session defects (JD2-C1, C4, C5): cold-start OAuth races `checkExistingSession` GLOBAL sign-out; null `intent.data` is treated as OAuth success and can wipe Room; `AuthDelegate` logs full deep-link URIs with tokens. Part1/2a recovery UI stays closed.

## Scope

### In Scope
- **C4:** Serialize cold-start — await OAuth/recovery deep-link handler **before** `checkExistingSession` via awaitable AuthViewModel APIs; do not change `SignOutScope.GLOBAL` semantics
- **C5:** Null `intent.data` → non-null error string (not success); `onNewIntent` requires `ACTION_VIEW` + non-null data before handlers; never `resolvePostLogin`/wipe on null data
- **C1:** Remove (prefer) or never log full deep-link URIs/tokens; no secret-bearing `Log.d` of Uri
- Strict TDD: RED null-data / no `resolvePostLogin` → serialize join → GREEN Delegate/VM/MainActivity; keep Part1/2a recovery suite green

### Out of Scope
- AndroidManifest `singleTask` / `singleTop` (deferred)
- JD2-C6–C8; CreatePin await; Membership Empty; ON_RESUME Loading→Idle; `prepareOpticaSelection`; Part2a Login reset
- Supabase schema or RLS

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `android-auth`: Add deep-link cold-start serialization, OAuth null-data fail-closed, and no secret-bearing deep-link URI logs. Prefer primary delta here; touch `android-password-recovery` only if recovery cold-start await needs a one-line note (no UI/retryability reopen).

## Approach

Locked **Approach 1** (exploration):

1. Expose joinable suspend/`Job` VM APIs; `MainActivity.onCreate` awaits VIEW deep-link handler then `checkExistingSession`; non-VIEW → session check only.
2. Delegate OAuth: `data == null` → error string (mirror recovery); VM Error path skips `resolvePostLogin`.
3. `onNewIntent`: gate on `ACTION_VIEW` + non-null data; else no-op.
4. Delete secret-bearing deep-link `Log.d` lines (prefer remove over DEBUG+redact).
5. Leave GLOBAL sign-out and PIN/logout untouched.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `viewmodel/auth/AuthDelegate.kt` | Modified | C5 null-data; C1 remove Uri logs |
| `viewmodel/AuthViewModel.kt` | Modified | Awaitable deep-link APIs; null-data → Error |
| `MainActivity.kt` | Modified | Serialize onCreate; guard onNewIntent |
| `AndroidManifest.xml` | Unchanged | `singleTask` deferred |
| AuthDelegate/VM unit tests | New/Modified | Null-data, no wipe, join contract |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Await API breaks Loading/Cancellation UX | Med | Preserve existing Loading states; lifecycleScope not main-thread block |
| Post-await `logout()` clears `pendingRecoveryToken` | Low | Keep today's invariant (logout does not clear token) |
| Reopen Part2a Login entry reset | Med | Do not touch Login mount reset |
| Scope creep to C6–C8 / singleTask | Med | Hard out-of-scope list |

## Rollback Plan

Revert this change’s Android/test commits. No Supabase schema or RLS. Manifest launchMode unchanged.

## Dependencies

- Part1/2a recovery closed; confirmed Approach 1 handoff
- `strict_tdd: true`

## Success Criteria

- [ ] Cold-start VIEW deep link completes before `checkExistingSession` (no concurrent GLOBAL wipe of in-flight OAuth)
- [ ] Null OAuth `intent.data` → Error; no `resolvePostLogin` / Room wipe
- [ ] `onNewIntent` without ACTION_VIEW+data is no-op
- [ ] No full deep-link URI/token in logs
- [ ] `SignOutScope.GLOBAL` unchanged; Part1/2a recovery tests green; `testDebugUnitTest` green
