# Delta for analisis-negocio

## MODIFIED Requirements

### R9: Supabase RPC `recalcular_resumen_diario`

The system SHALL create a PostgreSQL function `public.recalcular_resumen_diario(p_optica_id TEXT, p_fecha DATE) RETURNS void` that idempotently upserts a row into `resumen_diario`.

#### R9.1: Calculation Logic

The function SHALL:

1. **Sales aggregation**: Query `public.ventas` WHERE `optica_id = p_optica_id AND fecha = p_fecha` to compute:
   - `ventas_cantidad` = `COALESCE(COUNT(*), 0)`
   - `ventas_monto_total` = `COALESCE(SUM(monto_total), 0)`
   - `ventas_costo_total` = For each venta, JOIN `dispensaciones ON dispensaciones.venta_id = ventas.id` then JOIN `dispensacion_items ON dispensacion_items.dispensacion_id = dispensaciones.id`. Sum `COALESCE(dispensacion_items.costo_real_od, 0) + COALESCE(dispensacion_items.costo_real_oi, 0) + COALESCE(dispensacion_items.costo_real_montura, 0) + COALESCE(dispensacion_items.costo_real_biselado, 0) + COALESCE(dispensacion_items.costo_real_lc, 0)`. For ventas with no matching `dispensacion_items` (e.g., `servicio_extra`), fall back to `COALESCE(ventas.costo_unitario_snapshot, 0)`.

(Previously: `ventas_costo_total` summed `costo_unitario_snapshot` directly from `ventas` without JOIN to `dispensacion_items`.)

2. **Payments aggregation**: Query `public.pagos` WHERE `optica_id = p_optica_id AND fecha = p_fecha` to compute:
   - `cobros_cantidad` = `COALESCE(COUNT(*), 0)`
   - `cobros_monto_total` = `COALESCE(SUM(monto), 0)`

3. **Pending balance**: Query `public.ventas` LEFT JOIN aggregated `public.pagos` by `venta_id`:
   - `saldo_pendiente_cantidad` = COUNT of ventas where `monto_total - COALESCE(total_pagado, 0) > 0.005`
   - `saldo_pendiente_total` = SUM of the same difference

4. **Inventory snapshot**: Query `public.monturas` WHERE `optica_id = p_optica_id`:
   - `inventario_valor` = `COALESCE(SUM(costo * stock_actual), 0)`
   - `inventario_unidades` = `COALESCE(SUM(stock_actual), 0)`

5. **Idempotent upsert**: `INSERT INTO resumen_diario (...) VALUES (...) ON CONFLICT (optica_id, fecha) DO UPDATE SET ...` updating all computed fields plus `calculado_en = now()`.

#### R9.2: RPC Security

The function SHALL be defined as `SECURITY INVOKER` so it respects RLS policies of the calling user.

#### R9.3: Null-safe Inventory

If `monturas.costo` is NULL for any row, `COALESCE(SUM(costo * stock_actual), 0)` would return 0 for that row's contribution — this is acceptable. The function SHALL NOT fail on NULL cost values.

#### Scenario: costo_real_* from dispensacion_items is used when items exist

- GIVEN a venta on 2026-07-05 with linked dispensacion_items having `costo_real_od = 25.00` and `costo_real_montura = 80.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `105.00` (25 + 80) for that venta, NOT `costo_unitario_snapshot`

#### Scenario: costo_unitario_snapshot fallback for servicio_extra venta

- GIVEN a venta on 2026-07-05 with `categoria_producto_id = 'servicio_extra'` and no linked dispensacion_items, with `costo_unitario_snapshot = 15.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `15.00` from the fallback column

#### Scenario: Mixed ventas — some with items, some without

- GIVEN a mix of dispensacion-linked ventas and servicio_extra ventas on the same fecha
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN each venta's cost is summed using its correct source (items or fallback)
- AND the total `ventas_costo_total` is the correct aggregate

---

### R23: Supabase RPC `rpc_analisis_mensual`

The system SHALL create `public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE) RETURNS jsonb LANGUAGE plpgsql SECURITY INVOKER STABLE`.

The function SHALL compute CORE financial indicators for the given month by reading from `resumen_diario` and `gastos_operativos`.

| Indicator | Key | Source |
|-----------|-----|--------|
| Monthly sales | `ventas_mes` | `resumen_diario.ventas_monto_total` SUM |
| Monthly collections | `cobros_mes` | `resumen_diario.cobros_monto_total` SUM |
| Monthly cost | `costo_mes` | `resumen_diario.ventas_costo_total` SUM |
| Monthly expenses | `gastos_mes` | `gastos_operativos.monto` SUM |
| Pending balance | `saldo_pendiente` | Latest `resumen_diario.saldo_pendiente_total` |
| Net margin % | `margen_neto_pct` | `(ventas - costos - gastos) / ventas * 100` |
| Average ticket | `ticket_promedio` | `ventas_mes / cantidad_ventas` |
| Sales count | `cantidad_ventas` | `resumen_diario.ventas_cantidad` SUM |
| Previous month sales | `ventas_mes_anterior` | `resumen_diario.ventas_monto_total` SUM for previous month |
| Sales variation % | `variacion_ventas_pct` | `(ventas - anterior) / anterior * 100` |
| Historical months | `meses_historicos` | `COUNT(DISTINCT DATE_TRUNC('month', fecha))` from `resumen_diario` for `p_optica_id` |

(Previously: 10 indicators, no `meses_historicos`.)

The function SHALL ALSO compute inline:

- **`margen_por_categoria`** (JSONB array): revenue per category from `dispensaciones` + `servicios_extra`, mapping `tipo_lente + material_lente` to `categoria_producto_id` via CASE. Each row SHALL include `categoria`, `ventas`, `costos=0`, `margen_pct=null` (cost entry deferred to Slice 2).

- **`stock_estancado`** (JSONB array): unsold products from `monturas` LEFT JOIN `montura_movimientos (tipo='SALIDA_VENTA')` and `dispensaciones.montura_id`. `dias_sin_venta` SHALL be `CURRENT_DATE - MAX(fecha)` for sold, 999 for never-sold. `ultima_venta` SHALL be the real date or null. The low-stock filter (`stock_actual <= stock_minimo`) SHALL be removed.

(Previously: not computed by this RPC; `stock_estancado` used low-stock filter with hardcoded 999.)

#### R23.1: RPC Security

`REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon; GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;`

#### Scenario: margen_por_categoria returns real revenue from inline computation

- GIVEN dispensaciones with `tipo_lente='monofocal', material_lente='resina_stock'` for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `margen_por_categoria` contains a row with non-zero `ventas` for the mapped `categoria_producto_id`
- AND the mapping from `(tipo_lente, material_lente)` to `categoria` follows the CASE expression

#### Scenario: stock_estancado shows computed dias_sin_venta for sold monturas

- GIVEN a montura with a SALIDA_VENTA movimiento on 2026-03-15
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura appears in `stock_estancado` with real `diasSinVenta` and `ultimaVenta = "2026-03-15"`

#### Scenario: never-sold montura shows 999 days and null date

- GIVEN a montura with no SALIDA_VENTA and no `dispensaciones.montura_id` reference
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura has `diasSinVenta = 999` and `ultimaVenta = null`

#### Scenario: no sales data returns zero rows in margen_por_categoria

- GIVEN an optica has zero dispensaciones and zero servicios_extra for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `margen_por_categoria` contains 9 rows (one per `categorias_producto`) with `ventas = 0, costos = 0, margen_pct = null`

#### Scenario: meses_historicos returns correct count

- GIVEN `resumen_diario` has rows for 5 distinct months (2026-03 through 2026-07) for optica 'o1'
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN the returned JSON includes `meses_historicos = 5`

#### Scenario: meses_historicos counts only months with data

- GIVEN `resumen_diario` has zero rows for optica 'o1' (never synced or calculated)
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN `meses_historicos = 0`

---

## ADDED Requirements

### R32: ProyeccionCaja — mesesHistoricos Field

The Android domain model `ProyeccionCaja` (in `domain/`) SHALL gain a field `mesesHistoricos: Int` with default value `0`.

The field SHALL be populated from the `meses_historicos` value returned by `rpc_analisis_mensual`. The RPC response JSONB deserializer SHALL map `"meses_historicos"` to `ProyeccionCaja.mesesHistoricos`.

#### Scenario: RPC response with meses_historicos

- GIVEN `rpc_analisis_mensual` returns `{"meses_historicos": 5, ...}`
- WHEN the response is deserialized to `ProyeccionCaja`
- THEN `proyeccionCaja.mesesHistoricos == 5`

#### Scenario: Default when missing from response

- GIVEN `rpc_analisis_mensual` returns a JSON without the `meses_historicos` key (backward compatibility)
- WHEN the response is deserialized to `ProyeccionCaja`
- THEN `proyeccionCaja.mesesHistoricos == 0`

---

### R33: ProyeccionCard — Data-Depth Warning

The `ProyeccionCard` composable SHALL display a warning banner when `mesesHistoricos < 3`.

The warning SHALL contain user-facing text indicating that projections are based on limited data (fewer than 3 months). When `mesesHistoricos >= 3`, no warning SHALL be displayed. The decision SHALL be driven by the `ProyeccionCaja.mesesHistoricos` value — no separate RPC or query is needed.

#### Scenario: Warning shown for insufficient data

- GIVEN `ProyeccionCaja.mesesHistoricos == 1` (only 1 month of data)
- WHEN `ProyeccionCard` renders
- THEN a warning banner is visible with text referencing limited data depth

#### Scenario: No warning when data is sufficient

- GIVEN `ProyeccionCaja.mesesHistoricos == 5` (5 months of data)
- WHEN `ProyeccionCard` renders
- THEN no warning banner is shown

#### Scenario: Edge case — exactly 2 months

- GIVEN `ProyeccionCaja.mesesHistoricos == 2`
- WHEN `ProyeccionCard` renders
- THEN a warning banner is visible (2 < 3)

#### Scenario: Edge case — exactly 3 months

- GIVEN `ProyeccionCaja.mesesHistoricos == 3`
- WHEN `ProyeccionCard` renders
- THEN no warning banner is shown (3 >= 3)
