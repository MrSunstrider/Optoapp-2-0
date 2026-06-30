# Tasks: C4-Password-Recovery

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450 |
| 400-line budget risk | Medium-High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 |
| Delivery strategy | ask-on-risk |

## Delivery Strategy

- **PR1**: Foundation — tests + AuthDelegate + AuthViewModel (TDD: tests first)
- **PR2**: Screens + Navigation + Supabase templates (LoginScreen optimizado, RecoveryScreen, NewPasswordScreen, MainActivity, Supabase config)

## Phase 1: Foundation (Auth + State) — PR1

> TDD strict: write failing tests BEFORE implementation.

- [x] 1.1 `strings.xml` — agregar ~20 strings de recovery y new_password
- [ ] 1.2 Tests unitarios de AuthDelegate (sendRecoveryEmail, handleRecoveryDeepLink, updatePassword)
- [x] 1.3 `AuthDelegate.kt` — implementar sendRecoveryEmail(), handleRecoveryDeepLink(), updatePassword()
- [x] 1.4 Tests unitarios de AuthViewModel (RecoveryState transitions)
- [x] 1.5 `AuthViewModel.kt` — implementar RecoveryState sealed class + stateFlow + métodos

## Phase 2: Screens + Navigation — PR2

- [x] 2.1 `LoginScreen.kt` — optimizar layout (reducir spacers, padding, espaciado)
- [x] 2.2 `LoginScreen.kt` — agregar TextButton "¿Olvidaste tu contraseña?"
- [x] 2.3 `RecoveryScreen.kt` — crear pantalla con email input, validación, estados
- [x] 2.4 `NewPasswordScreen.kt` — crear pantalla con password input, validación, estados
- [x] 2.5 `MainActivity.kt` — agregar isRecoveryDeepLink(), modificar onCreate/onNewIntent
- [x] 2.6 `OptoAppNavigation` — agregar rutas recovery y new_password, LaunchedEffect para recoveryState
- [ ] 2.7 Tests UI de RecoveryScreen y NewPasswordScreen

## Phase 3: Supabase Config — PR2

- [ ] 3.1 `supabase/config.toml` — agregar template recovery + password_changed
- [x] 3.2 `supabase/templates/recovery.html` — crear template HTML
- [x] 3.3 `supabase/templates/password_changed.html` — crear template HTML
- [ ] 3.4 `config.toml` — subir email_sent de 2 a 10

## Phase 4: Integration Tests — PR2

- [ ] 4.1 Test end-to-end: login → recovery → deep link → new password → login
- [ ] 4.2 Test edge case: usuario con sesión activa + link recovery
- [ ] 4.3 Test edge case: link expirado
