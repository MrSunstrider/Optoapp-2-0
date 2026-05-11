# Design: Eliminar PIN por defecto y forzar creación en primer uso

## Technical Approach

Introduce a DataStore boolean flag `pref_pin_has_been_set` as the single source of truth for PIN setup state. `SecurityManager` exposes it as a reactive `Flow<Boolean>`. On app launch, `MainActivity`'s nav guard routes to `create_pin` when `isLoggedIn && !pinHasBeenSet`. Migration: on init, if an existing custom PIN is found in EncryptedSharedPreferences but the flag is false, auto-set the flag to true (existing users with custom PIN are undisturbed).

## Architecture Decisions

| Decision | Options | Tradeoffs | Choice |
|----------|---------|-----------|--------|
| **Setup detection** | A) Check stored PIN == "123456" | Weak: a user could legitimately choose 123456 | **B) DataStore flag `pin_has_been_set`** — unambiguous, survives upgrades |
| **Flag owner** | A) SessionManager | Not a session concern; hides PIN coupling from session | **B) SecurityManager** — PIN is security; shares DataStore already |
| **Navigation guard** | A) NavHost condition only | Misses post-login redirections | **B) NavHost + AuthViewModel combine** — `loading` composable checks `pinHasBeenSet`; future post-onboarding flows also use it |
| **CreatePinScreen** | A) Modify PinScreen with mode param | Pollutes existing verification screen | **B) New `CreatePinScreen` composable** — distinct UX: confirmation fields, no current PIN, "Crear PIN" title |
| **Configuración** | A) Two separate sections | Boilerplate | **B) Single `SecuritySection` with `pinHasBeenSet` param** — hides "PIN actual" when false, changes button label to "Crear PIN" |

## Data Flow

```
App Launch
  │
  ▼
AuthViewModel.checkExistingSession()
  │
  ▼
 SecurityManager ──reads──▶ DataStore(settings) ──▶ Flow<Boolean>: pinHasBeenSet
  │                        EncryptedSharedPrefs ──▶ Flow<String>: userPin
  ▼
Nav loading ─── LaunchedEffect(isAuthChecked, pinHasBeenSet) ───▶
  │  isLoggedIn && !pinHasBeenSet  ──▶ create_pin
  │  isLoggedIn && isPinRequired   ──▶ pin
  │  isLoggedIn && !isPinRequired  ──▶ main
  │  !isLoggedIn                   ──▶ login
```

### PIN Creation Flow

```
CreatePinScreen ──enters 6 digits──▶ SecurityManager.savePin(pin)
                                          │
                              ┌───────────┘
                              │ 1. Write PIN to EncryptedSharedPreferences
                              │ 2. Set pref_pin_has_been_set = true in DataStore
                              │ 3. Update _pinFlow MutableStateFlow
                              └───▶ navController.navigate("main")
```

### Migration Detection (SecurityManager init block)

```
SecurityManager.init:
  if (pref_pin_has_been_set == false AND storedPin != null AND storedPin != "")
      → storedPin != "123456"
          → set pref_pin_has_been_set = true (existing user with custom PIN)
      → storedPin == "123456"
          → flag stays false → user will be redirected to create_pin
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/.../data/SecurityManager.kt` | Modify | Remove `DEFAULT_PIN`. Add `PIN_HAS_BEEN_SET` DataStore key. Expose `pinHasBeenSet: Flow<Boolean>`. Migration: auto-set flag if custom PIN exists. `savePin` sets flag after write. `getStoredPin()` returns empty when flag is false. |
| `app/.../data/SessionManager.kt` | Modify | Add `PIN_HAS_BEEN_SET` to companion object keys (shared DataStore). Expose `pinHasBeenSet: Flow<Boolean>`. Add `setPinHasBeenSet()` and `hasBeenSetAtLeastOnce()` helper. |
| `app/.../viewmodel/AuthViewModel.kt` | Modify | Expose `pinHasBeenSet: StateFlow<Boolean>` from sessionManager. Thread it into nav guards. |
| `app/.../MainActivity.kt` | Modify | Add `create_pin` composable route. Add `pinHasBeenSet` to nav guard condition in `loading`. |
| `app/.../ui/screens/CreatePinScreen.kt` | Create | Compose screen: 6-digit PIN, confirmation field, validation (match, 6 digits, not weak patterns). Reuses numeric keypad style from PinScreen. Navigates to `main` on success. |
| `app/.../ui/screens/ConfiguracionScreen.kt` | Modify | `SecuritySection` receives `pinHasBeenSet: Boolean`. When false: hides "PIN actual" field, labels button "Crear PIN". Calls `savePin` (no old PIN validation). |
| `app/.../ui/screens/PinScreen.kt` | Modify | No functional changes. Title updated to clarify this is verification (not creation). |

## Interfaces / Contracts

```kotlin
// SecurityManager — new additions
val pinHasBeenSet: Flow<Boolean>  // derived from DataStore pref_pin_has_been_set

// Existing savePin signature — now sets flag after write:
suspend fun savePin(pin: String)  // sets pref_pin_has_been_set = true

// getStoredPin() — changed behavior:
// Returns "" when pref_pin_has_been_set == false
// Returns stored value when pref_pin_has_been_set == true
```

## Sequence Diagram: Launch Flow

```
User    MainActivity    AuthViewModel    SecurityManager    DataStore
 │          │                │                 │               │
 │  open    │                │                 │               │
 │─────────▶│                │                 │               │
 │          │ checkExisting  │                 │               │
 │          │───────────────▶│                 │               │
 │          │                │──get pinHasBeen │               │
 │          │                │────────────────▶│               │
 │          │                │                 │──read flag──▶│
 │          │                │                 │◀────false────│
 │          │                │◀──Flow(false)───│               │
 │          │                │                 │               │
 │          │ authChecked    │                 │               │
 │◀────────│ nav→create_pin  │                 │               │
 │          │                │                 │               │
 │ PIN:837291 confirm:837291 │                 │               │
 │─────────▶│ savePin(837291)│                 │               │
 │          │───────────────▶│────────────────▶│               │
 │          │                │  write ESP      │  write flag   │
 │          │                │◀──OK───────────│──────true────▶│
 │          │ nav→main       │                 │               │
 │◀────────│                │                 │               │
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `SecurityManager.getStoredPin()` returns "" when flag=false | Mock DataStore, assert empty |
| Unit | `SecurityManager.savePin` sets flag after write | Mock EncryptedSharedPrefs + DataStore, assert flag order |
| Unit | Weak PIN validation | Parametrized: `111111`, `123456`, `654321`, `abcdef` → rejected |
| Integration | Nav graph redirects `loading → create_pin` when `!pinHasBeenSet` | Instrumented test with mock SecurityManager |
| Integration | Migration: custom PIN existing in ESP auto-sets flag | Instrumented test: pre-seed EncryptedSharedPrefs, check flag after init |

## Open Questions

- [x] Should `pin_has_been_set` live in SecurityManager or SessionManager? → Resolved: SecurityManager owns it, SessionManager exposes it for AuthViewModel convenience (both use same DataStore).
- [ ] Expose `pinHasBeenSet` via SecurityManager directly in AuthViewModel, or via SessionManager proxy? → Design says SessionManager proxy (consistent with `isPinRequired` pattern). Implementer may thread directly if simpler.
