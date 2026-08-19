# Android Auth Onboarding Specification

## Purpose

First-óptica owner create, employee wait without logout, fail-closed membership fetch and role mapping, and skip-selector when exactly one óptica exists. Employee join is existing email plus `assign_optica_role_by_email`.

**A10 note:** Postgres table `invitaciones` exists and remains unused. This spec MUST NOT add invite-code product requirements. Android MUST NOT read or write `invitaciones`.

## Requirements

### Requirement: Owner First Optica Create

An authenticated user with no memberships MUST create their first óptica through an owner form (name and fiscal fields). The create path MUST keep the GoTrue session and MUST persist the server-returned óptica id, not a client-generated id.

#### Scenario: Owner create from empty memberships

- GIVEN an authenticated user with zero memberships and a valid GoTrue session
- WHEN they submit a valid owner form
- THEN the app MUST create the óptica and persist the returned id as the session óptica
- AND the user MUST reach pin-or-main without being signed out

#### Scenario: Owner action opens create form

- GIVEN an authenticated user with zero memberships
- WHEN they choose to create their óptica
- THEN the app MUST present the owner create form
- AND MUST NOT treat the empty membership list as a completed óptica selection

### Requirement: Empty Memberships Keep Session

When membership fetch succeeds with zero rows, the app MUST keep the GoTrue session and MUST mark onboarding needed. The app MUST NOT clear the session solely because memberships are empty.

#### Scenario: Zero memberships stay signed in

- GIVEN a successful membership fetch that returns an empty list
- WHEN post-login routing runs
- THEN the GoTrue session MUST remain
- AND the user MUST reach the no-óptica wait-or-create surface

#### Scenario: Employee wait does not logout

- GIVEN an authenticated user with zero memberships waiting to be assigned
- WHEN they remain in the app after login
- THEN they MUST stay signed in
- AND they MUST NOT be sent to Login only because memberships are empty

### Requirement: Membership Fetch Distinguishes Error From Empty

Membership fetch MUST expose distinct outcomes: success with a list, success empty, and fetch error. A fetch error MUST NOT be treated as empty memberships.

#### Scenario: Network error is not onboarding

- GIVEN an authenticated session
- WHEN membership fetch fails with a network or IO error
- THEN the app MUST surface an error state
- AND MUST NOT route to the no-óptica surface as if memberships were empty
- AND MUST NOT clear the session

#### Scenario: Empty list is onboarding not error

- GIVEN an authenticated session
- WHEN membership fetch succeeds with zero rows
- THEN the outcome MUST be empty, not error
- AND routing MUST keep the session and show the no-óptica surface

### Requirement: Blank Role Fail Closed

A membership row with blank or missing rol MUST NOT be treated as admin. The app SHALL skip the row or reject it.

#### Scenario: Blank rol is not admin

- GIVEN a membership row whose rol is blank
- WHEN memberships are mapped into session
- THEN that row MUST NOT receive rol `admin`
- AND the row MUST be skipped or rejected

#### Scenario: Valid rol is preserved

- GIVEN a membership row with rol `empleado`
- WHEN memberships are mapped
- THEN the session membership MUST keep rol `empleado`

### Requirement: Single Membership Skips Selector

When post-login memberships contain exactly one usable row, the app MUST persist that óptica session and MUST NOT open the óptica selector.

#### Scenario: Size one goes to pin or main

- GIVEN a successful fetch with exactly one usable membership
- WHEN post-login routing runs
- THEN the app MUST persist that óptica session
- AND MUST route to pin or main
- AND MUST NOT open the óptica selector

#### Scenario: Multiple memberships still select

- GIVEN a successful fetch with two or more usable memberships
- WHEN post-login routing runs
- THEN the app MUST present the óptica selector

### Requirement: Employee Join Is Assign By Email

Employee membership MUST be created by assigning an existing user email (`assign_optica_role_by_email`). Android MUST NOT query, insert, or update `invitaciones`.

#### Scenario: No invite-code collection

- GIVEN an authenticated user with no memberships
- WHEN they wait for access
- THEN the app MUST NOT require or collect an invite code

#### Scenario: Invitaciones table unused by Android

- GIVEN the Postgres `invitaciones` table exists
- WHEN Android auth, onboarding, or membership code runs
- THEN it MUST NOT read or write `invitaciones`
