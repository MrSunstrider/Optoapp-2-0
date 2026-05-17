# Delta for Android Auth

## ADDED Requirements

### Requirement: PIN Setup State in Auth Flow

The system MUST expose `pinHasBeenSet: Flow<Boolean>` as part of the authentication state. `AuthViewModel` SHALL combine `isLoggedIn`, `isPinRequired`, and `pinHasBeenSet` to determine the correct navigation target. The nav graph in `MainActivity` MUST route to `create_pin` when `isLoggedIn && !pinHasBeenSet`, regardless of `isPinRequired`.

#### Scenario: Auth state — PIN not set, logged in

- GIVEN `AuthViewModel` emits state with `isLoggedIn == true` and `pinHasBeenSet == false`
- WHEN `MainActivity` observes the auth state
- THEN the navigation target is `create_pin`

#### Scenario: Auth state — PIN set, PIN required

- GIVEN `AuthViewModel` emits state with `isLoggedIn == true`, `pinHasBeenSet == true`, and `isPinRequired == true`
- WHEN `MainActivity` observes the auth state
- THEN the navigation target is `pin` (verification screen)

#### Scenario: Auth state — PIN set, PIN not required

- GIVEN `AuthViewModel` emits state with `isLoggedIn == true`, `pinHasBeenSet == true`, and `isPinRequired == false`
- WHEN `MainActivity` observes the auth state
- THEN the navigation target is `main`

## MODIFIED Requirements

### Requirement: SecurityManager PIN Storage

SecurityManager MUST NOT contain a hardcoded `DEFAULT_PIN` constant or any fallback to `"123456"`. The `getStoredPin()` method SHALL return empty when `pin_has_been_set` is `false`. The `savePin()` method SHALL set `pin_has_been_set = true` in DataStore after successful persistence to EncryptedSharedPreferences.

(Previously: `DEFAULT_PIN = "123456"` existed as a fallback when no PIN was stored)

#### Scenario: No PIN configured — returns empty

- GIVEN `pin_has_been_set == false`
- WHEN `SecurityManager.getStoredPin()` is called
- THEN it returns an empty string (not `"123456"`)

#### Scenario: PIN configured — returns stored value

- GIVEN `pin_has_been_set == true` and a PIN exists in EncryptedSharedPreferences
- WHEN `SecurityManager.getStoredPin()` is called
- THEN it returns the stored PIN value

#### Scenario: Verify PIN — no PIN set

- GIVEN `pin_has_been_set == false`
- WHEN `SecurityManager.verifyPin("123456")` is called
- THEN it returns `false` (default PIN is no longer accepted)

## REMOVED Requirements

### Requirement: Default PIN Fallback

(Reason: The hardcoded `DEFAULT_PIN = "123456"` is being eliminated entirely. No fallback behavior should exist — an unset PIN means the user must create one through `CreatePinScreen`.)
