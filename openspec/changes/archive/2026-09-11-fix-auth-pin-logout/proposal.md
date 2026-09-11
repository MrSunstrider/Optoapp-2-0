# Proposal: Fix Auth PIN Logout (JD2-C8)

## Intent

Close Judgment Day ROUND 2 **JD2-C8**: logout leaves device PIN secret (`user_pin`) and `pref_pin_has_been_set`, so the next account can skip PIN incorrectly or unlock with the prior account’s PIN. Also close the CreatePin fire-and-forget race (navigate Main before persist succeeds).

## Scope

### In Scope

- Wipe PIN secret **and** `pref_pin_has_been_set` on logout (`SecurityManager` clear API + `SessionManager.clearSession` / AuthDelegate call site)
- Awaitable `AuthViewModel.createPin`; `CreatePinScreen` navigates Main only after successful persist
- Spec delta on `android-auth`; strict TDD (RED → GREEN)

### Out of Scope

- C6/C7 membership/JWT, deeplink, recovery, ON_RESUME
- Mandatory PIN / C3-Default-PIN (PIN stays optional)
- Per-uid PIN keying (Approach 2), `SignOutScope.GLOBAL` / `launchMode` changes

## Capabilities

### New Capabilities

- None

### Modified Capabilities

- `android-auth`: logout MUST wipe PIN secret + has-been-set; Create PIN MUST await successful persist before leave; optional-PIN defaults unchanged

## Approach

Locked **Approach 1** from exploration:

1. Add `ISecurityManager.clearStoredPin()` (remove secret, set `pinHasBeenSet=false`, clear in-memory flow).
2. Call wipe from `AuthDelegate.logout` with Room wipe (regardless of remote signOut errors except cancellation).
3. `SessionManager.clearSession` also clears `pref_pin_has_been_set` (belt-and-suspenders for dual readers).
4. Make `createPin` suspend/awaitable; CreatePin navigates only on success; stay on screen on failure.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/SecurityManager.kt` (+ `ISecurityManager`) | Modified | `clearStoredPin` API |
| `data/SessionManager.kt` | Modified | clear `pref_pin_has_been_set` in `clearSession` |
| `viewmodel/auth/AuthDelegate.kt` | Modified | call PIN wipe on logout |
| `viewmodel/AuthViewModel.kt` | Modified | awaitable `createPin` |
| `ui/screens/CreatePinScreen.kt` | Modified | navigate only after success |
| Unit tests / fakes | Modified | wipe + await contracts |
| `openspec/specs/android-auth` | Modified | delta requirements |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Dual readers of `pinHasBeenSet` lie after partial clear | Med | Wipe SecurityManager **and** clear DataStore key in `clearSession` |
| Interface change breaks many fakes | Med | Update all `ISecurityManager` fakes in same change |
| Invalid PIN after confirm navigates anyway | Low | Return success/failure; navigate only on success |

## Rollback Plan

Revert the change branch / PR. No Supabase schema or RLS changes. Device PIN state is local prefs only; no migration to reverse.

## Dependencies

- None (Android-only). **Supabase schema/RLS: not affected.**

## Success Criteria

- [ ] After logout, PIN secret empty and `pinHasBeenSet=false` for next login
- [ ] Next account cannot unlock with prior PIN or skip via stale has-been-set
- [ ] CreatePin reaches Main only after successful persist; failure stays on CreatePin
- [ ] Optional PIN product default unchanged; unit tests pass (`testDebugUnitTest`)
