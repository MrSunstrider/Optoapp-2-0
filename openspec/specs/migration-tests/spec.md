# Migration Tests Specification

## Purpose

Define schema integrity validation that runs after `supabase db reset` in CI, verifying that expected tables, columns, RLS policies, and functions exist and match their specifications.

## Requirements

### Requirement: Schema Invariant Checks

After `supabase db reset`, CI MUST run a suite of SQL-based schema assertions. The suite SHALL verify that every core table exists, every expected column has the correct type, every RLS policy is enabled, and every expected function is defined. The `ventas` table SHALL NOT be listed as an expected core table (it was dropped in the July 10 migration). The function `rpc_resumen_financiero` SHALL NOT be listed as an expected function (it is deprecated but not required).

#### Scenario: All core tables exist after reset

- GIVEN a fresh `supabase db reset` on the latest migrations
- WHEN the schema test suite runs a `SELECT` against `information_schema.tables`
- THEN every table listed in the test manifest SHALL be present

#### Scenario: Missing table fails the suite

- GIVEN a migration that removes a core table
- WHEN the schema test suite checks for its existence
- THEN the test SHALL fail AND CI SHALL block the push

#### Scenario: ventas table absent after db reset

- GIVEN a fresh `supabase db reset` on the latest migrations
- WHEN the schema test queries `information_schema.tables` for `'ventas'`
- THEN zero rows are returned (table does not exist)

#### Scenario: rpc_resumen_financiero not required

- GIVEN the schema test suite queries `information_schema.routines` for `rpc_resumen_financiero`
- WHEN a match is found or not found
- THEN the test SHALL NOT fail (the function is neither required nor forbidden)

### Requirement: RLS Policy Validation

Every table that requires Row-Level Security MUST have at least one RLS policy defined AND policies MUST reference the `optica_id` column for multi-tenant isolation.

#### Scenario: RLS policy present on protected table

- GIVEN a table that requires multi-tenant isolation
- WHEN the test suite queries `pg_policies`
- THEN at least one policy is found referencing `optica_id`

#### Scenario: Table missing RLS fails

- GIVEN a table requiring RLS but with no policies defined
- WHEN the test suite checks `pg_policies`
- THEN the test SHALL fail with a message identifying the unprotected table

### Requirement: Test Suite Execution

The schema test suite SHOULD be written as plain SQL files under `supabase/tests/`. If pgTAP is feasible, the project MAY adopt it. CI MUST exit non-zero if any test assertion fails.

#### Scenario: All assertions pass

- GIVEN schema tests matching the current schema
- WHEN CI runs `supabase db reset` followed by the test suite
- THEN all tests pass AND CI proceeds

#### Scenario: Assertion failure blocks CI

- GIVEN a schema test expects a column type that no longer matches
- WHEN the test suite runs
- THEN the assertion fails AND CI exits non-zero

### Requirement: Financial Pipeline Integration Test

The system SHALL include a new SQL test file `supabase/tests/test_financial_pipeline.sql` that asserts end-to-end correctness of the financial pipeline.

The test SHALL:

1. Call `recalcular_resumen_diario('test_optica', test_date)` AFTER the function has been fixed.
2. Assert `resumen_diario.ventas_monto_total` equals `SUM(dispensaciones.monto_total) + SUM(servicios_extra.monto_total)` for the same `(optica_id, fecha)` within 0.01 tolerance.
3. Assert `cobros_monto_total` equals `SUM(pagos.monto)` filtering out `'Anulación'` rows.
4. Assert `rpc_analisis_mensual('test_optica', test_date)` JSON response contains all 16 keys from the restored rich version.

#### Scenario: Financial pipeline output matches transactional source

- GIVEN test data with known dispensaciones, servicios_extra, and pagos for `('test_optica', '2026-07-01')`
- WHEN `recalcular_resumen_diario('test_optica', '2026-07-01')` is called
- THEN `resumen_diario.ventas_monto_total` = `SUM(dispensaciones.monto_total) + SUM(servicios_extra.monto_total)`
- AND `resumen_diario.cobros_monto_total` matches `SUM(pagos.monto)` excluding Anulación rows
- AND `rpc_analisis_mensual('test_optica', '2026-07-01')` contains all 16 top-level keys

#### Scenario: CI enforces test pass before deployment

- GIVEN a push touching `supabase/migrations/` or `supabase/tests/`
- WHEN CI runs the test suite
- THEN `test_financial_pipeline.sql` executes as part of the suite
- AND CI exits non-zero if the financial pipeline assertions fail
