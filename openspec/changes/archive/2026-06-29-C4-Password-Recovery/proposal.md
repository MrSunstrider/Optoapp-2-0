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
