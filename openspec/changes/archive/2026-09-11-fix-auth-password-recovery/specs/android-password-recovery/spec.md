# Android Password Recovery Specification

## Purpose

Android password-recovery lifecycle for warm deep links and password update (Part 1): explicit-exit reset policy, pending recovery token lifetime, recovery PUT timeouts, and NewPassword exit navigation. Parts 2–4 auth findings are out of scope.

## Requirements

### Requirement: Explicit-Exit Recovery State Reset

The system MUST NOT reset recovery state when `RecoveryScreen` or `NewPasswordScreen` is disposed by Compose. The system MUST reset recovery state only on explicit exits (Back to Login, success return to Login, Error CTA requesting a new link, or other intentional abort handlers).

#### Scenario: Dispose during LinkReceived navigation does not reset

- GIVEN recovery state is `LinkReceived` and navigation to NewPassword begins
- WHEN `RecoveryScreen` is disposed by the NavHost
- THEN recovery state MUST remain `LinkReceived`
- AND NewPassword MUST remain usable for password entry

#### Scenario: Explicit exit clears recovery state

- GIVEN recovery state is non-Idle
- WHEN the user triggers an explicit exit that resets recovery state
- THEN recovery state MUST become `Idle`

### Requirement: NewPassword Navigation Stack Hygiene

When navigating to NewPassword because recovery state becomes `LinkReceived`, the system SHOULD pop Recovery from the back stack (`popUpTo(Recovery)`).

#### Scenario: LinkReceived navigates with popUpTo Recovery

- GIVEN recovery state transitions to `LinkReceived` while Recovery is on the back stack
- WHEN navigation to NewPassword runs
- THEN the app SHOULD remove Recovery from the back stack
- AND NewPassword SHOULD be the current destination

### Requirement: Pending Recovery Token Lifetime

`pendingRecoveryToken` MUST be retained across retryable `updatePassword` failures (network errors, timeouts, HTTP 5xx). The system MUST clear `pendingRecoveryToken` only after HTTP 2xx success OR terminal auth failure (HTTP 401 or 403). `resetRecoveryState` MUST clear `pendingRecoveryToken`.

#### Scenario: Retryable failure retains token

- GIVEN a non-blank `pendingRecoveryToken`
- WHEN `updatePassword` fails with a retryable error
- THEN `pendingRecoveryToken` MUST remain non-blank
- AND a subsequent retry MUST be able to use that token

#### Scenario: HTTP success clears token

- GIVEN a non-blank `pendingRecoveryToken`
- WHEN `updatePassword` receives HTTP 2xx
- THEN `pendingRecoveryToken` MUST be cleared

#### Scenario: Terminal auth failure clears token

- GIVEN a non-blank `pendingRecoveryToken`
- WHEN `updatePassword` receives HTTP 401 or 403
- THEN `pendingRecoveryToken` MUST be cleared
- AND recovery state MUST surface an Error suitable for requesting a new link

#### Scenario: resetRecoveryState clears token

- GIVEN a non-blank `pendingRecoveryToken` and non-Idle recovery state
- WHEN `resetRecoveryState` is invoked
- THEN recovery state MUST be `Idle`
- AND `pendingRecoveryToken` MUST be cleared

### Requirement: Terminal Error CTA Navigates to Recovery

On terminal or burned-token Error on NewPassword, the Error CTA MUST navigate to Recovery when tapped. The system MUST NOT auto-navigate to Recovery solely because Error is shown.

#### Scenario: User taps Error CTA

- GIVEN NewPassword shows Error for a terminal or burned token
- WHEN the user taps the Error CTA ("Solicitar uno nuevo")
- THEN the app MUST navigate to Recovery
- AND recovery state MUST be Idle with token cleared

#### Scenario: Error does not auto-navigate

- GIVEN NewPassword enters Error for a terminal or burned token
- WHEN the screen renders without a CTA tap
- THEN the app MUST NOT navigate to Recovery automatically

### Requirement: Back from NewPassword Clears and Returns to Login

Back from NewPassword MUST navigate to Login and MUST clear recovery state, including `pendingRecoveryToken`.

#### Scenario: Back clears recovery and opens Login

- GIVEN the user is on NewPassword with non-Idle recovery state
- WHEN they navigate Back from NewPassword
- THEN the app MUST navigate to Login
- AND recovery state MUST be Idle
- AND `pendingRecoveryToken` MUST be cleared

### Requirement: Recovery Password Update Timeouts

The `updatePassword` HTTP connection MUST use connectTimeout of 10000 milliseconds and readTimeout of 15000 milliseconds.

#### Scenario: Timeouts applied on recovery PUT

- GIVEN `updatePassword` opens an HTTP connection for the recovery password PUT
- WHEN the connection is configured
- THEN connectTimeout MUST be 10000
- AND readTimeout MUST be 15000

#### Scenario: Timeout is retryable

- GIVEN a non-blank `pendingRecoveryToken`
- WHEN the recovery PUT times out
- THEN recovery state MUST become Error
- AND `pendingRecoveryToken` MUST remain available for retry
