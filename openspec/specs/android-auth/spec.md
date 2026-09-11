# Android Auth Specification

## Purpose

Cold-start session restore after auth check, Google sign-in cancel fail-closed, and optional PIN. Empty PIN is invalid. Create PIN runs only when PIN is required and unset. This spec MUST NOT adopt mandatory-PIN-for-all (do not merge C3).

## Requirements

### Requirement: Cold Start Restores Authenticated Route

After session check completes, a valid existing session MUST leave Login. The app SHALL restore PIN unlock or main according to whether PIN is required and set. Routing MUST wait until auth check completes.

#### Scenario: Valid session leaves Login

- GIVEN a cold start with a valid stored session
- WHEN auth check completes successfully
- THEN the app MUST NOT remain on Login
- AND MUST navigate to PIN unlock or main as applicable

#### Scenario: No session stays on Login

- GIVEN a cold start with no session
- WHEN auth check completes
- THEN the app MUST remain on or return to Login

#### Scenario: Incomplete check does not restore main

- GIVEN a cold start before auth check completes
- WHEN navigation is first evaluated
- THEN the app MUST NOT treat the user as authenticated
- AND MUST wait until auth check completes before restoring PIN or main

### Requirement: Google Cancel Leaves Idle

Google sign-in MUST NOT remain in Loading when the user cancels or the flow completes without a session.

#### Scenario: User cancel is Idle or Error

- GIVEN Google sign-in is in Loading
- WHEN the user cancels the provider UI
- THEN auth state MUST become Idle or Error
- AND MUST NOT remain Loading

#### Scenario: Complete without session is not Loading

- GIVEN Google sign-in was started
- WHEN the flow finishes without an authenticated session
- THEN auth state MUST become Idle or Error
- AND MUST NOT remain Loading

### Requirement: Empty PIN Is Invalid

PIN validation MUST return false when the entered PIN is empty or when no PIN has been set. Two empty strings MUST NOT count as a match.

#### Scenario: Both empty is invalid

- GIVEN no PIN has been set
- AND the user enters an empty PIN
- WHEN PIN validation runs
- THEN the result MUST be false

#### Scenario: Unset stored PIN never matches input

- GIVEN no PIN has been set
- AND the user enters any non-empty PIN
- WHEN PIN validation runs
- THEN the result MUST be false

### Requirement: Create PIN Only When Required And Unset

The app MUST navigate to Create PIN if and only if PIN is required and no PIN has been set. PIN-required MUST default to false. This spec MUST NOT require Create PIN for all users. This change MUST NOT adopt mandatory-PIN-for-all.

(Previously: Optional PIN routing unchanged in substance; wording now explicitly forbids reviving mandatory PIN as part of the logout/create-PIN fix.)

#### Scenario: Optional PIN skips create

- GIVEN PIN is not required
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST NOT navigate to Create PIN

#### Scenario: Required unset PIN creates

- GIVEN PIN is required AND no PIN has been set
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST navigate to Create PIN

#### Scenario: Required set PIN unlocks

- GIVEN PIN is required AND a PIN has been set
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST navigate to PIN unlock
- AND MUST NOT navigate to Create PIN
### Requirement: Cold Start Awaits Deep Link Before Session Check

On cold start, when the launching intent is `ACTION_VIEW` carrying an OAuth or recovery deep link, the app MUST await completion of that deep-link handler before starting `checkExistingSession`. Non-`ACTION_VIEW` cold starts MUST run session check only. This change MUST NOT alter `SignOutScope.GLOBAL` logout semantics and MUST NOT change Activity `launchMode` (including `singleTask`).

#### Scenario: VIEW deep link completes before session check

- GIVEN a cold start with `ACTION_VIEW` OAuth or recovery deep-link data
- WHEN auth bootstrap runs
- THEN the deep-link handler MUST complete before `checkExistingSession` starts
- AND MUST NOT run both concurrently

#### Scenario: Non-VIEW cold start checks session only

- GIVEN a cold start whose intent is not `ACTION_VIEW`
- WHEN auth bootstrap runs
- THEN the app MUST run `checkExistingSession`
- AND MUST NOT invoke OAuth or recovery deep-link handlers

#### Scenario: GLOBAL sign-out and launchMode unchanged

- GIVEN this change is applied
- WHEN session logout or Activity launch configuration is evaluated
- THEN `SignOutScope.GLOBAL` behavior MUST remain unchanged
- AND Activity `launchMode` MUST remain unchanged

### Requirement: OAuth Null Deep Link Data Is Fail-Closed

`handleAuthDeepLinkIntent` with null `intent.data` MUST NOT be treated as success. The system MUST surface a non-null error and MUST NOT call `resolvePostLogin` or wipe local account/Room data for that null-data path.

#### Scenario: Null data surfaces error without post-login

- GIVEN an OAuth deep-link intent whose `data` is null
- WHEN `handleAuthDeepLinkIntent` runs
- THEN the result MUST be a non-null error
- AND auth state MUST become Error
- AND MUST NOT call `resolvePostLogin`

#### Scenario: Null data does not wipe Room

- GIVEN an OAuth deep-link intent whose `data` is null
- WHEN `handleAuthDeepLinkIntent` completes
- THEN local account/Room data MUST NOT be wiped for that path

### Requirement: onNewIntent Ignores Non-VIEW Or Null-Data OAuth

For the OAuth deep-link path, `onNewIntent` MUST ignore intents that are not `ACTION_VIEW` or that have null `data`. Such intents MUST be a no-op for OAuth handling.

#### Scenario: Non-VIEW onNewIntent is OAuth no-op

- GIVEN `onNewIntent` receives an intent that is not `ACTION_VIEW`
- WHEN OAuth deep-link handling is considered
- THEN the app MUST NOT invoke the OAuth deep-link handler

#### Scenario: Null-data onNewIntent is OAuth no-op

- GIVEN `onNewIntent` receives `ACTION_VIEW` with null `data`
- WHEN OAuth deep-link handling is considered
- THEN the app MUST NOT invoke the OAuth deep-link handler
- AND MUST NOT treat the intent as OAuth success

### Requirement: Deep Link Handlers Do Not Log Secrets

OAuth and recovery deep-link handlers MUST NOT log full deep-link URIs or values of `access_token`, `refresh_token`, or PKCE secrets. Secret-bearing `Log.d` of the raw Uri MUST NOT remain.

#### Scenario: Full URI with tokens is not logged

- GIVEN an OAuth or recovery deep link whose Uri fragment or query contains tokens
- WHEN the deep-link handler processes it
- THEN logs MUST NOT contain the full Uri
- AND MUST NOT contain `access_token`, `refresh_token`, or PKCE secret values
### Requirement: Logout Clears Device PIN State

On logout, the app MUST clear the stored device PIN secret and MUST set pin-has-been-set to false. After logout, a subsequent account MUST NOT unlock with the prior account's PIN and MUST NOT treat PIN as already set from prior device state. PIN wipe MUST run as part of local logout cleanup regardless of remote sign-out success or failure, except cancellation. This change MUST NOT alter `SignOutScope.GLOBAL` logout semantics and MUST NOT change Activity `launchMode`.

#### Scenario: Logout empties PIN secret and has-been-set

- GIVEN an authenticated account with a stored PIN and pin-has-been-set true
- WHEN the user logs out
- THEN the stored PIN secret MUST be empty
- AND pin-has-been-set MUST be false

#### Scenario: Next account cannot unlock with prior PIN

- GIVEN account A set a PIN then logged out
- WHEN account B authenticates on the same device and PIN-required becomes true without B creating a new PIN
- THEN the app MUST treat PIN as unset
- AND MUST NOT accept account A's PIN for unlock

#### Scenario: PIN wipe despite remote sign-out failure

- GIVEN logout runs and remote sign-out fails with a non-cancellation error
- WHEN local logout cleanup completes
- THEN the stored PIN secret MUST still be cleared
- AND pin-has-been-set MUST be false

### Requirement: Create PIN Persists Before Navigate

The Create PIN flow MUST successfully persist the PIN before navigating away from Create PIN. The app MUST NOT navigate to Main until persist succeeds. On persist failure or invalid PIN, the app MUST remain on Create PIN.

#### Scenario: Success navigates only after persist

- GIVEN the user confirms a valid new PIN on Create PIN
- WHEN create PIN completes successfully
- THEN the PIN MUST be persisted
- AND pin-has-been-set MUST be true
- AND the app MAY then navigate to Main

#### Scenario: Failure stays on Create PIN

- GIVEN the user confirms a PIN on Create PIN
- WHEN create PIN fails or the PIN is invalid
- THEN the app MUST remain on Create PIN
- AND MUST NOT navigate to Main

### Requirement: JWT Sync Retry Refresh Is Fail-Closed

After a JWT-expired refresh for sync retry, the session MUST have an authenticated non-anonymous user and a usable access token. If refresh completes without that, the retry path MUST NOT proceed as success.

#### Scenario: Refresh without usable user fails closed

- GIVEN sync retry refreshes the session after JWT expiry
- AND post-refresh current user is null or anonymous
- WHEN the refresh-for-retry outcome is evaluated
- THEN the outcome MUST be failure
- AND the retry MUST NOT proceed as success

#### Scenario: Refresh without usable token fails closed

- GIVEN sync retry refreshes the session after JWT expiry
- AND post-refresh access token is missing or blank
- WHEN the refresh-for-retry outcome is evaluated
- THEN the outcome MUST be failure
- AND the retry MUST NOT proceed as success

#### Scenario: Refresh with user and token may proceed

- GIVEN sync retry refreshes the session after JWT expiry
- AND post-refresh user is authenticated and non-anonymous
- AND access token is usable
- WHEN the refresh-for-retry outcome is evaluated
- THEN the outcome MAY be success
- AND retry MAY proceed
