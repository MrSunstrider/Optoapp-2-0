# Delta for android-password-recovery

Part 2a (JD2-C2 + JD2-C3 only). Closes NewPassword treating every Error as terminal, and Login entry `resetRecoveryState` racing cold-start deep links. Dispose policy, token taxonomy, timeouts, and popUpTo remain unchanged.

## ADDED Requirements

### Requirement: Recovery Error Retryability Flag

After a failed `updatePassword`, recovery state MUST become `Error` that includes `isRetryable` derived from whether a pending recovery token remains (`hasPendingRecoveryToken()`). Blank-token or invalid-link Errors MUST set `isRetryable=false`.

#### Scenario: Retryable failure sets isRetryable true

- GIVEN a non-blank pending recovery token
- WHEN `updatePassword` fails and the pending token is still present
- THEN recovery state MUST be `Error` with `isRetryable=true`

#### Scenario: Terminal or blank-token failure sets isRetryable false

- GIVEN `updatePassword` fails after the pending token was cleared, or no pending token existed
- WHEN recovery state becomes `Error`
- THEN `isRetryable` MUST be `false`

### Requirement: Retryable NewPassword Error Keeps Form

When NewPassword shows `Error(isRetryable=true)`, the system MUST keep the password form usable and MUST allow the user to retry `updatePassword` without calling `resetRecoveryState`.

#### Scenario: Retryable Error keeps form and permits retry

- GIVEN NewPassword shows `Error` with `isRetryable=true`
- WHEN the screen renders
- THEN password fields and submit MUST remain usable
- AND the UI MUST NOT call `resetRecoveryState` solely to present retry

#### Scenario: User retries without resetting recovery

- GIVEN NewPassword shows `Error` with `isRetryable=true` and a pending token remains
- WHEN the user submits a new password
- THEN the system MUST attempt `updatePassword` again using the retained token
- AND MUST NOT have cleared recovery via `resetRecoveryState` for that retry path

## MODIFIED Requirements

### Requirement: Explicit-Exit Recovery State Reset

The system MUST NOT reset recovery state when `RecoveryScreen` or `NewPasswordScreen` is disposed by Compose. The system MUST NOT call `resetRecoveryState` on `LoginScreen` composition or entry. The system MUST reset recovery state only on explicit exits (Back to Login, success return to Login, terminal Error CTA requesting a new link, Recovery abort handlers, or other intentional abort handlers).
(Previously: Explicit exits only; dispose must not reset — did not forbid Login entry/composition reset, which introduced JD2-C3.)

#### Scenario: Dispose during LinkReceived navigation does not reset

- GIVEN recovery state is `LinkReceived` and navigation to NewPassword begins
- WHEN `RecoveryScreen` is disposed by the NavHost
- THEN recovery state MUST remain `LinkReceived`
- AND NewPassword MUST remain usable for password entry

#### Scenario: Explicit exit clears recovery state

- GIVEN recovery state is non-Idle
- WHEN the user triggers an explicit exit that resets recovery state
- THEN recovery state MUST become `Idle`

#### Scenario: Login entry does not reset recovery

- GIVEN a pending recovery token and/or non-Idle recovery state (including cold-start deep link toward Login)
- WHEN `LoginScreen` composes or runs its entry effect
- THEN the system MUST NOT call `resetRecoveryState`
- AND the pending recovery token MUST remain available for NewPassword

### Requirement: Terminal Error CTA Navigates to Recovery

When NewPassword shows `Error(isRetryable=false)` (terminal or burned-token), the Error CTA MUST call `resetRecoveryState` and navigate to Recovery when tapped. The system MUST NOT auto-navigate to Recovery solely because Error is shown.
(Previously: Terminal/burned-token Error CTA navigates Recovery on tap without auto-nav; did not bind to `isRetryable=false` or require reset on CTA.)

#### Scenario: User taps terminal Error CTA

- GIVEN NewPassword shows `Error` with `isRetryable=false`
- WHEN the user taps the Error CTA ("Solicitar uno nuevo")
- THEN the app MUST call `resetRecoveryState`
- AND MUST navigate to Recovery
- AND recovery state MUST be Idle with token cleared

#### Scenario: Terminal Error does not auto-navigate

- GIVEN NewPassword enters `Error` with `isRetryable=false`
- WHEN the screen renders without a CTA tap
- THEN the app MUST NOT navigate to Recovery automatically
- AND MUST NOT call `resetRecoveryState` solely because Error is shown
