# Design: Fix Financial Pipeline Regression

## Technical Approach

Six ordered work units (W0–W4) restoring two broken RPCs, dropping one dead function, regenerating stale data, and adding domain constraints — all without recreating the `ventas` table. Each migration is independent and idempotent. A TDD-first integration test must pass before any migration is applied.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice |
|----------|---------|----------|--------|
| Data source for "ventas" aggregation | Recreate `ventas` table vs. `UNION ALL dispensaciones + servicios_extra` | Recreating adds trigger maintenance and FK sync failures (proven Jul 5-10) | **UNION ALL** — proven working on Jul 10, simpler, source-of-truth |
| Cost source for dispensaciones | `ventas.costo_unitario_snapshot` (JOIN chain) vs. `dispensacion_items.costo_real_*` | Snapshot is stale after item edit; real-cost is accurate but requires subquery | **`costo_real_*` SUM with COALESCE fallback** to snapshot, then 0 |
| Saldo pendiente matching key | FK `pagos.venta_id → ventas.id` vs. namespace prefix matching | FK broke async sync before; namespace is convention-only but works | **Namespace prefixes** (`v_disp_`, `v_serv_`) — status quo, proven correct |
| meses_historicos field | Drop vs. preserve from Jul 13 | Loss of useful metric; preserving it in the Jul 10 base keeps full data | **Preserve** — COUNT(DISTINCT DATE_TRUNC) from `resumen_diario` |
| CHECK constraint validation | `VALIDATE` immediately vs. `NOT VALID` + report | Validating blocks migration on dirty data; NOT VALID allows progressive cleanup | **NOT VALID** — R4 spec requires non-blocking addition |
| Legacy test fix strategy | Remove vs. rewrite | Removing loses coverage; rewriting to UNION ALL keeps the assertion intent | **Rewrite** — `test_cost_recalculation.sql` uses UNION ALL CTE; `test_schema_integrity.sql` drops `ventas` + `rpc_resumen_financiero` from expected lists |

## Work Unit Design

### W0 — Integration Test (`supabase/tests/test_financial_pipeline_consistency.sql`)

DO block with three assertions:
1. **Happy path**: INSERT test dispensación (S/ 500 + items with costo_real_*), test servicio_extra (S/ 200, no items), test pago (S/ 100, tipo 'Pago'). Call `recalcular_resumen_diario('test_o', d)`. ASSERT `resumen_diario.ventas_monto_total = 700.00`, `cobros_monto_total = 100.00`, `ventas_costo_total = SUM(costo_real_* from items) + 0`.
2. **rpc_analisis_mensual keys**: Call `rpc_analisis_mensual('test_o', d)`. ASSERT `jsonb_object_keys(result)` contains all 16 field names (incl. restored `margen_por_categoria`, `deudores`, `proyeccion_caja`, `stock_estancado`, `valor_inventario`, and preserved `meses_historicos`).
3. **Empty month**: Call for a month with no data. ASSERT ventas_mes = 0, margen_por_categoria = `'[]'::jsonb`.

Cleanup in EXCEPTION block. Must fail before W1 is applied, pass after.

### W1 — `recalcular_resumen_diario()` (`202607XX00000_fix_recalcular_resumen_diario.sql`)

CTE structure (sketch):

```
WITH daily_ventas AS (
    SELECT d.id, d.monto_total,
        COALESCE((
            SELECT SUM(COALESCE(costo_real_od,0)+COALESCE(costo_real_oi,0)
                      +COALESCE(costo_real_montura,0)+COALESCE(costo_real_biselado,0)
                      +COALESCE(costo_real_lc,0))
            FROM dispensacion_items WHERE dispensacion_id = d.id
        ), d.costo_unitario_snapshot, 0) AS costo
    FROM dispensaciones d WHERE optica_id=p_optica_id AND fecha=p_fecha
    UNION ALL
    SELECT se.id, se.monto_total, 0::numeric
    FROM servicios_extra se WHERE optica_id=p_optica_id AND fecha=p_fecha
)
-- COUNT/SUM into v_ventas_* variables as Jul 10 lines 23
-- pagos CTE with Anulación filter (Jul 10 line 24)
-- saldo_pendiente via all_ventas UNION ALL + LEFT JOIN pagos_dedup (Jul 10 lines 25-34)
-- inventory unchanged (Jul 10 line 35)
-- INSERT ... ON CONFLICT DO UPDATE (Jul 10 lines 36-38)
```

Grants: `authenticated, service_role` only (SECURITY INVOKER + explicit GRANT).

### W2a — `rpc_analisis_mensual()` (`202607XX00001_fix_rpc_analisis_mensual.sql`)

Merge base: Jul 10 lines 66-85 (15-field JSON). Add: `v_meses_historicos INTEGER;` declaration and the Jul 13 `COUNT(DISTINCT DATE_TRUNC...` SELECT (lines 178-181). Append `'meses_historicos', v_meses_historicos` to the RETURN `jsonb_build_object` call. All other fields unchanged — `margen_por_categoria` reads `categorias_producto LEFT JOIN margen_por_categoria`, `proyeccion_caja` uses UNION ALL, `deudores` sub-calls `rpc_deudores()`.

### W2b — `DROP FUNCTION rpc_saldo_pendiente()` (`202607XX00002_drop_rpc_saldo_pendiente.sql`)

```sql
DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT);
```

Single-statement migration. No cascading effects — zero callers confirmed.

### W3 — Regenerate `resumen_diario` (`202607XX00003_regenerate_resumen_diario.sql`)

Loop over every distinct `(optica_id, fecha)` combination present in `dispensaciones` or `servicios_extra` that has a matching or missing `resumen_diario` row:

```sql
DO $$ DECLARE r RECORD; BEGIN
    FOR r IN
        SELECT d.optica_id, d.fecha FROM public.dispensaciones d
        UNION SELECT se.optica_id, se.fecha FROM public.servicios_extra se
        ORDER BY optica_id, fecha
    LOOP
        PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha);
    END LOOP;
END $$;
```

Post-condition: every day with transactional data has a fresh `resumen_diario` row.

### W4 — CHECK Constraints (`202607XX00004_add_pagos_domain_constraints.sql`)

```sql
ALTER TABLE public.pagos ADD CONSTRAINT chk_pagos_tipo
    CHECK (tipo IN ('Pago', 'Cuota', 'Anulación', 'Ajuste')) NOT VALID;
ALTER TABLE public.pagos ADD CONSTRAINT chk_pagos_metodo
    CHECK (metodo_pago IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'CtaCorriente')) NOT VALID;
```

Report existing rows violating the constraints (SELECT with NOT condition) as WARNING before constraint DDL. Re-runnable via `IF NOT EXISTS` guard in a DO block.

### Legacy Test Fixes

- **`test_schema_integrity.sql`**: Remove `('ventas'),` at line 27 from the expected core tables. Remove `'rpc_resumen_financiero'` from the expected functions array at line 224. No other changes.
- **`test_cost_recalculation.sql`**: Replace all references to `public.ventas` table with a UNION ALL CTE mimicking the function body. Create test data directly in `dispensaciones` + `servicios_extra` (not `ventas`). Assert `ventas_costo_total` equals SUM of `costo_real_*` from items plus snapshot fallback.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/tests/test_financial_pipeline_consistency.sql` | Create | W0 integration test — 3 assertions (happy path, 16 keys, empty month) |
| `supabase/tests/test_schema_integrity.sql` | Modify | Remove `ventas` from expected tables (L27), remove `rpc_resumen_financiero` from expected functions (L224) |
| `supabase/tests/test_cost_recalculation.sql` | Rewrite | UNION ALL pattern instead of `ventas` table |
| `supabase/migrations/202607XX00000_fix_recalcular_resumen_diario.sql` | Create | W1 — restore `recalcular_resumen_diario` with UNION ALL + real-cost |
| `supabase/migrations/202607XX00001_fix_rpc_analisis_mensual.sql` | Create | W2a — restore 16-field `rpc_analisis_mensual` with `meses_historicos` |
| `supabase/migrations/202607XX00002_drop_rpc_saldo_pendiente.sql` | Create | W2b — DROP FUNCTION rpc_saldo_pendiente |
| `supabase/migrations/202607XX00003_regenerate_resumen_diario.sql` | Create | W3 — regenerate all resumen_diario rows |
| `supabase/migrations/202607XX00004_add_pagos_domain_constraints.sql` | Create | W4 — CHECK constraints on pagos.tipo and pagos.metodo_pago |

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Integration | W0 financial pipeline | PostgreSQL DO block, test data + recalcular + assert output = SUM queries |
| Schema | Table existence | `test_schema_integrity.sql` (updated) — no longer expects `ventas` or `rpc_resumen_financiero` |
| Integration | Cost calculation | `test_cost_recalculation.sql` (rewritten) — UNION ALL data + items + assert ventas_costo_total |
| Migration | Idempotency | Each migration re-runnable via `CREATE OR REPLACE FUNCTION`, `DROP FUNCTION IF EXISTS`, or DO-block guards |
| Manual | Revenue gap closure | Run W3, SELECT SUM(ventas_monto_total) vs. SELECT SUM(monto_total) from UNION ALL source — gap must be < 0.01% |

## Deploy Strategy

**Local**: `supabase start` → `supabase db reset` → run all tests (W0 must pass) → apply migrations in order (00000–00004) → re-run tests → confirm no regression.
**Remote**: Apply migrations via `supabase db push` or Supabase SQL editor, in order.
**Rollback**: Each fix migration uses `CREATE OR REPLACE FUNCTION` (idempotent). To revert, re-apply any prior migration containing the target function. Data regeneration (W3) is destructive of old values — re-run it if rolled back. CHECK constraints (W4) are `NOT VALID` — drop with `ALTER TABLE ... DROP CONSTRAINT IF EXISTS`.

## Open Questions

None. All decisions resolved in exploration; this design describes execution.
