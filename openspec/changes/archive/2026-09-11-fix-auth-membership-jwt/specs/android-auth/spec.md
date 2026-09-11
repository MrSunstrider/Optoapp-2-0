# Delta for android-auth

JD2-C7. JWT-expired sync-retry refresh MUST fail-closed unless post-refresh session has an authenticated non-anonymous user and a usable access token.

## ADDED Requirements

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
