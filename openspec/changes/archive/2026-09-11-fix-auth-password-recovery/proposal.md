# Proposal: Fix Auth Password Recovery (Part 1)

## Intent

Warm recovery deep links fail because `DisposableEffect` dispose resets `RecoveryState` before `NewPassword` can use `LinkReceived` (JD-C1). Failed password updates clear `pendingRecoveryToken` before HTTP succeeds, so retries cannot work (JD-S4). The recovery PUT has no timeouts, and the Error CTA does not return the user to Recovery. Fix Part 1 JD findings only so users can complete password reset after email link.

## Scope

### In Scope
- Remove dispose→`resetRecoveryState` from `RecoveryScreen` and `NewPasswordScreen`; reset only on explicit exits
- Optional `popUpTo(Recovery)` when navigating to `NewPassword`
- Keep `pendingRecoveryToken` across retryable PUT failures; clear on HTTP 2xx or terminal 401/403; `resetRecoveryState()` clears delegate token
- NewPassword Error CTA navigates to Recovery (button, not auto-navigate)
- Back from NewPassword → Login + clear recovery state
- `HttpURLConnection` connectTimeout=10_000, readTimeout=15_000 on recovery PUT
- Strict TDD: AuthDelegate / AuthViewModel / RecoveryState unit tests first

### Out of Scope
- Parts 2–4 (S2/S3/null intent, S1/CreatePin, MembershipFetch/rol/NetworkRetryHelper)
- Compose dispose instrumented tests as primary coverage
- Supabase schema, RLS, email templates, or Ktor client rewrite

## Capabilities

### New Capabilities
- `android-password-recovery`: Revive recovery lifecycle not present in main specs (C4 archive). Cover dispose/reset policy, token lifetime, PUT timeouts, Error CTA → Recovery, Back → Login + clear.

### Modified Capabilities
- None — live `android-auth` has no recovery requirements today; Part 1 does not change Login cold-start/Google/PIN.

## Approach

Explicit-exit reset + optional `popUpTo(Recovery)` + clear-token-on-success/terminal-auth + 10s/15s timeouts + Delegate/ViewModel TDD (exploration recommendation; all open questions confirmed). Android-only; no DB migrations.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/screens/RecoveryScreen.kt` | Modified | Remove dispose reset |
| `ui/screens/NewPasswordScreen.kt` | Modified | Remove dispose reset; Error CTA → Recovery; Back → Login |
| `viewmodel/auth/AuthDelegate.kt` | Modified | Token clear timing; timeouts; clear API |
| `viewmodel/AuthViewModel.kt` | Modified | `resetRecoveryState` clears delegate token |
| `MainActivity.kt` (OptoAppNavigation) | Modified | Optional `popUpTo(Recovery)` |
| `src/test/.../auth` recovery tests | Modified/New | Strict TDD for Delegate/ViewModel |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Stale LinkReceived if user leaves without handlers | Med | Reset on explicit Back/success/Error CTA; optional Login-entry clear |
| Wrong status taxonomy (e.g. 422) | Low | Retryable = network/5xx/timeout; terminal = 401/403 |
| HTTP seam expands into large refactor | Low | Small connection factory only |

## Rollback Plan

Revert the Android commit(s). No Supabase schema or RLS changes. Recovery email templates untouched.

## Dependencies

- Existing recovery flow from archived C4 (`optoapp://auth`, `AuthDelegate.updatePassword` REST PUT)
- Confirmed pre-proposal decisions (state_revision: post-explore)

## Success Criteria

- [ ] Warm deep link → NewPassword keeps `LinkReceived`; user can set password
- [ ] Retryable PUT failure retains token; retry works; 2xx and 401/403 clear token
- [ ] `resetRecoveryState()` clears UI state and delegate token
- [ ] Error CTA navigates to Recovery; Back from NewPassword goes to Login cleared
- [ ] Recovery PUT uses 10s connect / 15s read timeouts
- [ ] Failing-then-passing AuthDelegate/AuthViewModel unit tests; `testDebugUnitTest` green
