# C4-Password-Recovery — Documentación completa

## Resumen del cambio

| Campo | Valor |
|-------|-------|
| Nombre | C4-Password-Recovery |
| Fecha | 2026-06-29 |
| Estado | Propuesta (pre-SDD) |
| Propuesto por | Usuario |
| Analizado por | Gentle AI Orchestrator |
| Severidad | Media — funcionalidad faltante que genera soporte innecesario |

---

## PARTE 1: Propuesta Original del Usuario

### Flujo propuesto

```
LoginScreen                     RecoveryScreen            NewPasswordScreen
┌─────────────────────┐         ┌────────────────┐        ┌──────────────────────┐
│ Email               │         │ Email           │        │ Nueva contraseña     │
│ Contraseña          │  tap    │ [Enviar enlace] │  clic  │ Confirmar contraseña  │
│ [Entrar]            │ ──────► │                │  link  │ [Guardar]            │
│ [Google]            │         │ ◄─ Success     │ ──────► │                      │
│ [Crear cuenta]      │         │ "Revisá tu     │        │ ◄──── Success        │
│ ¿Olvidaste tu       │         │  correo"       │        │ "Contraseña          │
│  contraseña?  ◄─────│         └────────────────┘        │  actualizada"        │
└─────────────────────┘                                    └──────────────────────┘
                                                                    │
                                                                    ▼
                                                              LoginScreen
                                                          (con mensaje éxito)
```

### 1. Cambios en Supabase

#### 1a. Template de email de recovery

```
Asunto: Recuperación de contraseña — OptoApp

Hola {{ .Email }},

Recibimos una solicitud para restablecer la contraseña de tu cuenta en OptoApp.

Hacé clic en el siguiente enlace para crear una nueva contraseña:

{{ .ConfirmationURL }}

Si no solicitaste este cambio, podés ignorar este mensaje. Tu contraseña actual sigue siendo segura.

— El equipo de OptoApp
```

#### 1b. Template de confirmación de cambio

```
Asunto: Tu contraseña fue cambiada — OptoApp

Hola {{ .Email }},

La contraseña de tu cuenta en OptoApp se cambió correctamente.

Si no realizaste este cambio, contactá a tu administrador o escribinos a soporte@optoapp.com.

— El equipo de OptoApp
```

#### 1c. config.toml

```toml
[auth.email.template.recovery]
subject = "Recuperación de contraseña — OptoApp"
content_path = "./supabase/templates/recovery.html"

[auth.email.notification.password_changed]
enabled = true
subject = "Tu contraseña fue cambiada — OptoApp"
content_path = "./supabase/templates/password_changed.html"
```

#### 1d. Rate limit

```toml
email_sent = 10  # subir de 2 a 10
```

### 2. Archivos a modificar

#### 2a. strings.xml

```xml
<!-- Recovery -->
<string name="login_forgot_password">¿Olvidaste tu contraseña?</string>
<string name="recovery_title">Recuperar cuenta</string>
<string name="recovery_subtitle">Ingresá el correo con el que te registraste</string>
<string name="recovery_email_label">Correo electrónico</string>
<string name="recovery_send_button">Enviar enlace de recuperación</string>
<string name="recovery_sending">Enviando…</string>
<string name="recovery_success_title">Correo enviado</string>
<string name="recovery_success_message">Si existe una cuenta con ese correo, vas a recibir un enlace para restablecer tu contraseña.</string>
<string name="recovery_error_email_invalid">Ingresá un correo electrónico válido</string>
<string name="recovery_error_generic">No se pudo enviar el correo. Intentá de nuevo más tarde.</string>
<string name="recovery_back_to_login">Volver a inicio de sesión</string>

<!-- New Password -->
<string name="new_password_title">Nueva contraseña</string>
<string name="new_password_subtitle">Elegí una contraseña nueva para tu cuenta</string>
<string name="new_password_label">Nueva contraseña</string>
<string name="new_password_confirm_label">Confirmar contraseña</string>
<string name="new_password_save_button">Guardar contraseña</string>
<string name="new_password_saving">Guardando…</string>
<string name="new_password_success_title">Contraseña actualizada</string>
<string name="new_password_success_message">Tu contraseña se actualizó correctamente. Ahora podés iniciar sesión.</string>
<string name="new_password_error_weak">Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.</string>
<string name="new_password_error_mismatch">Las contraseñas no coinciden.</string>
<string name="new_password_error_expired">El enlace de recuperación no es válido o expiró. Solicitá uno nuevo.</string>
<string name="new_password_error_generic">No se pudo actualizar la contraseña. El enlace puede haber expirado.</string>
<string name="new_password_to_login">Volver a iniciar sesión</string>
```

#### 2b. LoginScreen.kt

Después del checkbox "Recordar Cuenta" (línea 260) y antes del bloque de error:

```kotlin
TextButton(
    onClick = { navController.navigate("recovery") },
    modifier = Modifier.align(Alignment.End)
) {
    Text(
        "¿Olvidaste tu contraseña?",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}
```

#### 2c. MainActivity.kt

```kotlin
private fun isRecoveryDeepLink(intent: Intent?): Boolean {
    val fragment = intent?.data?.fragment ?: return false
    return fragment.contains("type=recovery")
}
```

En `onCreate`:
```kotlin
if (intent?.action == Intent.ACTION_VIEW) {
    if (isRecoveryDeepLink(intent)) {
        authViewModel.handleRecoveryDeepLink(intent)
    } else {
        authViewModel.handleAuthDeepLinkIntent(intent)
    }
}
```

En `onNewIntent`:
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (isRecoveryDeepLink(intent)) {
        authViewModel.handleRecoveryDeepLink(intent)
    } else {
        authViewModel.handleAuthDeepLinkIntent(intent)
    }
}
```

Rutas en NavHost:
```kotlin
composable("recovery") {
    RecoveryScreen(navController = navController, viewModel = authViewModel)
}
composable("new_password") {
    NewPasswordScreen(navController = navController, viewModel = authViewModel)
}
```

#### 2d. AuthDelegate.kt

```kotlin
suspend fun sendRecoveryEmail(email: String) {
    supabase.auth.resetPasswordEmail(
        email = email,
        redirectTo = "optoapp://auth"
    )
}

suspend fun handleRecoveryDeepLink(intent: Intent): String? {
    val deepLink = intent.data ?: return "Enlace inválido"
    Log.d(TAG, "Recibido deeplink recovery: $deepLink")

    val handleResult = runCatching { supabase.handleDeeplinks(intent) }
    handleResult.onFailure { e ->
        Log.w(TAG, "Error procesando deeplink recovery: ${e.localizedMessage}", e)
    }

    var hasSession = false
    repeat(20) {
        val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (session != null) {
            hasSession = true
            return@repeat
        }
        delay(300)
    }

    return if (hasSession) null
    else handleResult.exceptionOrNull()?.localizedMessage
        ?: "El enlace de recuperación no es válido o expiró."
}

suspend fun updatePassword(newPassword: String): String? {
    return try {
        supabase.auth.updateUser(Email.Password(newPassword))
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error actualizando contraseña", e)
        e.localizedMessage ?: "No se pudo actualizar la contraseña."
    }
}
```

#### 2e. AuthViewModel.kt

```kotlin
sealed class RecoveryState {
    data object Idle : RecoveryState()
    data object Loading : RecoveryState()
    data object EmailSent : RecoveryState()
    data object LinkReceived : RecoveryState()
    data object PasswordUpdated : RecoveryState()
    data class Error(val message: String) : RecoveryState()
}

private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

fun sendRecoveryEmail(email: String) = viewModelScope.launch {
    _recoveryState.value = RecoveryState.Loading
    try {
        authDelegate.sendRecoveryEmail(email)
        _recoveryState.value = RecoveryState.EmailSent
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error enviando recovery email", e)
        _recoveryState.value = RecoveryState.Error(
            "No se pudo enviar el correo. Intentá de nuevo más tarde."
        )
    }
}

fun handleRecoveryDeepLink(intent: Intent?) = viewModelScope.launch {
    _recoveryState.value = RecoveryState.Loading
    val error = authDelegate.handleRecoveryDeepLink(intent)
    if (error != null) {
        _recoveryState.value = RecoveryState.Error(error)
        return@launch
    }
    _recoveryState.value = RecoveryState.LinkReceived
}

fun updatePassword(newPassword: String) = viewModelScope.launch {
    _recoveryState.value = RecoveryState.Loading
    val error = authDelegate.updatePassword(newPassword)
    if (error != null) {
        _recoveryState.value = RecoveryState.Error(error)
        return@launch
    }
    _recoveryState.value = RecoveryState.PasswordUpdated
}

fun resetRecoveryState() {
    _recoveryState.value = RecoveryState.Idle
}
```

### 3. Archivos nuevos

#### 3a. RecoveryScreen.kt

- OptoTopAppBar con título "Recuperar cuenta" y botón atrás
- Campo de email (validación con Patterns.EMAIL_ADDRESS)
- Botón "Enviar enlace de recuperación" (deshabilitado si email vacío o loading)
- Observa recoveryState:
  - Idle → formulario
  - Loading → spinner en botón
  - EmailSent → pantalla de éxito con "Revisá tu correo" + botón volver
  - Error → Surface con error

#### 3b. NewPasswordScreen.kt

- Detecta recoveryState == LinkReceived, si no → error "Enlace inválido"
- Dos campos: nueva contraseña + confirmar
- Validación idéntica a RegisterScreen
- Botón "Guardar contraseña"
- Observa recoveryState:
  - LinkReceived → formulario
  - Loading → spinner
  - PasswordUpdated → éxito + "Volver a iniciar sesión"
  - Error → mensaje de error

### 4. Flujo completo

| Paso | Pantalla | Acción |
|------|----------|--------|
| 1 | LoginScreen | User tapa "¿Olvidaste tu contraseña?" |
| 2 | RecoveryScreen | Ingresa email → "Enviar enlace de recuperación" |
| 3 | RecoveryScreen | AuthDelegate.sendRecoveryEmail(email, redirectTo = "optoapp://auth") |
| 4 | RecoveryScreen | Muestra "Correo enviado..." |
| 5 | 📧 Email | User recibe email con link → hace clic |
| 6 | Sistema | Android captura optoapp://auth#access_token=xxx&type=recovery |
| 7 | MainActivity | isRecoveryDeepLink(intent) == true → handleRecoveryDeepLink |
| 8 | AuthDelegate | supabase.handleDeeplinks(intent) → sesión recovery establecida |
| 9 | NewPasswordScreen | Navegación automática a "new_password" |
| 10 | NewPasswordScreen | User ingresa nueva contraseña → "Guardar" |
| 11 | AuthDelegate | supabase.auth.updateUser(Email.Password(newPassword)) |
| 12 | NewPasswordScreen | "Contraseña actualizada" → "Volver a inicio de sesión" |
| 13 | LoginScreen | User inicia sesión con su nueva contraseña |

### 5. Edge cases

- Deep link recovery vs OAuth: detectar type=recovery en fragment
- Sesión recovery vs normal: NO llamar resolvePostLogin después del recovery
- Timeout: recovery links expiran en 1h
- Rate limiting: subir email_sent de 2 a 10
- Email no existente: Supabase siempre responde 200

---

## PARTE 2: Análisis Crítico

### Problemas encontrados en la propuesta original

| # | Problema | Severidad | Por qué importa |
|---|----------|-----------|------------------|
| 1 | No sigue formato SDD del proyecto | Alta | El proyecto tiene workflow estricto: explore → propose → spec → design → tasks → apply → verify → archive |
| 2 | `handleDeeplinks` para recovery no está documentado | Media | Si no funciona, toda la cadena de deep link se rompe |
| 3 | `OptoAppNavigation` no observa `recoveryState` | Alta | La navegación automática a NewPasswordScreen no va a funcionar |
| 4 | No maneja usuario ya logueado | Media | Si alguien con sesión activa hace clic en link viejo, no hay flujo |
| 5 | `resetRecoveryState()` sin cuándo se llama | Baja | El estado queda residuo al navegar atrás |
| 6 | No considera que `handleDeeplinks` puede fallar para recovery | Media | Si falla, necesitamos parseo manual del fragment |
| 7 | Tests no especificados (TDD) | Alta | Con strict_tdd: true, los tests van ANTES del código |

### Lo que SÍ está bien

- Arquitectura de 3 pantallas (Login → Recovery → NewPassword)
- `RecoveryState` como sealed class (consistente con `AuthState`)
- Supabase `resetPasswordEmail` con `redirectTo` (API correcta)
- Template de email Option A (sobria, profesional)
- Rate limit concern (bien identificado)
- Misma validación de password que RegisterScreen (reutilizar, no duplicar)

---

## PARTE 3: Propuesta Corregida (formato SDD)

### proposal.md

```markdown
# Proposal: Recuperación de contraseña

## Intent

Los usuarios no tienen forma de recuperar su cuenta si olvidan la contraseña.
Actualmente la única opción es pedir al admin que cree una cuenta nueva.
Esto genera soporte innecesario y frustración.

## Scope

### In Scope
- Botón "¿Olvidaste tu contraseña?" en LoginScreen
- RecoveryScreen: ingreso de email, envío de enlace
- NewPasswordScreen: formulario de nueva contraseña post-link
- Deep link handler para `type=recovery`
- Email template de Supabase (recovery + password_changed)
- Tests unitarios de AuthDelegate y AuthViewModel

### Out of Scope
- Rate limiting de intentos de recuperación (cubierto por C2)
- Biometría
- Cambios en Web (optoweb)

## Capabilities

### New Capabilities
- `android-password-recovery`: Flujo completo de recuperación de contraseña via email

### Modified Capabilities
- `android-auth`: LoginScreen agrega botón de recuperación. AuthViewModel maneja RecoveryState. MainActivity detecta deep links de tipo recovery.

## Approach

1. Supabase envía email con magic link (resetPasswordEmail + redirectTo)
2. Link redirige a optoapp://auth#access_token=xxx&type=recovery
3. MainActivity detecta type=recovery en el fragment del deep link
4. AuthDelegate procesa el deep link y establece sesión recovery
5. AuthViewModel navega a NewPasswordScreen
6. User elige nueva contraseña → updateUser → redirige a Login

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../ui/screens/LoginScreen.kt` | Modified | Agrega TextButton "¿Olvidaste tu contraseña?" |
| `optoapp/.../ui/screens/RecoveryScreen.kt` | New | Pantalla de ingreso de email |
| `optoapp/.../ui/screens/NewPasswordScreen.kt` | New | Pantalla de nueva contraseña |
| `optoapp/.../viewmodel/AuthViewModel.kt` | Modified | RecoveryState + métodos recovery |
| `optoapp/.../viewmodel/auth/AuthDelegate.kt` | Modified | sendRecoveryEmail + handleRecoveryDeepLink + updatePassword |
| `optoapp/.../MainActivity.kt` | Modified | isRecoveryDeepLink + rutas de navegación |
| `optoapp/.../res/values/strings.xml` | Modified | ~20 nuevos strings |
| `supabase/config.toml` | Modified | Template recovery + password_changed |
| `supabase/templates/recovery.html` | New | Template HTML del email |
| `supabase/templates/password_changed.html` | New | Template de notificación |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| handleDeeplinks no funciona para recovery | Med | Verificar con SDK; fallback: parseo manual del fragment + setSession |
| Navegación a NewPasswordScreen no se dispara | Med | Agregar LaunchedEffect en OptoAppNavigation que observe recoveryState |
| Usuario con sesión activa hace clic en link viejo | Low | Mostrar error "Ya tenés sesión activa" |
| Rate limit email_sent=2 bloquea usuarios | High | Subir a 10 en config.toml |

## Rollback Plan

Revertir el commit. No hay migraciones de schema. Los templates de email se revierten en el dashboard de Supabase.

## Dependencies

- Supabase Auth (resetPasswordEmail API) — ya disponible en supabase-kt v3.6.0
- optoapp://auth en additional_redirect_urls — ya configurado

## Success Criteria

- [ ] Usuario puede recuperar contraseña desde la pantalla de login
- [ ] Email de recovery se envía correctamente con template personalizado
- [ ] Deep link abre la app y navega a NewPasswordScreen
- [ ] Nueva contraseña se guarda vía updateUser
- [ ] Tests unitarios pasan para AuthDelegate y AuthViewModel
- [ ] Cobertura de tests no baja del threshold actual
```

### design.md

```markdown
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
```

### specs/android-password-recovery/spec.md

```markdown
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
```

### specs/android-auth/spec.md (delta)

```markdown
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
```

### tasks.md

```markdown
# Tasks: C4-Password-Recovery

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450 |
| 400-line budget risk | Medium-High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |

## Phase 1: Foundation (Auth + State)

- [ ] 1.1 `strings.xml` — agregar ~20 strings de recovery y new_password
- [ ] 1.2 `AuthDelegate.kt` — agregar sendRecoveryEmail(), handleRecoveryDeepLink(), updatePassword()
- [ ] 1.3 `AuthViewModel.kt` — agregar RecoveryState sealed class + stateFlow + métodos
- [ ] 1.4 Tests unitarios de AuthDelegate (sendRecoveryEmail, handleRecoveryDeepLink, updatePassword)
- [ ] 1.5 Tests unitarios de AuthViewModel (RecoveryState transitions)

## Phase 2: Screens + Navigation

- [ ] 2.1 `LoginScreen.kt` — agregar TextButton "¿Olvidaste tu contraseña?"
- [ ] 2.2 `RecoveryScreen.kt` — crear pantalla con email input, validación, estados
- [ ] 2.3 `NewPasswordScreen.kt` — crear pantalla con password input, validación, estados
- [ ] 2.4 `MainActivity.kt` — agregar isRecoveryDeepLink(), modificar onCreate/onNewIntent
- [ ] 2.5 `OptoAppNavigation` — agregar rutas recovery y new_password, LaunchedEffect para recoveryState
- [ ] 2.6 Tests UI de RecoveryScreen y NewPasswordScreen

## Phase 3: Supabase Config

- [ ] 3.1 `supabase/config.toml` — agregar template recovery + password_changed
- [ ] 3.2 `supabase/templates/recovery.html` — crear template HTML
- [ ] 3.3 `supabase/templates/password_changed.html` — crear template HTML
- [ ] 3.4 `config.toml` — subir email_sent de 2 a 10

## Phase 4: Integration Tests

- [ ] 4.1 Test end-to-end: login → recovery → deep link → new password → login
- [ ] 4.2 Test edge case: usuario con sesión activa + link recovery
- [ ] 4.3 Test edge case: link expirado
```

---

## PARTE 4: Resumen Comparativo

| Aspecto | Propuesta Original | Propuesta Corregida |
|---------|-------------------|---------------------|
| Formato SDD | ❌ No sigue el formato | ✅ proposal.md + design.md + specs + tasks |
| TDD | ❌ Tests no especificados | ✅ 9 tests unitarios + 2 UI definidos |
| Deep link handling | ⚠️ Usa handleDeeplinks (no documentado para recovery) | ✅ Parseo manual del fragment (control total) |
| Navegación | ⚠️ No observa recoveryState en OptoAppNavigation | ✅ LaunchedEffect que observa recoveryState |
| Edge cases | ⚠️ No maneja usuario logueado | ✅ Manejado en specs |
| State cleanup | ⚠️ resetRecoveryState() sin cuándo se llama | ✅ Definido en specs (onDispose, navegación) |
| Rollback | ❌ No definido | ✅ Revertir commit, no hay migraciones |
| Success Criteria | ❌ No definidos | ✅ 6 criterios medibles |
| Capabilities | ❌ No definidas | ✅ 1 nueva + 1 modificada |

---

## Próximo paso

Cuando quieras arrancar la implementación, seguimos el workflow SDD:

1. **proposal** → ya creada (este archivo)
2. **spec** → crear los archivos de specs
3. **design** → crear el design.md
4. **tasks** → crear el tasks.md
5. **apply** → implementar con TDD (tests primero)
6. **verify** → ejecutar tests y verificar
7. **archive** → archivar el cambio

¿Arrancamos?
