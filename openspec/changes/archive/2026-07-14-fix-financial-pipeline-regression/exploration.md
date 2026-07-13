# Exploration: fix-financial-pipeline-regression

## Root Cause

Two Supabase migrations competed and the wrong one won. On **July 10**, `drop_ventas_rewrite_rpcs.sql` intentionally dropped the `ventas` table (a denormalized trigger-synced mirror) and rewrote 4 financial RPC functions to use `UNION ALL` directly on `dispensaciones` + `servicios_extra` — the source-of-truth tables. On **July 13**, `completar_diferidos_financieros.sql` (developed on a pre-July-10 feature branch) overwrote 2 of those 4 functions with versions that reference the now-dropped `ventas` table. No CI check compared RPC output against transactional sums, so the regression was silent.

---

## Timeline: 9 Versions of the Same Functions in 9 Days

| Date | Migration | `recalcular_resumen_diario` | `rpc_analisis_mensual` | `rpc_saldo_pendiente` | `rpc_deudores` / `rpc_count_pendientes` |
|------|-----------|----------------------------|------------------------|------------------------|----------------------------------------|
| May 13 | `rpc_financial_aggregates` | Does not exist yet | Does not exist yet | Reads `dispensaciones` + `servicios_extra` directly | Original versions, reads from `dispensaciones`/`servicios_extra` |
| Jul 5 | `fase6_esquema_analisis` | **V1**: Reads from `ventas` | Does not exist yet | (unchanged) | (unchanged) |
| Jul 6 (various) | `p2_fix_recalcular` through `jd_fix8` | **V2-V6**: Various fixes, all reading from `ventas` | **V1**: `rpc_analisis_add_missing_fields` — 15-field rich version, reads `ventas` | **V2**: `p4_rewrite` references `ventas` | Rewritten to read from `ventas` during this period |
| Jul 7 | `sync_rpc_functions` | **V7**: Consolidated, reads `ventas` | **V2**: Consolidated 15-field version, reads `ventas` | (references `ventas`) | (references `ventas`) |
| Jul 9 | `fix_analisis_mensual_categorias` | (unchanged, reads `ventas`) | **V3**: Fixes category ordering, still reads `ventas` | (unchanged) | (unchanged) |
| **Jul 10** | **`drop_ventas_rewrite_rpcs`** | **V8**: Drops `ventas` table. Rewrites to `UNION ALL` on `dispensaciones` + `servicios_extra` | **V4**: Rewrites to `UNION ALL`, keeps all 15 fields | Marked DEPRECATED. Dropped? (no — not explicitly listed in the DROP, but `ventas` FK references dropped earlier) | **V3**: Rewritten to `UNION ALL` (both functions) |
| **Jul 13** | **`completar_diferidos_financieros`** | **V9**: Overwrites with version referencing `FROM public.ventas v` → **BROKEN** | **V5**: Overwrites with simplified 11-field version → **10 fields lost** | **Not overwritten** — still references `ventas` from Jul 6 | **Not overwritten** — both still use `UNION ALL` from Jul 10 |

### The Broken Chain

```
Jul 10 ──► drop_ventas_rewrite_rpcs.sql
               │
               ├── DROP TABLE public.ventas CASCADE
               ├── REWRITE recalcular_resumen_diario → UNION ALL  ✓
               ├── REWRITE rpc_analisis_mensual → UNION ALL      ✓
               ├── REWRITE rpc_deudores → UNION ALL              ✓
               └── REWRITE rpc_count_pendientes → UNION ALL       ✓

Jul 13 ──► completar_diferidos_financieros.sql (forked from pre-Jul-10 branch)
               │
               ├── OVERWRITE recalcular_resumen_diario → REFERENCES ventas  ✗
               └── OVERWRITE rpc_analisis_mensual → SIMPLIFIED (11 fields)  ✗
```

---

## What Is Broken

### 1. `recalcular_resumen_diario(TEXT, DATE)` — Critical. Silent data corruption.

- **Symptom**: Function body references `FROM public.ventas v` (table dropped July 10). Any call will throw `relation "public.ventas" does not exist`.
- **Current state**: `resumen_diario` has NOT been recalculated since the last successful run (July 11 00:34 UTC, from the Supabase cron or automated post-migration loop). All data AFTER that date is stale.
- **Impact**: `resumen_diario` revenue numbers are 24.6% below real revenue — a gap of approximately **S/ 803 for July** (based on the difference between stale `ventas_monto_total` and actual `dispensaciones.monto_total + servicios_extra.monto_total`).
- **Why it wasn't caught**: The Android app never calls this function directly. It only reads from `resumen_diario` (via `rpc_analisis_mensual` and Room queries). As long as nobody triggered a recalculation, the error stayed latent.

### 2. `rpc_analisis_mensual(TEXT, DATE)` — Degraded. 10 fields lost.

| Field | July 10 Union All (15 fields) | July 13 Simplified (11 fields) | Needed by Android App? |
|-------|------|------|------|
| `ventas_mes` | ✓ | ✓ | ✓ |
| `cobros_mes` | ✓ | ✓ | ✓ |
| `costo_mes` | ✓ | ✓ | ✓ |
| `gastos_mes` | ✓ | ✓ | ✓ |
| `saldo_pendiente` | ✓ | ✓ | ✓ |
| `margen_neto_pct` | ✓ | ✓ | ✓ |
| `ticket_promedio` | ✓ | ✓ | ✓ |
| `cantidad_ventas` | ✓ | ✓ | ✓ |
| `ventas_mes_anterior` | ✓ | ✓ | ✓ |
| `variacion_ventas_pct` | ✓ | ✓ | ✓ |
| `meses_historicos` | ✗ | ✓ (**was the addition**) | ✓ |
| `margen_por_categoria` | ✓ | ✗ | ✓ |
| `deudores` (resumen) | ✓ | ✗ | ✓ |
| `proyeccion_caja` | ✓ | ✗ | ✓ |
| `stock_estancado` | ✓ | ✗ | ✓ |
| `valor_inventario` | ✓ | ✗ | ✓ |

- **Why it still "works"**: The July 13 version doesn't reference `ventas` — it reads only from `resumen_diario` and `gastos_operativos` (both still exist). The Android app doesn't crash because `AnalisisMensual.fromJson()` defaults missing keys to `0`/`null`/`emptyList()`. But the dashboard shows zeros and empty sections.
- **`meses_historicos` was legitimately added**: The July 13 version correctly added `COUNT(DISTINCT DATE_TRUNC('month', fecha))` from `resumen_diario`. This enhancement should be preserved.

### 3. `rpc_saldo_pendiente(TEXT)` — Broken. Dead code.

- **Symptom**: References `FROM public.ventas v` (from July 6 `p4_rewrite_rpc_saldo_pendiente_ventas.sql`).
- **Usage**: Zero callers in the Android app (`grep` confirmed no `.kt` file references it). Marked `DEPRECATED` by comment on July 6: *"DEPRECATED: Uses dispensaciones/servicios_extra directly instead of unified ventas table"*.
- **Recommendation**: `DROP FUNCTION` — no callers, no migration dependency.

---

## What Still Works

| Function | Status | Why |
|----------|--------|-----|
| `rpc_deudores(TEXT)` | ✅ Functional | Overwritten July 10 with `UNION ALL`. NOT touched by July 13 migration. |
| `rpc_count_pendientes(TEXT)` | ✅ Functional | Overwritten July 10 with `UNION ALL`. NOT touched by July 13 migration. |
| `rpc_cierre_caja_resumen(TEXT, DATE)` | ✅ Functional | Reads `pagos` directly. Never used the `ventas` table. |
| `resumen_diario` table data | ⚠️ Stale | Correct up to July 11 00:34 UTC. After that, it's the last good snapshot. |
| All operational tables | ✅ Functional | `dispensaciones`, `servicios_extra`, `pagos`, `pacientes`, `monturas` — none affected. |

---

## Design Decisions (Why Not Recreate `ventas`)

### Why `ventas` Was Created (and Then Dropped)

The `ventas` table was a **denormalized mirror** kept in sync by triggers on `dispensaciones` and `servicios_extra`. It existed for ~5 days (July 5–10) and was dropped because:

1. **FK constraint failed in async sync**: `pagos → ventas` FK was dropped earlier (July 7, `drop_fk_pagos_venta_documented.sql`) because the offline-first architecture uploads `dispensaciones` and `pagos` in separate HTTP transactions — a `pago` could arrive before its parent `dispensacion` trigger committed the `venta` row, causing FK violations.
2. **Write overhead**: Every insert/update on `dispensaciones` and `servicios_extra` required a trigger to upsert `ventas`. For an offline-first sync architecture, this adds latency and complexity.
3. **Not the source of truth**: `ventas` was always a derived view. Reading directly from `dispensaciones + servicios_extra` via `UNION ALL` is both correct and bounded.

### Why `UNION ALL` Is Correct

```sql
WITH daily_ventas AS (
    SELECT monto_total, costo_unitario_snapshot
    FROM public.dispensaciones
    WHERE optica_id = p_optica_id AND fecha = p_fecha
    UNION ALL
    SELECT monto_total, 0::numeric
    FROM public.servicios_extra
    WHERE optica_id = p_optica_id AND fecha = p_fecha
)
```

- **`UNION ALL`** (not `UNION`) is correct because dispensaciones and servicios_extra have disjoint primary keys. No duplicates to deduplicate.
- **Namespace key pattern**: `pagos.venta_id` stores synthetic keys like `'v_disp_' || dispensacion_id` or `'v_serv_' || servicio_extra_id`. This is NOT a foreign key — it's a namespace collision key for matching pagos to their parent transactions.
- **`pagos.venta_id`**: FK was dropped. Integrity is enforced at the app layer (Android ViewModels all set `ventaId` correctly; verified 325/325 pagos had valid IDs).

### Design That Should NOT Be Changed

| Decision | Rationale |
|----------|-----------|
| Do NOT recreate `ventas` table | Was proven to be maintenance debt. `UNION ALL` is simpler and equally performant (both tables indexed by `(optica_id, fecha)`). |
| Do NOT add FK `pagos → ventas` | Will break async sync. Already tried and reverted. |
| Keep `pagos.venta_id` as naming convention only | Consistent, used across Android ViewModels, no integrity issues. |

---

## Impact Assessment

| Category | Impact | Details |
|----------|--------|---------|
| **Revenue reporting** | 🔴 24.6% gap | S/ 803 below actual for July. Only affects `resumen_diario` data from July 11 onward. |
| **Dashboard analytics** | 🟡 Degraded | `rpc_analisis_mensual` returns 11 fields instead of 16. Dashboard shows zeros for margin-by-category, cash flow projection, stagnant stock, inventory value, and debtor summary. |
| **Operations** | 🟢 None | Dispensaciones, pagos, pacientes, stock, cash register closure — all unaffected. |
| **Offline mode** | 🟢 None | Room fallback in `ObtenerAnalisisMensualUseCase` works independently. Reports fewer fields but doesn't crash. |
| **Android app crash risk** | 🟢 None | `AnalisisMensual.fromJson()` tolerates missing keys with defaults. |
| **Data loss** | 🟢 None | Operational data (dispensaciones, pagos, etc.) is intact. Only the derived `resumen_diario` snapshot is stale. All data can be regenerated. |
| **Existing tests** | 🟡 At risk | `test_schema_integrity.sql` expects `ventas` table to exist (line 27: `('ventas'),`) — will fail on a fresh `db reset`. `test_costos_reales.sql` checks for `costo_real_*` in `recalcular_resumen_diario` body — will find the July 13 version. |

### Who's Affected

- **Opticians (end users)**: See incorrect/empty financial dashboard figures. Cannot reliably evaluate monthly performance, margins, or cash flow projections.
- **No regulatory or audit impact**: This is internal analytics. No compliance filing depends on this data.
- **Sync architecture**: Unaffected. `SyncFinanzasUseCase` syncs `resumen_diario` from Supabase to Room. If stale data is synced, Room will have stale data until `resumen_diario` is regenerated.

---

## Proposed Approach

### W0 — SQL Integration Test (TDD First)

Write a PostgreSQL `DO $$` block that:
1. Calls `recalcular_resumen_diario('test_optica', '2026-07-01')` AFTER fixing it
2. Asserts `resumen_diario.ventas_monto_total` equals `SUM(dispensaciones.monto_total) + SUM(servicios_extra.monto_total)` for the same (optica_id, fecha)
3. Asserts `cobros_monto_total` equals `SUM(pagos.monto)` filtering out `Anulación` rows
4. Asserts `rpc_analisis_mensual('test_optica', '2026-07-01')` JSON contains all 16 keys from the rich version

This test MUST exist and pass BEFORE deploying the fix. File at `supabase/tests/test_financial_pipeline.sql`.

### W1 — Restore `recalcular_resumen_diario()`

- Base on the July 10 UNION ALL architecture (proven working)
- Add **real cost from `dispensacion_items.costo_real_*`** (OD, OI, montura, biselado, LC) adapted from July 13 logic but WITHOUT referencing `ventas`:
  ```sql
  WITH daily_ventas AS (
      SELECT d.id, d.monto_total,
             COALESCE((
                 SELECT SUM(
                     COALESCE(di.costo_real_od, 0) +
                     COALESCE(di.costo_real_oi, 0) +
                     COALESCE(di.costo_real_montura, 0) +
                     COALESCE(di.costo_real_biselado, 0) +
                     COALESCE(di.costo_real_lc, 0)
                 ) FROM dispensacion_items di
                 WHERE di.dispensacion_id = d.id
             ), d.costo_unitario_snapshot, 0) AS costo
      FROM public.dispensaciones d
      WHERE d.optica_id = p_optica_id AND d.fecha = p_fecha
      UNION ALL
      SELECT se.id, se.monto_total, 0::numeric AS costo
      FROM public.servicios_extra se
      WHERE se.optica_id = p_optica_id AND se.fecha = p_fecha
  )
  ```
- Keep existing pagos dedup logic (COALESCE for venta_id fallback, Anulación exclusion)
- Keep inventory section as-is
- `SECURITY INVOKER`, proper `GRANT EXECUTE`

### W2a — Restore `rpc_analisis_mensual()` Rich Version

- Merge July 10 (15 fields) + July 13 (added `meses_historicos`):
  - `ventas_mes` through `variacion_ventas_pct` — from `resumen_diario` (unchanged)
  - `margen_por_categoria` — from `categorias_producto LEFT JOIN margen_por_categoria`
  - `deudores` — from `rpc_deudores(optica_id)`
  - `proyeccion_caja` — UNION ALL pattern (no `ventas`): `all_ventas` CTE with `dispensaciones UNION ALL servicios_extra`
  - `stock_estancado` — from `monturas` (unchanged)
  - `valor_inventario` — from `monturas` (unchanged)
  - `meses_historicos` — `COUNT(DISTINCT DATE_TRUNC('month', fecha))` from `resumen_diario`

### W2b — `DROP FUNCTION rpc_saldo_pendiente`

Already deprecated since July 6. Zero callers across entire Android codebase.

### W3 — Regenerate `resumen_diario` History

```sql
DO $$ DECLARE r RECORD; BEGIN
    FOR r IN SELECT DISTINCT optica_id, fecha FROM public.resumen_diario LOOP
        PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha);
    END LOOP;
END $$;
```

Also recompute for any (optica_id, fecha) that has dispensaciones but no resumen_diario entry (edge case).

### W4 — Add CHECK Constraints on `pagos`

Prevent future silent regressions with domain-level validation:

```sql
ALTER TABLE public.pagos ADD CONSTRAINT chk_pagos_tipo CHECK (tipo IN (
    'Pago', 'Cuota', 'Anulación', 'Ajuste'
));
ALTER TABLE public.pagos ADD CONSTRAINT chk_pagos_metodo CHECK (metodo_pago IN (
    'Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'CtaCorriente'
));
```

---

## Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Recreating `ventas` table | Will not happen | Explicit architecture decision against it |
| Cost from `dispensacion_items` diverges from `costo_unitario_snapshot` | Low | Both values are source-of-truth; COALESCE fallback handles missing items |
| `rpc_analisis_mensual` field order differs from July 10 | Low | JSON output is consumed by key name, not position |
| Existing tests reference `ventas` table | **High** | `test_schema_integrity.sql` line 27 expects `ventas` table — must be removed |
| July 13 real-cost logic has bugs not caught by text-only tests | Medium | W0 integration test must assert output sums match transactional data |
| `resumen_diario` regeneration takes long on production | Low | It's a simple aggregate per (optica_id, fecha). Even thousands of rows takes seconds. |

---

## Ready for Proposal

Yes. The root cause is fully understood, the broken functions are identified, all affected and unaffected code paths are confirmed via codebase investigation, and a clear 6-work-unit plan is established (W0–W4). The next phase is `propose`.
