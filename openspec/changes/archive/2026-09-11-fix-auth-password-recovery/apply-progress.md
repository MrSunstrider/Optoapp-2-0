# Apply Progress: fix-auth-password-recovery

**Status**: 17/17 tasks complete. Ready for sdd-verify.
**Mode**: Strict TDD
**Delivery**: single PR (No chain; Medium ~280-380)

## Completed Tasks
- [x] 1.1–1.5 AuthDelegate timeouts + token taxonomy
- [x] 2.1–2.5 AuthViewModel reset clears token + Error/retry
- [x] 3.1–3.5 Screens/nav wiring (dispose-reset removed; CTA/Back/popUpTo/Login guard)
- [x] 4.1 RecoveryStateTest optional — no change needed
- [x] 4.2 Scope lock confirmed

## TDD Cycle Evidence
| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1–1.5 | `AuthDelegateRecoveryTest.kt` | Unit | ✅ | ✅ | ✅ 11/11 | ✅ multi status | ✅ |
| 2.1–2.5 | `AuthViewModelTest.kt` | Unit | ✅ | ✅ | ✅ 22/22 | ✅ 401/403/500 | ✅ |
| 3.1–3.5 | Screens (GREEN wiring) | N/A | N/A | ➖ | ✅ | ➖ | ➖ |
| 4.1–4.2 | RecoveryStateTest unchanged | Unit | ✅ 21/21 | ➖ | ✅ | ➖ | ➖ |

## Work Unit Evidence
| Unit | Focused command | Result | Runtime | Rollback |
|------|-----------------|--------|---------|----------|
| 1 | AuthDelegateRecoveryTest | 11/0 fail | N/A | AuthDelegate + test |
| 2 | AuthViewModelTest | 22/0 fail | N/A | AuthViewModel + test |
| 3 | recovery-related trio | 54/0 fail | Manual deferred | screen/nav files |

## Files Changed
- `optoapp/.../auth/AuthDelegate.kt` — taxonomy, timeouts, clear/has/openRecoveryPasswordConnection
- `optoapp/.../AuthViewModel.kt` — resetRecoveryState clears delegate token
- `optoapp/.../RecoveryScreen.kt` — remove dispose reset
- `optoapp/.../NewPasswordScreen.kt` — remove dispose; CTA→Recovery; Back→Login
- `optoapp/.../MainActivity.kt` — popUpTo(Recovery) inclusive
- `optoapp/.../LoginScreen.kt` — entry resetRecoveryState
- `optoapp/.../auth/AuthDelegateRecoveryTest.kt` — created
- `optoapp/.../AuthViewModelTest.kt` — recovery contracts
- `openspec/changes/fix-auth-password-recovery/tasks.md` — all [x]
