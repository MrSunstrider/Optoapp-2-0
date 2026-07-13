# Tasks: Fix Financial Pipeline Regression

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (all supabase/ — tightly coupled tests + migrations) |
| Delivery strategy | ask-on-risk |
| Chain strategy | size-exception |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Phase 1: RED — Failing Tests First (TDD)

- [x] 1.1 Create `supabase/tests/test_financial_pipeline_consistency.sql` — DO block with 3 assertions (recalcular sums match transactional data, rpc_analisis_mensual returns 16 JSON keys, empty month returns zeros). Must fail before W1/W2a applied.
- [x] 1.2 Modify `supabase/tests/test_schema_integrity.sql` — remove `('ventas'),` from expected tables (L27), remove `'rpc_resumen_financiero'` from expected functions (L224).
- [x] 1.3 Rewrite `supabase/tests/test_cost_recalculation.sql` — replace `ventas` table with UNION ALL of `dispensaciones` + `servicios_extra`, insert test data directly into source tables, assert `ventas_costo_total` matches `costo_real_*` sum + fallback 0 (not snapshot — column absent on source tables). Expected cost: 105.0 (not 120.0).

## Phase 2: GREEN — Fix Migrations

- [x] 2.1 Create `supabase/migrations/20260714000000_fix_recalcular_resumen_diario.sql` — restore with UNION ALL `dispensaciones` + `servicios_extra`. Cost via `SUM(COALESCE(costo_real_*,0))` from `dispensacion_items` with fallback 0 (costo_unitario_snapshot does NOT exist on source tables). Pagos dedup, inventory, idempotent upsert unchanged. SECURITY INVOKER + GRANT TO authenticated/service_role.
- [x] 2.2 Create `supabase/migrations/20260714000001_fix_rpc_analisis_mensual.sql` — restore 16-field version merging Jul 10 union-all fields with Jul 13 `meses_historicos`. `margen_por_categoria` via LEFT JOIN, `deudores` sub-calls `rpc_deudores()`, `proyeccion_caja` via UNION ALL. No `ventas` references.
- [x] 2.3 Create `supabase/migrations/20260714000002_drop_rpc_saldo_pendiente.sql` — `DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT)`.

## Phase 3: Data Recovery + Domain Constraints

- [x] 3.1 Create `supabase/migrations/20260714000003_regenerate_resumen_diario.sql` — DO $$ loop over UNION of distinct `(optica_id, fecha)` from `dispensaciones` and `servicios_extra`, call `recalcular_resumen_diario` for each.
- [x] 3.2 Create `supabase/migrations/20260714000004_add_pagos_domain_constraints.sql` — NOT VALID CHECK on `pagos.tipo IN ('Abono','Pago completo','Reembolso','Reverso','Anulación')` and `pagos.metodo_pago IN ('Efectivo','Tarjeta','Transferencia','Yape','Plin','Móvil')` (production reality values, per technical flags). Report violating rows before DDL. Idempotent via DO block guard.

## Verification

| # | Command / Check | Expected Result |
|---|-----------------|-----------------|
| V0 | `supabase db reset` (local Docker) — todas las migrations aplican sin error | Build exit code 0, DB lista |
| V1 | `supabase db reset` + run all tests (`test_financial_pipeline_consistency.sql`) | W0 must pass (all 3 assertions) |
| V2 | `test_schema_integrity.sql` | No errors about `ventas` or `rpc_resumen_financiero` |
| V3 | `test_cost_recalculation.sql` | ventas_costo_total matches expected sum |
| V4 | `DROP FUNCTION rpc_saldo_pendiente` | Zero rows in `information_schema.routines` for `rpc_saldo_pendiente` |
| V5 | Manual: SELECT SUM(ventas_monto_total) vs. UNION ALL source | Gap < 0.01% after W3 regeneration |
| V6 | `supabase db diff` revisado manualmente antes de `supabase db push` | Solo los DDL esperados (5 funciones + 2 constraints), sin DROPs inesperados |

**Gate**: V0 y V1 deben pasar en local antes de autorizar `supabase db push` a remoto. V6 es la última revisión pre-push.
