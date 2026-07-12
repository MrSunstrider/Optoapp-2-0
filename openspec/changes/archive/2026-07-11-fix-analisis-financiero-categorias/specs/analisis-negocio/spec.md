# Delta for analisis-negocio

## MODIFIED Requirements

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
