# Proposal: Fix Auth Recovery Regressions (Part 2a)

## Intent

Part 1 fixed Delegate token taxonomy and dispose races, but UI/VM wiring still burns recoverable sessions: NewPassword treats every `Error` as terminal (JD2-C2), and Login entry always calls `resetRecoveryState()` racing cold-start deep links (JD2-C3). Close those two regressions so retryable failures keep the form and Login mount never clears recovery.

## Scope

### In Scope
- Extend `RecoveryState.Error` with `isRetryable` from `hasPendingRecoveryToken()` after failed `updatePassword` (blank-token Errors → `false`)
- NewPassword: retryable → keep form + retry (no reset); terminal → "Solicitar uno nuevo" → `resetRecoveryState` + navigate Recovery
- Remove `LoginScreen` entry `LaunchedEffect` `resetRecoveryState()`; rely on explicit-exit handlers only
- Strict TDD: ViewModel contracts first, then screens

### Out of Scope
- AuthDelegate token taxonomy rewrite (Part 1 stays)
- JD2-C1 Log.d, C4 cold-start `checkExistingSession` race, C5 null `intent.data`, C6 membership/rol, C7 NetworkRetry, C8 PIN; suspects `prepareOpticaSelection` / ON_RESUME / CreatePin
- Supabase schema or RLS

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `android-password-recovery`: Revive/extend Part 1 delta — Error must distinguish retryable vs terminal for NewPassword UI; explicit-exit reset MUST exclude Login entry mount (close JD2-C2/C3 without reopening dispose, timeouts, or popUpTo).

## Approach

1. VM: after failed `updatePassword`, set `Error(message, isRetryable = authDelegate.hasPendingRecoveryToken())`.
2. NewPassword branches on `isRetryable`; terminal path unchanged.
3. Delete Login entry reset; keep Recovery/NewPassword explicit-exit resets.
4. TDD RED→GREEN on `AuthViewModelTest` then screen wiring. No Delegate taxonomy changes.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `viewmodel/AuthViewModel.kt` | Modified | `Error(isRetryable)`; map from token presence |
| `ui/screens/NewPasswordScreen.kt` | Modified | Split retryable vs terminal Error UI |
| `ui/screens/LoginScreen.kt` | Modified | Remove entry `resetRecoveryState()` |
| `viewmodel/auth/AuthDelegate.kt` | Unchanged | Taxonomy read-only; expose via existing `hasPendingRecoveryToken` |
| `AuthViewModelTest` (+ screen tests) | Modified/New | Retryable/terminal + no Login-entry reset |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Stale EmailSent after removing Login reset | Med | Accept residual; Recovery Back / "Intentar de nuevo" still clear |
| Blank-token Error marked retryable | Low | Force `isRetryable=false` when no pending token |
| Scope creep into MainActivity C4/C5 | Med | Hard out-of-scope; touch MainActivity only if nav contracts demand it |

## Rollback Plan

Revert this change’s Android UI/VM/test commits. No Supabase schema or RLS. Part 1 Delegate taxonomy untouched.

## Dependencies

- Part 1 `fix-auth-password-recovery` / `android-password-recovery` delta (token keep/clear + explicit-exit policy)
- Confirmed pre-proposal decisions (C2 Error+isRetryable, C3 remove Login entry reset)

## Success Criteria

- [ ] Retryable `updatePassword` failure → `Error(isRetryable=true)`, form usable, retry without reset
- [ ] Terminal/burned-token Error → CTA resets + navigates Recovery; no auto-nav
- [ ] Login compose/entry does not call `resetRecoveryState()`
- [ ] Explicit exits still clear Idle + token
- [ ] Failing-then-passing ViewModel (then screen) tests; `testDebugUnitTest` green
