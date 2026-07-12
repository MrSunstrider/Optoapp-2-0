## Exploration: fix-analisis-financiero-categorias

### 1. Current DB State

**Tables created by Fase 6 migration (idempotent, all exist):**

| Table | Rows | Populated? | Purpose |
|-------|------|------------|---------|
| `categorias_producto` | 9 (seed) | ✅ Seed data | Global categories: lente_progresivo, lente_monofocal, lente_bifocal, lente_otro, montura_premium, montura_estandar, montura_economica, servicio_extra, servicio_garantia |
| `margen_por_categoria` | **0** | ❌ Empty | Server-calculated margin per category/period — **nothing populates it** |
| `resumen_diario` | 152 | ✅ Populated via `recalcular_resumen_diario()` | Daily aggregated financial summaries (2025-01-08 to 2026-07-10) |
| `costos_productos` | **0** | ❌ Empty | Per-optica product cost history — **no UI writes to it, no sync downloads it** |
| `configuracion_financiera` | ~1 | ✅ Per-optica settings (margen objetivo, alertas, etc.) | Read by `GenerarRecomendacionesUseCase` |
| `gastos_operativos` | ~10+ | ✅ User-entered expenses | Has full Room Entity/DAO/ViewModel/UI pattern |
| `feedback_recomendaciones` | 0 | ❌ Empty (optional, per-user feedback) | Append-only feedback for recommendations |

**Data flow:**

- `dispensaciones` (241 rows, all with monto_total > 0) + `servicios_extra` (99 rows) = sales data
- `recalcular_resumen_diario()` aggregates daily totals → `resumen_diario`
- `rpc_analisis_mensual()` reads `resumen_diario` + `gastos_operativos` + `margen_por_categoria` (LEFT JOIN = zeros!)
- No function populates `margen_por_categoria` — it was designed to be written by a server-side trigger/job that was never implemented
- `ventas` table was **dropped** in migration `20260709000002` — all RPCs now UNION `dispensaciones` + `servicios_extra`

### 2. RPC Dependency Map

```
rpc_analisis_mensual(p_optica_id, p_mes)
├── resumen_diario          → ventas_mes, cobros_mes, costo_mes, cantidad_ventas, saldo_pendiente, ventas_mes_anterior
├── gastos_operativos       → gastos_mes
├── categorias_producto     → lista de categorías
├── margen_por_categoria    → LEFT JOIN → TODOS CEROS (tabla vacía)
├── rpc_deudores()          → deudores_resumen (cantidad, saldo_total)
│   └── dispensaciones      → UNION
│   └── servicios_extra     → UNION  
│   └── pagos               → restar pagos
├── dispensaciones          → proyeccion_caja (UNION + pagos dedup)
├── servicios_extra         → proyeccion_caja (UNION + pagos dedup)
└── monturas                → stock_estancado (bug: stock_actual<=stock_minimo, hardcoded 999 días)
  └── costo*stock_actual    → valor_inventario

recalcular_resumen_diario(p_optica_id, p_fecha)
├── dispensaciones          → daily sales (UNION ALL, hardcoded costo=0)
├── servicios_extra         → daily sales (UNION ALL, hardcoded costo=0)
├── pagos                   → daily cobros (excluye Anulación)
├── pagos_dedup + all_ventas → saldo pendiente
└── monturas                → inventario_valor (costo*stock_actual)

rpc_deudores(p_optica_id)
├── pagos                   → pagos_dedup (excluye Anulación)
├── dispensaciones          → UNION
├── servicios_extra         → UNION
└── pacientes               → nombres + teléfonos
```

**Key insight**: `rpc_analisis_mensual` currently produces the `margen_por_categoria` section by LEFT JOINing `categorias_producto` with `margen_por_categoria`. Since `margen_por_categoria` is empty, every category shows ventas=0, costos=0, margen_pct=0. The query structure is correct — the data is simply missing.

### 3. Android Code Touch Points

| File | Lines | Impact |
|------|-------|--------|
| `AnalisisMensual.kt` | 1-139 | **Data model**. `MargenCategoria` expects `categoria`, `ventas`, `costos`, `margenPct`. `StockEstancadoItem` expects `monturaId`, `sku`, `modelo`, `costo`, `stockActual`, `ultimaVenta`, `diasSinVenta`. **JSON parsing keys match exactly what rpc_analisis_mensual returns** — no model changes needed unless we add new fields |
| `AnalisisDetalleScreen.kt` | L99-168 | **UI**. Two expandable sections: "Lo que más te deja" (shows `analisis.margenPorCategoria`) and "Productos sin vender" (shows `analisis.stockEstancado`). Currently shows "Sin datos de categorías" because data is empty |
| `AnalisisNegocioScreen.kt` | 1-625 | **Main "Análisis Financiero" screen**. Reads `uiState.analisis` for resumen card. Uses GastosViewModel inline for expense entry dialog. The Gastos pattern is directly relevant for Propuesta D |
| `ObtenerAnalisisMensualUseCase.kt` | 1-69 | **RPC caller**. Calls `rpc_analisis_mensual` with `(p_optica_id, p_mes)`. Falls back to Room (empty data) on IO error. **No changes needed** — it just passes through whatever the RPC returns |
| `GenerarRecomendacionesUseCase.kt` | 1-263 | **Recommendations engine**. Uses `margenPorCategoria` for `evaluarMejorarPrecio()` and `evaluarVenderMasDe()` — currently get correct data but with zeros. Uses `stockEstancado` for `evaluarLiquidarStock()` — currently gets low-stock items with hardcoded 999 days, so the liquidar-stock recommendation will fire incorrectly |
| `AnalisisNegocioViewModel.kt` | 1-153 | **ViewModel**. Orchestrates 3 parallel calls: `obtenerAnalisisMensual`, `obtenerDeudores`, `generarRecomendaciones`. No changes needed |
| `ResumenDiarioEntity.kt` | 1-21 | **Room entity**. Mirrors `resumen_diario` table. CreatedAt is String? |
| `ConfiguracionFinancieraEntity.kt` | 1-18 | **Room entity**. Mirrors `configuracion_financiera`. No optica_ID → opticaId translation issue? |
| `CategoriaProductoEntity.kt` | 1-12 | ✅ Already exists. Room entity mirroring `categorias_producto`. DAO with `getAll()`, `getByFamilia()`, `insertAll()` |
| `GastoOperativoEntity.kt` | 1-24 | **Reference pattern** for Propuesta D. Full entity + DAO + ViewModel + Screen pattern |
| `OptoDatabase.kt` | 34-47 | Entities list (v38). Currently has `CategoriaProductoEntity`, `GastoOperativoEntity`, `ResumenDiarioEntity`, `ConfiguracionFinancieraEntity`. **No CostoProducto entity exists** |
| `SyncFinanzasDto.kt` | 1-415 | Remote DTOs for sync. **No CostoProductoRemoto exists**. The sync system does NOT download `costos_productos` |

**No `CostoProducto` entity, DAO, or remote DTO exists anywhere in the codebase.**

### 4. Data Quality Assessment

**Dispensaciones by tipo_lente + material_lente (for possible categorization):**

| tipo_lente | material_lente | Count | Total Sales |
|------------|---------------|-------|-------------|
| Monofocal | Resina | 182 | S/ 26,685 |
| Bifocal | Resina | 39 | S/ 5,600 |
| Monofocal | Cristal | 10 | S/ 1,360 |
| Progresivo | Resina | 7 | S/ 3,530 |
| Monofocal | (empty) | 1 | S/ 100 |
| Monofocal | Policarbonato | 1 | S/ 240 |
| Bifocal | Cristal | 1 | S/ 140 |

- 241 dispensaciones total, all with monto_total > 0
- Only **241/241 ≈ 100%** have both tipo_lente and material_lente populated
- **Valid mapping possible**: tipo_lente + material_lente → categoria_producto_id
- `costos_productos` has **0 rows** — no cost data exists anywhere
- `monturas.costo`: **8/11 monturas have costo=0**, only 3 have costo > 0
- `montura_movimientos`: only **8 records** total (4 SALIDA_VENTA, 3 ENTRADA, 1 SALIDA)
- Only **4 dispensaciones** have `montura_id` set — linking sales to specific frames is almost nonexistent

**Tratamientos data (for potential service add-on categorization):**
- AR Blue Defense: 61
- Antireflejo: 42
- Fotocromático: 17
- AR Blue Defense + Fotocromático: 7
- Various combinations

**Dispensaciones by tipo_montura + material_montura (for montura categorization):**
| tipo_montura | material_montura | Count | Total |
|-------------|-----------------|-------|-------|
| (empty) | Metal | 106 | S/ 15,300 |
| (empty) | Acetato | 80 | S/ 16,115 |
| (empty) | Carey | 39 | S/ 4,560 |
| (empty) | Econ | 8 | S/ 560 |
| (empty) | (empty) | 8 | S/ 1,120 |

Note: `tipo_montura` is always empty — the categorization would need to use `material_montura`.

### 5. Cost Tracking Design Options (Propuesta D)

**Existing patterns to follow:**

The `gastos_operativos` implementation provides a complete reference:

1. **Supabase table**: `costos_productos` already exists with schema: id(UUID), optica_id(TEXT), categoria_producto_id(TEXT), producto_descripcion(TEXT), costo_unitario(NUMERIC), vigente_desde(DATE), vigente_hasta(DATE), fecha_actualizacion(TIMESTAMPTZ)
2. **RLS policies**: Already defined (select for members, insert/update for admin/gerente, delete for admin)
3. **Missing in Android**: Entity, DAO, Remote DTO, sync coordinator, ViewModel, UI screen

**Option A: Product-cost-per-category (simple)**
- One cost per `(optica_id, categoria_producto_id)` — the average or typical cost for that category
- Store in `costos_productos` with `vigente_hasta=NULL` for current
- Show as a simple inline editable list in an existing settings/configuration screen
- **Pros**: Simple, maps directly to existing `categorias_producto` seed data (9 categories)
- **Cons**: Doesn't capture individual product variation

**Option B: Per-dispensacion cost entry (complex)**
- Add a cost field to the dispensacion form or to dispensacion_items
- Requires migration of `dispensaciones` or `dispensacion_items` table
- **Pros**: Most accurate margin calculation
- **Cons**: Massive UI change, data entry burden on opticians, most dispensaciones already exist without cost data

**Option C: Hybrid (Option A for now, Option B later)**
- Start with category-level costs in `costos_productos` for initial margin visibility
- Later add per-product costs for precision
- **Recommended**: Quick win + future-proof

**UI placement options for cost entry:**
- Inside the existing "Análisis Financiero" screen as an expandable "Costos por categoría" section (like Gastos section already is)
- As a standalone screen in the settings/config section
- As a slide-out dialog similar to the existing `GastosViewModel` pattern

### 6. Approaches for "Lo que más te deja"

**Approach A: Compute margin inline in `rpc_analisis_mensual` (no `margen_por_categoria` needed)**

- Group dispensaciones by `(tipo_lente, material_lente)` → map to `categoria_producto_id`
- Compute revenue per category
- If cost data exists in `costos_productos`, subtract; else show revenue only

```sql
WITH ventas_categoria AS (
  SELECT 
    CASE 
      WHEN tipo_lente = 'Progresivo' THEN 'lente_progresivo'
      WHEN tipo_lente = 'Monofocal' AND material_lente = 'Resina' THEN 'lente_monofocal'
      WHEN tipo_lente = 'Bifocal' THEN 'lente_bifocal'
      ELSE 'lente_otro'
    END AS cat_id,
    SUM(monto_total) as ventas
  FROM public.dispensaciones
  WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month'
  GROUP BY cat_id
  UNION ALL
  SELECT 'servicio_extra' AS cat_id, SUM(monto_total)
  FROM public.servicios_extra
  WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month'
)
SELECT cat.nombre, COALESCE(vc.ventas, 0), 0 as costos, NULL as margen_pct
FROM categorias_producto cat
LEFT JOIN ventas_categoria vc ON vc.cat_id = cat.id;
```

- **Effort**: Low (SQL-only, no new entities)
- **Pros**: No cost data dependency, immediate fix, no new tables
- **Cons**: Margin can't be computed without costs; mapping logic is heuristic
- **Fixes**: ✅ Shows real data, solves the "all zeros" problem

**Approach B: Populate `margen_por_categoria` via a new `recalcular_margenes()` function**

- Create a stored procedure that computes and upserts into `margen_por_categoria`
- Call it from `recalcular_resumen_diario()` or as a separate scheduled job
- Requires cost data to be populated first

- **Effort**: Medium (new SQL function + trigger/schedule)
- **Pros**: Pre-computed, clean separation, supports historical data
- **Cons**: Requires cost data; still shows zeros until costs exist; more complex

**Approach C: Compute inline in `rpc_analisis_mensual` with costos_productos lookup**

- Same as A but also JOIN `costos_productos` to compute estimated costs per category
- For each category, find the active cost (vigente_hasta IS NULL) and multiply by quantity

- **Effort**: Low (SQL-only)
- **Pros**: Real margin when costs are entered
- **Cons**: `costos_productos` is empty now — shows zeros for costs until user enters data

**Recommendation**: **Approach A** as the immediate fix (show revenue per category), then evolve to Approach C once Propuesta D is implemented.

### 7. Approaches for "Productos sin vender"

**Approach A: Use `montura_movimientos` SALIDA_VENTA to find unsold frames**

```sql
SELECT m.id, m.sku, m.modelo, COALESCE(m.costo,0) as costo, m.stock_actual,
  (SELECT MAX(mm.fecha) FROM montura_movimientos mm 
   WHERE mm.montura_id = m.id AND mm.tipo = 'SALIDA_VENTA') as ultima_venta,
  CASE WHEN MAX(mm.fecha) IS NOT NULL 
    THEN CURRENT_DATE - MAX(mm.fecha) 
    ELSE 999 
  END as dias_sin_venta
FROM monturas m
LEFT JOIN montura_movimientos mm ON mm.montura_id = m.id AND mm.tipo = 'SALIDA_VENTA'
WHERE m.optica_id = p_optica_id AND m.activo = true
GROUP BY m.id
HAVING COUNT(mm.id) = 0 OR MAX(mm.fecha) < CURRENT_DATE - INTERVAL '90 days'
ORDER BY dias_sin_venta DESC;
```

- **Effort**: Low
- **Pros**: Correct metric (unsold, not low stock)
- **Cons**: Only 4 SALIDA_VENTA records exist in montura_movimientos — most monturas will show as "never sold"

**Approach B: Also check `dispensaciones.montura_id` for sales**

```sql
LEFT JOIN dispensaciones d ON d.montura_id = m.id
```

- Only 4 dispensaciones have montura_id set — limited help
- But combining with montura_movimientos gives a more complete picture

**Approach C: Use `stock_actual > stock_minimo` as a filter (current behavior) but fix the hardcoded values**

The current behavior isn't entirely wrong — low stock items near reorder point is a valid concern. But the requirement is "productos sin vender" (unsold) not "productos con stock bajo" (low stock).

- **Approach A + B combined** is the most correct approach given data reality:
  - A montura is "unsold" if it has NO `SALIDA_VENTA` in `montura_movimientos` AND no row in `dispensaciones.montura_id`
  - For monturas that DO have sales, compute actual `dias_sin_venta` from the last sale date
  - Remove the `stock_actual<=stock_minimo` filter — use actual sales data

**Recommendation**: **Approach A + B combined** — query montura_movimientos LEFT JOINed with dispensaciones to get the most accurate picture. Accept that with current sparse data, most monturas will show as "never sold." This is honest and correct — when users start recording frame sales properly, the data will improve.

### 8. Migration Strategy

**Proper migration order:**

| Step | What | Migration File | Breaks Android? |
|------|------|---------------|-----------------|
| 1 | Seed mapping: `tipo_lente+material_lente → categoria_producto_id` | New SQL | No (new table/function) |
| 2 | Rewrite `rpc_analisis_mensual` — inline margen_por_categoria computation + fix stock_estancado | New SQL | **Yes — JSON response structure changes.** Must verify field names match `AnalisisMensual.fromJson()` |
| 3 | (Propuesta D) Create `CostoProductoEntity` + DAO + Remote DTO | Room migration v38→v39 | Yes — DB schema change required |
| 4 | (Propuesta D) Add costo entry UI | New Compose screens | Yes — new screens |
| 5 | (Propuesta D) Add sync download for `costos_productos` | New sync coordinator code | Yes — new download table |
| 6 | (Propuesta D) Rewrite margin computation to include costs | New SQL + code | Yes — RPC return values may change |

**What DOES NOT break:**
- `AnalisisMensual.fromJson()` reads `margen_por_categoria` as `[{"categoria":"...","ventas":...,"costos":...,"margen_pct":...}]` — the JSON structure stays the same, just values change from 0 to real
- `StockEstancadoItem` reads `dias_sin_venta` — currently always 999, will change to real values

**What DOES break if we add `margen_pct` as non-null:**
- Currently `margenPct` is `Double?` in `MargenCategoria` — safe
- If we add new fields, `fromJson()` would need to handle them — but extra JSON fields are ignored by kotlinx.serialization, so new additions are backward-compatible

### 9. Risks and Dependencies

| Risk | Impact | Mitigation |
|------|--------|------------|
| **categorias_producto mapping** — categorizing Monofocal+Resina as "lente_monofocal" is correct for this optica but may not generalize | Incorrect categorization for different opticas | Make the mapping configurable or use a lookup table per optica; start with heuristic and iterate |
| **Stock estancado hardcoded NULL/dias_sin_venta** — the current code sends NULL/dias_sin_venta=999. Auto-generating recommendations from this has been non-functional because `evaluarLiquidarStock` checks `diasSinVenta > stockEstancadoAlertaDias` (default 180), so with 999 it would have fired wrongly | Bad recommendations | Fixed by correct query; verify recommendation logic after fix |
| **No monturas for this optica** — the test DB has 11 monturas, total inventory. But real opticas may have many more. Performance of STOCK_ESTANCADO query could degrade | Slow RPC response | Add proper index on `montura_movimientos(montura_id, tipo, fecha)` if needed |
| **Cost data is incomplete** — `costos_productos` is empty, `monturas.costo` is mostly 0 | Cost-based margin will be 0 everywhere | First fix: show revenue-only. Then: implement cost entry. Document: "Add costs to see real margins" |
| **Offline fallback** — `ObtenerAnalisisMensualUseCase.fallbackToRoom()` returns empty `margenPorCategoria` and `stockEstancado`. If we add inline computation in the RPC, offline mode still returns empty data for these sections | Offline = empty sections | Acceptable tradeoff. Offline is degraded mode. When resumen_diario is synced to Room, at least the main indicators work |
| **costo_total in resumen_diario** — currently hardcoded to 0 in `recalcular_resumen_diario()` because `costo_unitario_snapshot` was removed when `ventas` was dropped | costo_mes is always 0 | This affects `margen_neto_pct` calculation. Without real cost data, the net margin is inflated. Need to either populate from costos_productos or document the limitation |

### 10. Recommendation

**Approach:** **Phase the fix in 2-3 work slices:**

**Slice 1 (CORE FIX — DB only, no Android changes):**
1. Rewrite `rpc_analisis_mensual` to compute `margen_por_categoria` inline from `dispensaciones` + `servicios_extra`, grouped by `categoria_producto_id` (derived from tipo_lente+material_lente)
2. Fix the `stock_estancado` query: remove the `stock_actual<=stock_minimo` filter, use `montura_movimientos` + `dispensaciones.montura_id` to find truly unsold items and compute real `dias_sin_venta`
3. The JSON response structure stays identical — `AnalisisMensual.fromJson()` continues to work unchanged
4. **Effort**: ~1 day (SQL + migration file + test)

**Slice 2 (PROPUESTA D — cost entry):**
1. Create `CostoProductoEntity` + `CostoProductoDao` in Room (table: `costos_productos`)
2. Create `CostoProductoRemoto` DTO for sync
3. Add sync download for `costos_productos` in `DownloadSyncCoordinator` and `SyncFinanzasUseCase`
4. Create a cost entry UI (follow GastosViewModel pattern — dialog in AnalisisNegocioScreen)
5. When costs are populated, `rpc_analisis_mensual` can JOIN `costos_productos` to compute real margin
6. **Effort**: ~2-3 days (Room entity + DAO + sync + UI)

**Slice 3 (ENHANCEMENT):**
1. Create `recalcular_margenes()` function to populate `margen_por_categoria` for historical data
2. Add a nightly/scheduled trigger or just call it from `recalcular_resumen_diario()`
3. This is optional — the inline computation suffices for real-time display
4. **Effort**: ~0.5 day

**Immediate no-brainer fixes:**
- The `recalcular_resumen_diario()` function hardcodes `0::numeric AS costo_unitario_snapshot` — this means `ventas_costo_total` in `resumen_diario` is always 0, which makes `costo_mes` and `margen_neto_pct` wrong. Fixing this requires cost data (Slice 2).

**Deliverable:** Start with Slice 1 for the explore → propose → spec → design → tasks pipeline. This gives the biggest visible impact (the two empty sections start showing real data) with the smallest code footprint.
