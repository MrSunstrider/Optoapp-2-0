# Delta for pagos-constraints

## MODIFIED Requirements

### Requirement: CHECK Constraint on `pagos.tipo`

The system SHALL enforce `CONSTRAINT chk_pagos_tipo CHECK (tipo IN ('Abono', 'Pago completo', 'Reembolso', 'Reverso', 'Anulación'))` on `public.pagos`. `Anulación` remains allowed for legacy rows only; application writers MUST NOT insert new Anulación values.
(Previously: Allowed set was incorrectly documented as `Pago`, `Cuota`, `Anulación`, `Ajuste`.)

#### Scenario: Valid tipo values are accepted

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `tipo` to `'Abono'`
- THEN the operation succeeds
- AND the same holds for `'Pago completo'`, `'Reembolso'`, `'Reverso'`, and `'Anulación'`

#### Scenario: Invalid tipo is rejected

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `tipo` to `'Descuento'` (not in the allowed set)
- THEN the operation fails with a CHECK constraint violation
- AND the error message mentions `chk_pagos_tipo`

#### Scenario: Negative control — blank tipo rejected

- GIVEN `pagos_tipo_not_blank_chk` (or equivalent) remains enforced
- WHEN `tipo` is blank or whitespace
- THEN the write MUST fail

## ADDED Requirements

### Requirement: Keep Non-Negative `pagos_monto_chk`

The system MUST keep `pagos_monto_chk` requiring `monto >= 0`. Migrations for this change MUST NOT drop or weaken that constraint.

#### Scenario: Zero monto allowed for legacy Anulación only where already present

- GIVEN `monto = 0` and a valid `tipo`
- WHEN INSERT/UPDATE runs
- THEN `pagos_monto_chk` MUST allow the row
- AND cash effect for Abono/Pago completo/Reembolso/Reverso of zero MUST be zero

#### Scenario: Negative control — negative monto still blocked after rollout

- GIVEN DB and app after this change
- WHEN any client attempts `monto = -1`
- THEN the write MUST fail with CHECK or local validation
- AND rollback MUST leave no partially written negative magnitude

### Requirement: `reversa_pago_id` Uniqueness Per Original

The system MUST enforce that at most one pago references a given original via `reversa_pago_id` (unique when not null), scoped such that RLS/`optica_id` isolation still applies to visible rows.

#### Scenario: Second reverse link rejected

- GIVEN pago `R1` already has `reversa_pago_id = P1`
- WHEN another pago attempts `reversa_pago_id = P1`
- THEN the write MUST fail uniqueness
- AND `P1` MUST retain exactly one reverse link

#### Scenario: Multi-tenant — distinct optics may not cross-link originals

- GIVEN original pago `P1` belongs to optica A
- WHEN a pago for optica B attempts to set `reversa_pago_id = P1`
- THEN the write MUST be rejected by FK/RLS/domain rules
- AND optica A ledger MUST remain unchanged

### Requirement: Rollback-Safe Constraint Expansion

CHECK expansions for related sale-state domains (referenced by sync writers) MUST be additive and reversible only if unused; otherwise expanded CHECKs MUST remain and writers MUST be feature-flagged or reverted independently. Negative montos MUST NEVER be re-allowed on rollback.

#### Scenario: DB rollback refuses negative montos

- GIVEN expanded CHECKs are in production and clients depend on them
- WHEN a rollback of a later migration is considered
- THEN `pagos_monto_chk` MUST remain `monto >= 0`
- AND any rollback path MUST NOT restore negative-monto acceptance
