# Supabase User Profiles RLS Specification

## Purpose

Restrict `user_profiles` SELECT so callers cannot read emails of users outside their óptica. Admin or gerente privilege alone MUST NOT grant global SELECT.

## Requirements

### Requirement: Caller Can Select Own Profile

An authenticated user MUST be able to SELECT their own `user_profiles` row and MUST NOT require admin or gerente rol to do so.

#### Scenario: Own row visible

- GIVEN an authenticated user with a profile row
- WHEN they SELECT `user_profiles` without extra filters
- THEN their own row MUST be returned

#### Scenario: Unauthenticated select denied

- GIVEN no authenticated session
- WHEN a SELECT is issued against `user_profiles`
- THEN no profile rows MUST be returned

### Requirement: Peer Select Requires Shared Optica

An admin or gerente MUST SELECT another user's profile only when they share an `optica_id` via `usuario_optica`. Role without a shared óptica MUST NOT reveal the row.

#### Scenario: Same-optica privileged peer visible

- GIVEN the caller is admin or gerente of óptica X
- AND the target user is a member of óptica X
- WHEN the caller SELECTs `user_profiles`
- THEN the target user's row MUST be returned

#### Scenario: Other-optica privileged caller hidden

- GIVEN the caller is admin of óptica X only
- AND the target user belongs only to óptica Y
- WHEN the caller SELECTs `user_profiles`
- THEN the target row MUST NOT be returned

#### Scenario: Employee sees only own row

- GIVEN the caller has rol `empleado` in óptica X
- AND other members exist in óptica X
- WHEN the caller SELECTs `user_profiles`
- THEN they MUST receive their own row
- AND MUST NOT receive other members' emails
