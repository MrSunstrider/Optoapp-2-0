# Android PIN Setup Specification

## Purpose

Defines the mandatory PIN creation flow for first-time users and migration path for existing users with the hardcoded default PIN. Eliminates `DEFAULT_PIN = "123456"` from the codebase.

## Requirements

### Requirement: PIN Has Been Set Flag

The system MUST track whether a user has explicitly configured a local PIN at least once. A DataStore boolean `pin_has_been_set` (default `false`) SHALL serve as the single source of truth. The flag MUST be set to `true` only after a PIN is successfully persisted to EncryptedSharedPreferences. The flag MUST NOT be set before the PIN write succeeds.

#### Scenario: Fresh install — flag is false by default

- GIVEN a user installs the app for the first time
- WHEN the app reads the `pin_has_been_set` flag
- THEN the value is `false`

#### Scenario: PIN saved — flag transitions to true

- GIVEN `pin_has_been_set` is `false`
- WHEN the user successfully creates and saves a PIN via `SecurityManager.savePin`
- THEN the PIN is written to EncryptedSharedPreferences first
- AND `pin_has_been_set` is set to `true` in DataStore

#### Scenario: Save fails — flag remains false

- GIVEN `pin_has_been_set` is `false`
- WHEN `SecurityManager.savePin` encounters an error during PIN persistence
- THEN `pin_has_been_set` remains `false`

### Requirement: Mandatory PIN Creation on First Use

The system MUST force a new user to create a PIN before accessing any protected screen. The `CreatePinScreen` SHALL be shown when `isLoggedIn == true` AND `pin_has_been_set == false`. Navigation to `main` SHALL be blocked until PIN creation completes.

#### Scenario: Fresh install — redirected to CreatePinScreen

- GIVEN a fresh install with `isLoggedIn == true` and `pin_has_been_set == false`
- WHEN the app resolves the initial navigation route
- THEN the user is navigated to `create_pin`
- AND navigation to `main` is blocked

#### Scenario: Existing user with custom PIN — no redirection

- GIVEN `isLoggedIn == true` and `pin_has_been_set == true`
- WHEN the app resolves the initial navigation route
- THEN the user proceeds to the normal PIN verification screen or main screen based on `isPinRequired`

### Requirement: PIN Creation Validation

The system MUST validate the PIN during creation. The PIN MUST be exactly 6 numeric digits. The system MUST require confirmation — the user enters the PIN twice and both entries MUST match. The system MUST NOT accept weak patterns (all same digits like `000000`, sequential like `123456` or `654321`).

#### Scenario: Valid PIN with matching confirmation — success

- GIVEN the user is on `CreatePinScreen`
- WHEN they enter `837291` in both fields and confirm
- THEN the PIN is saved and `pin_has_been_set` becomes `true`
- AND the user is navigated to `main`

#### Scenario: PINs do not match — validation error

- GIVEN the user is on `CreatePinScreen`
- WHEN they enter `837291` in the first field and `837292` in the second
- THEN a "PINs no coinciden" error is displayed
- AND the PIN is NOT saved
- AND both fields are cleared for retry

#### Scenario: Weak PIN pattern — validation error

- GIVEN the user is on `CreatePinScreen`
- WHEN they enter `111111` or `123456` or `654321` in both fields
- THEN a "PIN demasiado fácil — elige otro" error is displayed
- AND the PIN is NOT saved

#### Scenario: Non-numeric or wrong length — validation error

- GIVEN the user is on `CreatePinScreen`
- WHEN they enter fewer or more than 6 digits, or non-numeric characters
- THEN the input is rejected or an error is displayed
- AND the PIN is NOT saved

### Requirement: Migration for Existing Users with Default PIN

The system MUST detect users who never changed from the hardcoded default PIN (`"123456"`) and force them through the `CreatePinScreen` flow. When `pin_has_been_set == false` AND the stored value equals `"123456"`, the system MUST treat the user as unset and redirect to `create_pin`.

#### Scenario: Existing user with default PIN — forced setup

- GIVEN a user upgrading from a version with `DEFAULT_PIN`
- AND `pin_has_been_set == false` AND stored PIN == `"123456"`
- WHEN the app launches
- THEN the user is redirected to `create_pin`
- AND the old `"123456"` value is cleared from EncryptedSharedPreferences after new PIN is saved

#### Scenario: Existing user with custom PIN — no change

- GIVEN a user who previously set a custom PIN (stored value != `"123456"`)
- WHEN the app launches
- THEN the user proceeds to normal PIN verification
- AND `pin_has_been_set` is treated as `true`

### Requirement: Settings Screen — Create vs Change Mode

The system MUST adapt the Settings security section based on `pin_has_been_set`. When the flag is `false`, the screen SHALL show "Crear PIN" without requiring a current PIN. When the flag is `true`, the screen SHALL show "Cambiar PIN" requiring the current PIN for verification.

#### Scenario: Settings — create mode (flag false)

- GIVEN `pin_has_been_set == false`
- WHEN the user opens Settings > Security
- THEN the action label is "Crear PIN"
- AND no "PIN actual" field is shown

#### Scenario: Settings — change mode (flag true)

- GIVEN `pin_has_been_set == true`
- WHEN the user opens Settings > Security
- THEN the action label is "Cambiar PIN"
- AND a "PIN actual" field is required before setting a new PIN
