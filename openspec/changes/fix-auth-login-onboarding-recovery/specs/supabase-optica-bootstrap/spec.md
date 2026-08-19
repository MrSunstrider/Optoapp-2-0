# Supabase Optica Bootstrap Specification

## Purpose

Harden `create_optica_for_current_user` so a known client id cannot join an existing óptica as admin, membership is created only when a new óptica row is inserted, the RPC returns the server id, and authenticated clients cannot INSERT `opticas` directly.

## Requirements

### Requirement: Client Optica Id Is Ignored

`create_optica_for_current_user` MUST ignore `p_optica_id` when assigning identity. The new `opticas.id` MUST be generated on the server.

#### Scenario: Known existing id does not join as admin

- GIVEN an óptica id that already exists
- AND an authenticated caller who is not a member of that óptica
- WHEN they call the RPC with that id as `p_optica_id`
- THEN the RPC MUST NOT insert `usuario_optica` for that existing óptica
- AND the caller MUST NOT become admin of it

#### Scenario: Client-supplied id is not used as the new row id

- GIVEN an authenticated caller allowed to create a first óptica
- WHEN they call the RPC with a non-empty client `p_optica_id` and a valid name
- THEN the inserted `opticas.id` MUST be server-generated
- AND MUST NOT equal the client-supplied `p_optica_id`

### Requirement: Admin Membership Only On Insert

The RPC MUST insert `usuario_optica` with rol `admin` only if the `opticas` INSERT created a new row. An insert that affects zero rows MUST NOT attach admin membership.

#### Scenario: Zero-row optica insert does not grant admin

- GIVEN an `opticas` INSERT that inserts zero rows
- WHEN the RPC continues
- THEN it MUST NOT insert `usuario_optica` admin for the caller on the targeted existing óptica

#### Scenario: New optica row grants admin

- GIVEN the `opticas` INSERT creates one row
- WHEN the RPC continues
- THEN it MUST insert `usuario_optica` for the caller as admin of that new id

### Requirement: RPC Returns Server Id

The RPC MUST return the server-generated óptica id. The Android client MUST persist that returned id as the session óptica, not the id it sent.

#### Scenario: Return value equals inserted id

- GIVEN a successful first-óptica create
- WHEN the RPC completes
- THEN the return value MUST equal the new `opticas.id`

#### Scenario: Client persists returned id

- GIVEN the RPC returns id S
- AND the client had generated id C
- WHEN the client saves session after create
- THEN the saved óptica id MUST be S
- AND MUST NOT be C unless S equals C

### Requirement: Authenticated Direct Opticas Insert Forbidden

Authenticated clients MUST NOT INSERT into `opticas` except through the SECURITY DEFINER RPC. Policy `opticas_insert_authenticated` MUST be absent after this change.

#### Scenario: Direct insert denied

- GIVEN an authenticated session not executing the RPC
- WHEN it attempts INSERT into `opticas`
- THEN the statement MUST fail due to RLS
- AND no new `opticas` row MUST remain

#### Scenario: RPC insert still succeeds

- GIVEN the SECURITY DEFINER RPC
- WHEN it inserts a new óptica
- THEN the insert MUST succeed without policy `opticas_insert_authenticated`

### Requirement: RPC Does Not Encode Membership Cap

The RPC MUST NOT encode a `max_opticas` cap in its body. Existing `trg_opticas_limit_guard` MAY still reject inserts; this change MUST NOT drop that trigger.

#### Scenario: Function body has no max-opticas raise

- GIVEN the replaced `create_optica_for_current_user` body
- WHEN it is inspected or executed for a first óptica
- THEN it MUST NOT raise a dedicated max-opticas error of its own

#### Scenario: Limit trigger remains

- GIVEN `trg_opticas_limit_guard` exists before this change
- WHEN the new migration is applied
- THEN that trigger MUST still exist
