# Android Auth Specification

## Purpose

Cold-start session restore after auth check, Google sign-in cancel fail-closed, and optional PIN. Empty PIN is invalid. Create PIN runs only when PIN is required and unset. This spec MUST NOT adopt mandatory-PIN-for-all (do not merge C3).

## Requirements

### Requirement: Cold Start Restores Authenticated Route

After session check completes, a valid existing session MUST leave Login. The app SHALL restore PIN unlock or main according to whether PIN is required and set. Routing MUST wait until auth check completes.

#### Scenario: Valid session leaves Login

- GIVEN a cold start with a valid stored session
- WHEN auth check completes successfully
- THEN the app MUST NOT remain on Login
- AND MUST navigate to PIN unlock or main as applicable

#### Scenario: No session stays on Login

- GIVEN a cold start with no session
- WHEN auth check completes
- THEN the app MUST remain on or return to Login

#### Scenario: Incomplete check does not restore main

- GIVEN a cold start before auth check completes
- WHEN navigation is first evaluated
- THEN the app MUST NOT treat the user as authenticated
- AND MUST wait until auth check completes before restoring PIN or main

### Requirement: Google Cancel Leaves Idle

Google sign-in MUST NOT remain in Loading when the user cancels or the flow completes without a session.

#### Scenario: User cancel is Idle or Error

- GIVEN Google sign-in is in Loading
- WHEN the user cancels the provider UI
- THEN auth state MUST become Idle or Error
- AND MUST NOT remain Loading

#### Scenario: Complete without session is not Loading

- GIVEN Google sign-in was started
- WHEN the flow finishes without an authenticated session
- THEN auth state MUST become Idle or Error
- AND MUST NOT remain Loading

### Requirement: Empty PIN Is Invalid

PIN validation MUST return false when the entered PIN is empty or when no PIN has been set. Two empty strings MUST NOT count as a match.

#### Scenario: Both empty is invalid

- GIVEN no PIN has been set
- AND the user enters an empty PIN
- WHEN PIN validation runs
- THEN the result MUST be false

#### Scenario: Unset stored PIN never matches input

- GIVEN no PIN has been set
- AND the user enters any non-empty PIN
- WHEN PIN validation runs
- THEN the result MUST be false

### Requirement: Create PIN Only When Required And Unset

The app MUST navigate to Create PIN if and only if PIN is required and no PIN has been set. PIN-required MUST default to false. This spec MUST NOT require Create PIN for all users.

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
