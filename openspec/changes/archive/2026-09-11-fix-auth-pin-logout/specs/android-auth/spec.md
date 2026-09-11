# Delta for android-auth

JD2-C8. Logout MUST wipe device PIN secret and pin-has-been-set so a later account cannot unlock with a prior PIN or treat PIN as set. Create PIN MUST persist successfully before leaving Create PIN. Optional PIN remains the product default (MUST NOT require PIN for all users). `SignOutScope.GLOBAL` and Activity `launchMode` remain unchanged.

## ADDED Requirements

### Requirement: Logout Clears Device PIN State

On logout, the app MUST clear the stored device PIN secret and MUST set pin-has-been-set to false. After logout, a subsequent account MUST NOT unlock with the prior account's PIN and MUST NOT treat PIN as already set from prior device state. PIN wipe MUST run as part of local logout cleanup regardless of remote sign-out success or failure, except cancellation. This change MUST NOT alter `SignOutScope.GLOBAL` logout semantics and MUST NOT change Activity `launchMode`.

#### Scenario: Logout empties PIN secret and has-been-set

- GIVEN an authenticated account with a stored PIN and pin-has-been-set true
- WHEN the user logs out
- THEN the stored PIN secret MUST be empty
- AND pin-has-been-set MUST be false

#### Scenario: Next account cannot unlock with prior PIN

- GIVEN account A set a PIN then logged out
- WHEN account B authenticates on the same device and PIN-required becomes true without B creating a new PIN
- THEN the app MUST treat PIN as unset
- AND MUST NOT accept account A's PIN for unlock

#### Scenario: PIN wipe despite remote sign-out failure

- GIVEN logout runs and remote sign-out fails with a non-cancellation error
- WHEN local logout cleanup completes
- THEN the stored PIN secret MUST still be cleared
- AND pin-has-been-set MUST be false

### Requirement: Create PIN Persists Before Navigate

The Create PIN flow MUST successfully persist the PIN before navigating away from Create PIN. The app MUST NOT navigate to Main until persist succeeds. On persist failure or invalid PIN, the app MUST remain on Create PIN.

#### Scenario: Success navigates only after persist

- GIVEN the user confirms a valid new PIN on Create PIN
- WHEN create PIN completes successfully
- THEN the PIN MUST be persisted
- AND pin-has-been-set MUST be true
- AND the app MAY then navigate to Main

#### Scenario: Failure stays on Create PIN

- GIVEN the user confirms a PIN on Create PIN
- WHEN create PIN fails or the PIN is invalid
- THEN the app MUST remain on Create PIN
- AND MUST NOT navigate to Main

## MODIFIED Requirements

### Requirement: Create PIN Only When Required And Unset

The app MUST navigate to Create PIN if and only if PIN is required and no PIN has been set. PIN-required MUST default to false. This spec MUST NOT require Create PIN for all users. This change MUST NOT adopt mandatory-PIN-for-all.

(Previously: Optional PIN routing unchanged in substance; wording now explicitly forbids reviving mandatory PIN as part of the logout/create-PIN fix.)

#### Scenario: Optional PIN skips create

- GIVEN PIN is not required
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST NOT navigate to Create PIN

#### Scenario: Required unset PIN creates

- GIVEN PIN is required AND no PIN has been set
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST navigate to Create PIN

#### Scenario: Required set PIN unlocks

- GIVEN PIN is required AND a PIN has been set
- WHEN an authenticated user reaches pin-or-main routing
- THEN the app MUST navigate to PIN unlock
- AND MUST NOT navigate to Create PIN
