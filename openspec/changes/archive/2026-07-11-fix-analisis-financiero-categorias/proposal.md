# Proposal: fix-analisis-financiero-categorias

## Intent

Two bugs in `rpc_analisis_mensual` make "Lo que más te deja" and "Productos sin vender" sections useless. Fix both with SQL-only changes — zero Android code touched.

## Bugs & Fixes

### Bug 1: "Lo que más te deja" shows all zeros
`margen_por_categoria` table has 0 rows. The RPC LEFT JOINs `categorias_producto` (9 seed rows) with an empty table → every category shows `ventas=0, costos=0, margen_pct=0`.

**Fix**: Rewrite the `margen_por_categoria` section to compute revenue inline from `dispensaciones` + `servicios_extra`. Map `tipo_lente + material_lente` to `categoria_producto_id` via a CASE expression. No cost data needed yet — `costos=0, margen_pct=null` until Slice 2 (Propuesta D) adds cost entry. JSON structure stays identical (`categoria, ventas, costos, margen_pct`).

### Bug 2: "Productos sin vender" shows wrong metric
Current query filters `stock_actual <= stock_minimo` (low-stock alert, not unsold). `diasSinVenta` hardcoded to 999, `ultimaVenta` hardcoded to NULL.

**Fix**: Remove the low-stock filter. Query `montura_movimientos (tipo='SALIDA_VENTA')` + `dispensaciones.montura_id` to find actual last sale date per montura. Compute real `dias_sin_venta`. A montura is unsold if it has no SALIDA_VENTA and no `dispensaciones.montura_id` link.

## Approach

**Single migration** rewriting `rpc_analisis_mensual` with:
1. **Inline `margen_por_categoria` computation**: CTE grouping dispensaciones by `(tipo_lente, material_lente)` mapped to `categoria_producto_id`, UNION ALL `servicios_extra` as `servicio_extra`, LEFT JOIN to `categorias_producto` for full category list
2. **Fixed `stock_estancado`**: Remove `stock_actual<=stock_minimo` WHERE clause. Add LEFT JOIN to `montura_movimientos` (filtered to SALIDA_VENTA) and `dispensaciones`. Compute `ultima_venta = MAX(fecha)`, `dias_sin_venta = CURRENT_DATE - ultima_venta` for sold items, 999 for never-sold.

No new tables, no new functions, no schema changes. The RPC's JSON response structure (`margen_por_categoria[]`, `stock_estancado[]`) is preserved — `AnalisisMensual.fromJson()` and `StockEstancadoItem.fromJson()` work unchanged.

## Capabilities Affected

| Spec | Relevance |
|------|-----------|
| `analisis-negocio` (R23) | Defines `rpc_analisis_mensual`. Currently specifies that `margen_por_categoria` is NOT part of the RPC — this proposal CHANGES that by computing it inline. R23's base indicators remain untouched. The delta is `margen_por_categoria` and `stock_estancado` now come from inline computation within the same RPC instead of separate table queries |
| `recomendaciones` (R4–R6) | `evaluarMejorarPrecio()` consumes `margenPorCategoria` — currently gets zeros, will now get real revenue data. `evaluarLiquidarStock()` consumes `stockEstancado` — currently gets low-stock items with hardcoded 999 days (would fire wrongly), will now get unsold items with real days. Recommendation logic unchanged |
| `reportes-financieros` | Not affected — no margin-by-category or stock data used |
| `indicadores-negocio` | Not affected — RPC is consumed via `ObtenerAnalisisMensualUseCase` which delegates to the same `AnalisisMensual.fromJson()`. Only values change, not structure |

## Rollback Plan

1. **Down-migration**: Revert `rpc_analisis_mensual` to the previous definition (before this change). The function is versioned via migrations — the down SQL is the CREATE OR REPLACE FUNCTION with the old body
2. **Verify rollback**: Call `rpc_analisis_mensual('o1', '2026-07-01')` — both sections return to pre-fix behavior (zeros for margin, low-stock filter for stock_estancado)
3. **No data loss**: No schema changes, no table data affected — rollback is swapping the function body only

## Success Criteria

- [ ] `rpc_analisis_mensual('o1', '2026-07-01')` returns `margen_por_categoria` with non-zero `ventas` for at least 3 categories (matching real dispensing data: Monofocal/Resina S/ 26,685, Progresivo/Resina S/ 3,530, Bifocal/Resina S/ 5,600)
- [ ] `stock_estancado` items show actual `ultima_venta` dates (not all NULL) and `dias_sin_venta` values computed from `montura_movimientos` + `dispensaciones.montura_id`
- [ ] Monturas with no sales at all show `dias_sin_venta = 999`, `ultima_venta = null`
- [ ] JSON structure matches `AnalisisMensual.fromJson()` expectations — no deserialization failures
- [ ] `AnalisisDetalleScreen` expandable sections show data instead of "Sin datos de categorías"
- [ ] `GenerarRecomendacionesUseCase.evaluarLiquidarStock` no longer fires misleadingly for low-stock items that were actually sold recently
