# Delta for schema-integrity

## MODIFIED Requirements

### Requirement: Schema Invariant Checks

MODIFIED: Remove `ventas` from the list of expected core tables. Remove `rpc_resumen_financiero` from the list of expected functions. Add `test_financial_pipeline.sql` to the test suite.

The test suite SHALL assert `ventas` table no longer exists (verifying the July 10 `DROP TABLE public.ventas CASCADE` is in effect). The test suite SHALL NOT assert `rpc_resumen_financiero` exists (may be present as deprecated, but is no longer a required function).

(Previously: `ventas` was listed as an expected core table; `rpc_resumen_financiero` was listed as an expected function.)

#### Scenario: ventas table absent after db reset

- GIVEN a fresh `supabase db reset` on the latest migrations
- WHEN the schema test queries `information_schema.tables` for `'ventas'`
- THEN zero rows are returned (table does not exist)

#### Scenario: rpc_resumen_financiero not required

- GIVEN the schema test suite queries `information_schema.routines` for `rpc_resumen_financiero`
- WHEN a match is found or not found
- THEN the test SHALL NOT fail (the function is neither required nor forbidden)

## ADDED Requirements

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
