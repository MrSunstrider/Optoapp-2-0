# Tasks: Fix `recalcular_resumen_diario` — SECURITY DEFINER + Anulado filter

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~90 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Failing tests against current function

- [x] 1.1 Write SQL test (as `authenticated`, not `service_role`): call `SELECT recalcular_resumen_diario('test-optica', CURRENT_DATE)` → verify `resumen_diario` has **no new row** (RLS silently blocks INSERT)
- [x] 1.2 Write SQL test: query `daily_ventas` logic → verify anulado dispensaciones/servicios_extra **ARE included** in current totals (no filter)

## Phase 2: GREEN — Write migration

- [x] 2.1 `supabase migration new fix_recalcular_resumen_diario_security` to create timestamped file
- [x] 2.2 Write `CREATE OR REPLACE FUNCTION` preserving full body from `20260714000000` with 4 changes: (a) `SECURITY INVOKER` → `SECURITY DEFINER`, (b) `SET search_path = public` → `SET search_path = ''`, (c) add `AND estado IS DISTINCT FROM 'Anulado'` to dispensaciones and servicios_extra WHERE clauses, (d) append `ALTER FUNCTION ... OWNER TO postgres` + `GRANT EXECUTE TO authenticated, service_role`

## Phase 3: VERIFY — All tests pass

- [x] 3.1 `supabase db reset` — all 196 migrations replay without error
- [x] 3.2 Re-run 1.1: authenticated call now succeeds → `resumen_diario` has 1 row with non-null values across all columns
- [x] 3.3 Re-run 1.2: anulado records are excluded from `ventas_cantidad`, `ventas_monto_total`, `ventas_costo_total`
- [x] 3.4 Verify function signature is unchanged `(text, date) → void` — existing Android callers unaffected
