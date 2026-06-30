# Design: Recuperación de contraseña

## Technical Approach

Supabase auth-kt expone `resetPasswordEmail(email, redirectTo)` que envía un email con magic link. El link redirige a la app via deep link `optoapp://auth#access_token=xxx&type=recovery`. La app detecta `type=recovery` en el fragment, establece la sesión de recovery, y navega a NewPasswordScreen donde el usuario elige su nueva contraseña.

## Architecture Decisions

| Decision | Options | Tradeoffs | Choice |
|----------|---------|-----------|--------|
| **Deep link detection** | A) handleDeeplinks | No documentado para recovery | **B) Parseo manual del fragment** — control total |
| **Recovery session** | A) Reusar sesión normal | Conflicto con nav guard | **B) StateFlow separado `recoveryState`** — no contamina |
| **Email template** | A) Inline en config.toml | Hardcodeado | **B) Template HTML en supabase/templates/** — versionado |
| **Navegación post-recovery** | A) Ir a main directo | Usuario no sabe su contraseña | **B) Ir a Login con mensaje** — explícito y seguro |
| **Password validation** | A) Reglas propias | Duplica lógica | **B) Reutilizar de RegisterScreen** — consistencia |

## Data Flow

```
LoginScreen
  │ tap "¿Olvidaste tu contraseña?"
  ▼
RecoveryScreen ──email──▶ AuthDelegate.sendRecoveryEmail()
  │                              │
  │ "Correo enviado"             ▼
  │                     Supabase resetPasswordEmail()
  │                              │
  │                              ▼
  │                     Email con magic link
  │                              │
  ▼                              ▼
(Usuario hace clic en el link)
  │
  ▼
MainActivity.onNewIntent()
  │ isRecoveryDeepLink() → true
  ▼
AuthViewModel.handleRecoveryDeepLink()
  │
  ├─ AuthDelegate.handleRecoveryDeepLink()
  │    ├─ Parsea fragment: access_token, refresh_token, type=recovery
  │    ├─ supabase.auth.setSession(tokens)
  │    └─ Retorna null (éxito) o error
  │
  ▼
recoveryState = LinkReceived
  │
  ▼
OptoAppNavigation ──LaunchedEffect(recoveryState)──▶ navigate("new_password")
  │
  ▼
NewPasswordScreen
  │ Valida password (mismas reglas que register)
  │ tap "Guardar contraseña"
  ▼
AuthDelegate.updatePassword(newPassword)
  │ supabase.auth.updateUser(Email.Password(newPassword))
  │
  ▼
recoveryState = PasswordUpdated
  │
  ▼
Nav a "login" con popUpTo(0) { inclusive = true }
```

## Interfaces / Contracts

```kotlin
// AuthDelegate — nuevos métodos
suspend fun sendRecoveryEmail(email: String)
suspend fun handleRecoveryDeepLink(intent: Intent): String?
suspend fun updatePassword(newPassword: String): String?

// AuthViewModel — nuevo estado
sealed class RecoveryState {
    data object Idle : RecoveryState()
    data object Loading : RecoveryState()
    data object EmailSent : RecoveryState()
    data object LinkReceived : RecoveryState()
    data object PasswordUpdated : RecoveryState()
    data class Error(val message: String) : RecoveryState()
}
val recoveryState: StateFlow<RecoveryState>

// MainActivity — nueva función
private fun isRecoveryDeepLink(intent: Intent?): Boolean
```

## Sequence Diagram

```
User        LoginScreen    AuthViewModel    AuthDelegate    Supabase
 │              │               │                │             │
 │ tap forgot   │               │                │             │
 │─────────────▶│               │                │             │
 │              │ navigate      │                │             │
 │              │──"recovery"──▶│                │             │
 │              │               │                │             │
 │ email: x@x  │               │                │             │
 │─────────────▶│               │                │             │
 │              │ sendRecovery  │                │             │
 │              │──────────────▶│                │             │
 │              │               │ resetPassword  │             │
 │              │               │───────────────▶│             │
 │              │               │                │──────200───▶│
 │              │               │◀───OK──────────│             │
 │              │ EmailSent     │                │             │
 │◀────────────│               │                │             │
 │              │               │                │             │
 │ (recibe email, hace clic)   │                │             │
 │              │               │                │             │
 │              │    MainActivity.onNewIntent()  │             │
 │              │               │ handleRecovery │             │
 │              │               │───────────────▶│             │
 │              │               │ parse fragment │             │
 │              │               │ setSession     │             │
 │              │               │◀───OK──────────│             │
 │              │               │                │             │
 │              │    OptoAppNav observe recoveryState          │
 │              │               │ navigate       │             │
 │              │               │──"new_password"│             │
 │              │               │                │             │
 │ new password │               │                │             │
 │─────────────▶│               │                │             │
 │              │ updatePassword│                │             │
 │              │──────────────▶│                │             │
 │              │               │ updateUser     │             │
 │              │               │───────────────▶│             │
 │              │               │                │──────200───▶│
 │              │               │◀───OK──────────│             │
 │              │ PasswordUpdated                │             │
 │              │ navigate──"login"              │             │
 │◀────────────│               │                │             │
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `isRecoveryDeepLink()` parsea fragment | Parametrized: type=recovery → true, type=signup → false, null → false |
| Unit | `sendRecoveryEmail()` llama a resetPasswordEmail | Mock SupabaseClient, verify called with email + redirectTo |
| Unit | `handleRecoveryDeepLink()` retorna null en éxito | Mock session, return tokens |
| Unit | `handleRecoveryDeepLink()` retorna error cuando sesión falla | Mock session returning null |
| Unit | `updatePassword()` llama a updateUser | Mock SupabaseClient, verify called |
| Unit | `RecoveryState` transiciones: Idle → Loading → EmailSent | ViewModel test con mock AuthDelegate |
| Unit | `RecoveryState` transiciones: Idle → Loading → Error | ViewModel test con mock AuthDelegate throwing |
| UI | `RecoveryScreen` muestra formulario, valida email, muestra éxito | Compose test |
| UI | `NewPasswordScreen` muestra formulario, valida password, muestra éxito | Compose test |
