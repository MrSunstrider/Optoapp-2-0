# Tasks: C3-Default-PIN

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~395 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | DataStore flag + SecurityManager + SessionManager + migration logic | PR 1 | Foundation: no UI, all data layer |
| 2 | CreatePinScreen + MainActivity nav guard + AuthViewModel wiring | PR 2 | Depends on PR 1; full user-facing flow |
| 3 | ConfiguracionScreen create/change mode + PinScreen title | PR 3 | Depends on PR 1 only |
| 4 | Tests for all units | Across PRs | Test each PR's code in same PR |

## Phase 1: Foundation (DataStore Flag + Data Layer)

- [ ] 1.1 `SecurityManager.kt` — remove `DEFAULT_PIN`, remove fallback in `getSecurePin()`, add `pref_pin_has_been_set` DataStore key
- [ ] 1.2 `SecurityManager.kt` — expose `pinHasBeenSet: Flow<Boolean>`, migrate `savePin()` to set flag after ESP write
- [ ] 1.3 `SecurityManager.kt` — add init-block migration: auto-set flag=true if ESP has custom PIN (!= "" and != "123456")
- [ ] 1.4 `SecurityManager.kt` — `getStoredPin()` returns "" when flag is false
- [ ] 1.5 `SessionManager.kt` — add `PIN_HAS_BEEN_SET` key in companion, expose `pinHasBeenSet: Flow<Boolean>`, add `setPinHasBeenSet()` + `hasBeenSetAtLeastOnce()` helpers

## Phase 2: Navigation Guard + New Screen

- [ ] 2.1 `AuthViewModel.kt` — expose `pinHasBeenSet: StateFlow<Boolean>` from `sessionManager.pinHasBeenSet`
- [ ] 2.2 `MainActivity.kt` — add `create_pin` composable route; add `pinHasBeenSet` to `LaunchedEffect(isAuthChecked)` nav guard (priority: `create_pin` > `pin` > `main`)
- [ ] 2.3 Create `CreatePinScreen.kt` — UI: 6-digit PIN entry + confirmation field, numeric keypad, validation (match, length, weak patterns), "Crear PIN" title, navigates to `main` on success

## Phase 3: Settings Adaptation + Polish

- [ ] 3.1 `ConfiguracionScreen.kt` — pass `pinHasBeenSet: Boolean` to `SecuritySection`; when false: hide "PIN actual" field, label button "Crear PIN"
- [ ] 3.2 `PinScreen.kt` — update title text to clarify verification mode (not creation)

## Phase 4: Testing

- [ ] 4.1 Unit: `SecurityManager.getStoredPin()` returns "" when `pinHasBeenSet=false` (mock DataStore)
- [ ] 4.2 Unit: `SecurityManager.savePin()` sets DataStore flag after ESP write (mock both, verify order)
- [ ] 4.3 Unit: Weak PIN validation — parametrized (`111111`, `123456`, `654321`, `abcdef` → rejected)
- [ ] 4.4 Integration: Nav graph redirects `loading → create_pin` when `!pinHasBeenSet` (instrumented, mock SecurityManager)
- [ ] 4.5 Integration: Migration auto-sets flag when custom PIN exists in ESP (pre-seed EncryptedSharedPrefs, verify flag after init)
