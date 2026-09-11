# Design: Fix Auth Password Recovery (Part 1)

## Technical Approach

Fix warm deep-link abort (JD-C1) and burned-token retries (JD-S4) without schema/RLS or Parts 2–4. Keep `AuthDelegate.updatePassword` REST PUT. Match `android-password-recovery`: explicit-exit reset, `popUpTo(Recovery)`, token taxonomy, Error CTA → Recovery (button), Back → Login + clear, connect 10s / read 15s. Strict TDD: Delegate → ViewModel → screens.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|----------|---------|--------|-----------|
| Reset policy | Dispose / Conditional / Explicit-exit | **Explicit-exit** | Dispose races `LinkReceived`; C4 never required dispose reset |
| Stack hygiene | None / `popUpTo(Recovery)` | **`popUpTo(Route.Recovery) { inclusive = true }`** | Prevents Back→EmailSent; alone does not fix C1 |
| Token lifetime | Clear-before-PUT / Clear on 2xx only / 2xx+401/403 | **Clear on 2xx or 401/403; keep else** | Network/5xx retries; burn invalid tokens |
| Timeouts | None / 8s-8s / 10s-15s / Ktor | **10_000 / 15_000** | Spec; avoid hang; defer Ktor (bad_jwt) |
| Error CTA | Auto-nav / Button | **Button → Recovery** | Spec; user confirms re-request |
| NewPassword Back | `popBackStack` / Login+clear | **Login + reset** | Spec; link treated consumed |
| HTTP seam | Rewrite client / Small factory | **`openRecoveryPasswordConnection`** | Unit-test timeouts/status |
| Stale state | Ignore / Login-entry clear | **Login entry resets** | Guards leave-without-handler |

### HTTP status taxonomy (`updatePassword`)

| Outcome | Codes / errors | Token | UI |
|---------|----------------|-------|-----|
| Success | `200..299` | Clear | `PasswordUpdated` |
| Terminal auth | `401`, `403` | Clear | `Error` (CTA → Recovery) |
| Retryable transport | `IOException`, timeout; rethrow cancel | Keep | `Error` (retry on screen) |
| Retryable server | `500..599` | Keep | `Error` |
| Retryable client (non-auth) | Other `4xx` (`400`,`422`,`429`,…) | Keep | `Error` (token reusable) |
| Blank token | n/a | empty | `Error`; CTA → Recovery |

Never log Bearer/fragment `access_token` — HTTP code + sanitized body only. Token lives in process memory until clear/reset/death.

## Data Flow

```
Deep link → pendingRecoveryToken + LinkReceived
  → navigate(NewPassword) { popUpTo(Recovery) inclusive }
  → updatePassword (clear only per taxonomy)
       ├─ 2xx → clear → PasswordUpdated → Login + reset
       ├─ 401/403 → clear → Error → CTA → reset + Recovery
       └─ retryable → keep → Error → retry on NewPassword
Back (NewPassword) → resetRecoveryState → Login
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `viewmodel/auth/AuthDelegate.kt` | Modify | Taxonomy; timeouts; `clearPendingRecoveryToken()`; connection seam; `hasPendingRecoveryToken()` |
| `viewmodel/AuthViewModel.kt` | Modify | `resetRecoveryState()` clears delegate token |
| `MainActivity.kt` | Modify | LinkReceived nav with `popUpTo(Recovery)` inclusive |
| `ui/screens/RecoveryScreen.kt` | Modify | Remove dispose reset; keep explicit exits |
| `ui/screens/NewPasswordScreen.kt` | Modify | Remove dispose; CTA → Recovery; Back → Login+clear |
| `ui/screens/LoginScreen.kt` | Modify | Entry `resetRecoveryState()` stale guard |
| `src/test/.../AuthDelegateRecoveryTest.kt` | Create | RED→GREEN: timeouts, keep/clear by status |
| `src/test/.../AuthViewModelTest.kt` | Modify | RED→GREEN: reset clears token; Error/retry |
| `src/test/.../RecoveryStateTest.kt` | Modify | Optional only; not primary |

No Supabase migrations, edge functions, or RLS.

## Interfaces / Contracts

```kotlin
fun clearPendingRecoveryToken()
internal fun hasPendingRecoveryToken(): Boolean
internal fun openRecoveryPasswordConnection(url: URL): HttpURLConnection // 10s/15s
// updatePassword: use local token copy; clear only per taxonomy
```

`resetRecoveryState()`: Idle then `authDelegate.clearPendingRecoveryToken()`.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit 1st | Token/timeouts | MockK connection via factory |
| Unit 2nd | reset + Error/retry | Mock AuthDelegate in ViewModelTest |
| Screen | CTA/Back only if needed | After Delegate/VM; no dispose instrumented tests |
| E2E | — | Out of Part 1 |

Order: **Delegate → ViewModel → screens**.

## Threat Matrix

N/A for shell/git/PR/executable-path rows — Android NavHost + in-process HTTP.

**Auth notes:** recovery JWT in memory until clear; never log token; no auto-nav on Error; `popUpTo(Recovery)` blocks EmailSent + live token.

## Migration / Rollout

No migration. Revert Android commit(s) to roll back.

## Open Questions

- None — CTA button, Back→Login, and 10s/15s already decided.
