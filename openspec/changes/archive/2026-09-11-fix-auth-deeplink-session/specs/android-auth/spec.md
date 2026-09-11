# Delta for android-auth

Part 2b (JD2-C1, C4, C5). Cold-start OAuth/recovery deep-link serialization, OAuth null-data fail-closed, warm-intent guards, and no secret-bearing deep-link logs. `SignOutScope.GLOBAL` and Activity `launchMode` remain unchanged. Password-recovery UI/retryability contracts are out of scope.

## ADDED Requirements

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
