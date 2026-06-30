# Android Password Recovery Specification

## Purpose

Permite a los usuarios recuperar su contraseña mediante un enlace enviado por email. Flujo completo: ingreso de email → envío de enlace → deep link → formulario de nueva contraseña → actualización.

## Requirements

### Requirement: Recovery Entry Point

LoginScreen MUST display a "¿Olvidaste tu contraseña?" button below the "Recordar Cuenta" checkbox. The button SHALL navigate to the `recovery` route.

#### Scenario: User taps forgot password

- GIVEN the user is on LoginScreen
- WHEN they tap "¿Olvidaste tu contraseña?"
- THEN the app navigates to RecoveryScreen

### Requirement: Send Recovery Email

RecoveryScreen MUST accept an email address and call `AuthDelegate.sendRecoveryEmail(email)`. The system SHALL call `supabase.auth.resetPasswordEmail(email, redirectTo = "optoapp://auth")`. The system MUST NOT reveal whether the email exists in the database.

#### Scenario: Valid email — sends recovery email

- GIVEN the user is on RecoveryScreen with a valid email
- WHEN they tap "Enviar enlace de recuperación"
- THEN `resetPasswordEmail` is called with the email and redirectTo
- AND the screen shows "Correo enviado" with a generic message

#### Scenario: Invalid email format — validation error

- GIVEN the user is on RecoveryScreen with "not-an-email"
- WHEN they tap "Enviar enlace de recuperación"
- THEN a "Ingresá un correo electrónico válido" error is shown
- AND `resetPasswordEmail` is NOT called

#### Scenario: Network error — error message

- GIVEN the user is on RecoveryScreen
- WHEN `resetPasswordEmail` throws an IOException
- THEN a "No se pudo enviar el correo" error is shown
- AND the user can retry

### Requirement: Recovery Deep Link Detection

MainActivity MUST detect recovery deep links by checking for `type=recovery` in the URI fragment. The system SHALL NOT call `handleAuthDeepLinkIntent` for recovery links — it MUST use a separate `handleRecoveryDeepLink` path.

#### Scenario: Recovery deep link received

- GIVEN the app receives an intent with URI fragment containing `type=recovery`
- WHEN `isRecoveryDeepLink(intent)` is called
- THEN it returns `true`

#### Scenario: OAuth deep link received

- GIVEN the app receives an intent with URI fragment containing `type=signup` or no type parameter
- WHEN `isRecoveryDeepLink(intent)` is called
- THEN it returns `false`

### Requirement: Recovery Session Establishment

AuthDelegate.handleRecoveryDeepLink MUST parse the URI fragment to extract `access_token` and `refresh_token`. The system SHALL call `supabase.auth.setSession()` with these tokens. The system MUST NOT call `resolvePostLogin()` — recovery sessions are limited to `updateUser` only.

#### Scenario: Valid recovery tokens — session established

- GIVEN a recovery deep link with valid tokens
- WHEN `handleRecoveryDeepLink` processes the intent
- THEN `setSession` is called with the tokens
- AND the method returns `null` (success)

#### Scenario: Invalid or expired tokens — error

- GIVEN a recovery deep link with expired tokens
- WHEN `handleRecoveryDeepLink` processes the intent
- AND `currentSessionOrNull()` returns null after polling
- THEN the method returns an error message

### Requirement: New Password Form

NewPasswordScreen MUST display when `recoveryState == LinkReceived`. The system SHALL require two fields: new password and confirmation. The system MUST validate with the same rules as RegisterScreen: min 6 chars, lowercase, UPPERCASE, digit, symbol. Both fields MUST match.

#### Scenario: Valid password — updates successfully

- GIVEN the user is on NewPasswordScreen with `LinkReceived` state
- WHEN they enter a valid password in both fields and tap "Guardar"
- THEN `updatePassword(newPassword)` is called
- AND on success, the app navigates to LoginScreen

#### Scenario: Password too weak — validation error

- GIVEN the user enters "abc123" (no uppercase, no symbol)
- WHEN they tap "Guardar"
- THEN a password complexity error is shown

#### Scenario: Passwords don't match — validation error

- GIVEN the user enters different passwords in each field
- WHEN they tap "Guardar"
- THEN a "Las contraseñas no coinciden" error is shown

#### Scenario: Expired link — error screen

- GIVEN `recoveryState == Error` with expired link message
- WHEN NewPasswordScreen renders
- THEN it shows "El enlace de recuperación no es válido o expiró"
- AND a "Solicitar uno nuevo" button navigates to RecoveryScreen

### Requirement: Post-Recovery Navigation

After successful password update, the system MUST navigate to LoginScreen with `popUpTo(0) { inclusive = true }` to clear the navigation stack. The system SHOULD display a success message indicating the password was updated.

#### Scenario: Password updated — redirected to login

- GIVEN `recoveryState == PasswordUpdated`
- WHEN the user taps "Volver a iniciar sesión"
- THEN the app navigates to LoginScreen clearing the full stack

---

# Delta for Android Auth

## MODIFIED Requirements

### Requirement: Login Screen — Forgot Password Button

LoginScreen MUST display a "¿Olvidaste tu contraseña?" TextButton below the "Recordar Cuenta" checkbox. The button SHALL navigate to the `recovery` route when tapped.

#### Scenario: Forgot password button visible

- GIVEN the user is on LoginScreen
- WHEN the screen renders
- THEN a "¿Olvidaste tu contraseña?" button is visible below the checkbox
- AND the button navigates to `recovery` route when tapped

## ADDED Requirements

### Requirement: Recovery State Management

AuthViewModel MUST expose `recoveryState: StateFlow<RecoveryState>` as a separate state from `authState`. The system SHALL NOT mix recovery state with login/register state. The `RecoveryState` sealed class MUST include: Idle, Loading, EmailSent, LinkReceived, PasswordUpdated, Error.

#### Scenario: Recovery state starts idle

- GIVEN the app starts or navigates to login
- WHEN `recoveryState` is read
- THEN the value is `RecoveryState.Idle`

#### Scenario: Recovery state transitions on send success

- GIVEN `recoveryState` is `Loading`
- WHEN `sendRecoveryEmail` completes successfully
- THEN `recoveryState` transitions to `EmailSent`

#### Scenario: Recovery state transitions on deep link received

- GIVEN `recoveryState` is `Loading`
- WHEN `handleRecoveryDeepLink` completes successfully
- THEN `recoveryState` transitions to `LinkReceived`

#### Scenario: Recovery state transitions on password updated

- GIVEN `recoveryState` is `Loading`
- WHEN `updatePassword` completes successfully
- THEN `recoveryState` transitions to `PasswordUpdated`

#### Scenario: Recovery state transitions on error

- GIVEN `recoveryState` is `Loading`
- WHEN any recovery operation throws an exception
- THEN `recoveryState` transitions to `Error` with a user-friendly message
