# pagos-constraints Specification

## Purpose

Prevent silent data corruption in the financial pipeline by enforcing domain-level CHECK constraints on `pagos.tipo` and `pagos.metodo_pago`. These constraints protect against regressions where invalid enum values enter the payments table, which would silently break financial aggregations downstream.

## Requirements

### R1: CHECK Constraint on `pagos.tipo`

The system SHALL add `CONSTRAINT chk_pagos_tipo CHECK (tipo IN ('Pago', 'Cuota', 'Anulación', 'Ajuste'))` to `public.pagos`.

#### Scenario: Valid tipo values are accepted

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `tipo` to `'Pago'`
- THEN the operation succeeds
- AND the same holds for `'Cuota'`, `'Anulación'`, and `'Ajuste'`

#### Scenario: Invalid tipo is rejected

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `tipo` to `'Descuento'` (not in the allowed set)
- THEN the operation fails with a CHECK constraint violation
- AND the error message mentions `chk_pagos_tipo`

### R2: CHECK Constraint on `pagos.metodo_pago`

The system SHALL add `CONSTRAINT chk_pagos_metodo CHECK (metodo_pago IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'CtaCorriente'))` to `public.pagos`.

#### Scenario: Valid metodo_pago values are accepted

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `metodo_pago` to `'Efectivo'`
- THEN the operation succeeds
- AND the same holds for `'Tarjeta'`, `'Transferencia'`, `'Yape'`, `'Plin'`, and `'CtaCorriente'`

#### Scenario: Invalid metodo_pago is rejected

- GIVEN the constraint exists on `public.pagos`
- WHEN an INSERT or UPDATE sets `metodo_pago` to `'Credito'` (not in the allowed set)
- THEN the operation fails with a CHECK constraint violation
- AND the error message mentions `chk_pagos_metodo`

### R3: Constraint Idempotency

The migration SHALL use `ALTER TABLE ... ADD CONSTRAINT ... IF NOT EXISTS` syntax, or wrap the DDL in a `DO $$` block that checks for pre-existing constraints, to ensure re-running the migration does not error.

#### Scenario: Re-running migration does not error

- GIVEN both constraints already exist on `public.pagos`
- WHEN the migration is applied again
- THEN the migration completes without error
- AND the constraints remain unchanged

### R4: Existing Data Validation

After adding the constraints, the migration SHOULD validate existing rows. If any existing row violates `chk_pagos_tipo` or `chk_pagos_metodo`, the migration SHALL report the violating rows but SHALL NOT block the constraint addition (`NOT VALID` or equivalent pattern).

#### Scenario: Existing invalid rows are reported, not blocked

- GIVEN a pre-existing pago with `tipo = 'Descuento'` (invalid)
- WHEN the migration adds the CHECK constraint
- THEN the constraint is created successfully (existing data is not validated immediately with `NOT VALID`)
- AND invalid rows are identified in migration output for manual remediation
