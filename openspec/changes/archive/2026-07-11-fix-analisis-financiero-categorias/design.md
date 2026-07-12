# Design: Fix Analisis Financiero Categorias

## Technical Approach

Single migration (`20260709000003_fix_analisis_mensual_categorias.sql`) that `CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual` — rewriting two sections inline via CTEs. Core indicators (ventas_mes, cobros_mes, gastos_mes, etc.) stay untouched. No schema changes, no new tables.

## Architecture Decisions

### Decision 1: CASE mapping `(tipo_lente, material_lente)` → `categoria_producto_id`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Lookup table for mapping | Extra maintenance, overengineering for 5 rules | **Rejected** |
| Inline CASE expression | Simple, no JOIN, easy to verify | **Chosen** |

Mapping based on actual data from `dispensaciones`:

```
Progresivo / any         → lente_progresivo (1)
Bifocal / any            → lente_bifocal    (3)
Monofocal / 'Resina'     → lente_monofocal  (2)
Monofocal / 'Cristal','Policarbonato','' → lente_otro (9)
else                     → lente_otro (9)   — safety fallback
```

`servicios_extra` is UNION ALL'd with hardcoded `categoria_producto_id = 'servicio_extra' (7)`.

### Decision 2: Stock estancado — two CTEs UNION'd

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Single GROUP BY with COALESCE | Multiple LEFT JOINs to subqueries, harder to read | **Rejected** |
| `ventas_montura` CTE (UNION of sale dates from both sources), then aggregate | Clean, handles sparse data gracefully | **Chosen** |

Both sources (`montura_movimientos.tipo='SALIDA_VENTA'` and `dispensaciones.montura_id`) are sparse — 8 and 4 rows respectively. UNION deduplicates, second CTE collapses by `MAX(ultima_venta)`. Final LEFT JOIN to `monturas`.

### Decision 3: Montura categories included in CASE mapping

**Chosen**: Include all 9 `categorias_producto` in the LEFT JOIN. Montura categories (`montura_premium`, `montura_estandar`, `montura_economica`) show `ventas=0` because no dispensaciones rows map to them. The JSON array always has 9 entries. Structure is ready for when `tipo_montura` data becomes available — no rework needed.

### Decision 4: Migration naming

Next timestamp after `20260709000002` is `20260709000003`. File: `20260709000003_fix_analisis_mensual_categorias.sql`.

## Data Flow

```
rpc_analisis_mensual(p_optica_id, p_mes)
  │
  ├── Core indicators (unchanged) ──→ resumen_diario, gastos_operativos
  │
  ├── margen_por_categoria ──→ CTE: category_revenue
  │   │                          ├── (dispensaciones → CASE → categoria_producto_id)
  │   │                          └── UNION ALL servicios_extra → 'servicio_extra'
  │   │                       └── CTE: aggregated_revenue (GROUP BY categoria_producto_id)
  │   │                       └── LEFT JOIN categorias_producto → jsonb_agg
  │
  └── stock_estancado ──→ CTE: ventas_montura
                             ├── (montura_movimientos WHERE tipo='SALIDA_VENTA')
                             └── UNION (dispensaciones WHERE montura_id IS NOT NULL)
                          └── CTE: montura_venta_agg (GROUP BY, MAX(ultima_venta))
                          └── LEFT JOIN monturas → jsonb_agg
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20260709000003_fix_analisis_mensual_categorias.sql` | **Create** | `CREATE OR REPLACE FUNCTION` — rewrites `margen_por_categoria` (inline CTE) and `stock_estancado` (remove low-stock filter, compute real `ultima_venta`/`dias_sin_venta`) |

Zero Android files changed. Zero schema changes.

## Exact Function Sections (non-trivial patterns)

### `margen_por_categoria` — inline CTE (replaces lines 261-265)

```sql
WITH category_revenue AS (
    SELECT
        CASE
            WHEN d.tipo_lente = 'Progresivo' THEN 'lente_progresivo'
            WHEN d.tipo_lente = 'Bifocal' THEN 'lente_bifocal'
            WHEN d.tipo_lente = 'Monofocal' AND d.material_lente = 'Resina' THEN 'lente_monofocal'
            WHEN d.tipo_lente = 'Monofocal' THEN 'lente_otro'   -- Cristal, Policarbonato, ''
            ELSE 'lente_otro'
        END AS categoria_producto_id,
        SUM(d.monto_total) AS ventas
    FROM public.dispensaciones d
    WHERE d.optica_id = p_optica_id
      AND d.fecha >= p_mes AND d.fecha < p_mes + INTERVAL '1 month'
    GROUP BY categoria_producto_id
    UNION ALL
    SELECT 'servicio_extra', SUM(se.monto_total)
    FROM public.servicios_extra se
    WHERE se.optica_id = p_optica_id
      AND se.fecha >= p_mes AND se.fecha < p_mes + INTERVAL '1 month'
),
aggregated_revenue AS (
    SELECT categoria_producto_id, SUM(ventas) AS ventas
    FROM category_revenue GROUP BY categoria_producto_id
)
SELECT COALESCE(jsonb_agg(jsonb_build_object(
    'categoria', cat.nombre,
    'ventas', COALESCE(ar.ventas, 0),
    'costos', 0,
    'margen_pct', null::numeric
) ORDER BY cat.orden), '[]'::jsonb)
INTO v_margen_categoria
FROM public.categorias_producto cat
LEFT JOIN aggregated_revenue ar ON ar.categoria_producto_id = cat.id;
```

### `stock_estancado` — real sales dates (replaces lines 299-302)

```sql
WITH ventas_montura AS (
    SELECT montura_id, MAX(fecha) AS ultima_venta
    FROM public.montura_movimientos
    WHERE optica_id = p_optica_id AND tipo = 'SALIDA_VENTA'
    GROUP BY montura_id
    UNION
    SELECT montura_id, MAX(fecha) AS ultima_venta
    FROM public.dispensaciones
    WHERE optica_id = p_optica_id AND montura_id IS NOT NULL
    GROUP BY montura_id
),
montura_venta_agg AS (
    SELECT montura_id, MAX(ultima_venta) AS ultima_venta
    FROM ventas_montura GROUP BY montura_id
)
SELECT COALESCE(jsonb_agg(jsonb_build_object(
    'montura_id', m.id, 'sku', m.sku, 'modelo', m.modelo,
    'costo', COALESCE(m.costo, 0), 'stock_actual', m.stock_actual,
    'ultima_venta', mva.ultima_venta,
    'dias_sin_venta', CASE WHEN mva.ultima_venta IS NOT NULL
        THEN (CURRENT_DATE - mva.ultima_venta) ELSE 999 END
) ORDER BY CASE WHEN mva.ultima_venta IS NULL THEN 0 ELSE 1 END,
    mva.ultima_venta ASC NULLS LAST), '[]'::jsonb)
INTO v_stock_estancado
FROM public.monturas m
LEFT JOIN montura_venta_agg mva ON mva.montura_id = m.id
WHERE m.optica_id = p_optica_id AND m.activo = true AND m.stock_actual > 0;
```

## Testing Strategy (TDD — strict mode)

| # | Type | What | Before Migration | After Migration | Way to Test |
|---|------|------|-----------------|-----------------|-------------|
| T1 | SQL unit | CASE mapping correctness | Run `SELECT CASE...END` with known inputs → wrong categories | Expected mapping works | Simple `SELECT` test query |
| T2 | SQL integration | `margen_por_categoria` returns non-zero `ventas` | All zeros | Monofocal/Resina ~S/26,685, Progresivo~S/3,530, Bifocal~S/5,600 | `SELECT * FROM rpc_analisis_mensual('o1','2026-07-01')→'margen_por_categoria'` |
| T3 | SQL integration | `stock_estancado` shows real dates | All `ultima_venta=NULL, dias_sin_venta=999` | Monturas with sales show real dates; never-sold show 999 | Same RPC call → `stock_estancado` JSON |
| T4 | SQL integration | All 9 categories present | Only categories in `margen_por_categoria` table | 9 rows, all `ventas >= 0` | Count JSON array length |
| T5 | Android unit | `AnalisisMensual.fromJson()` deserialization | Already passes (zeros) | Still passes with real values | `./gradlew :optoapp:testDebugUnitTest` — no regressions |

T1–T4 are SQL scripts in `supabase/tests/` (new dir). T5 is existing test suite — must still pass.

### Execution order

```
T1 (CASE unit) → T2 (margin values) → T3 (stock dates) → T4 (full coverage) → Apply migration → T1' (PASS) → T2' (PASS) → T3' (PASS) → T4' (PASS) → T5 (Android regression)
```

## Migration / Rollout

1. Create `20260709000003_fix_analisis_mensual_categorias.sql` with the full `CREATE OR REPLACE FUNCTION`
2. `REVOKE/GRANT EXECUTE` (same as current: `authenticated`, `service_role` only)
3. Apply: `supabase migration up` or manual apply
4. **Rollback**: Restore previous function body (from `20260709000002`). Down-migration is not needed since schema is unchanged — the previous migration's function body can be re-applied.

## Open Questions

None. All four architecture decisions resolved.
