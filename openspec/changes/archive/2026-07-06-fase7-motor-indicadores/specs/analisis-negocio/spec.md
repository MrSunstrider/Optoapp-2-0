# Delta Spec: analisis-negocio — Fase 7 Motor de Indicadores

## ADDED Requirements

### Requirement: rpc_analisis_mensual

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

> **NOTE**: `margen_por_categoria`, `stock_estancado`, and `valor_inventario` are queried directly from their respective tables (server-side), not computed in this RPC. This matches the plan design where CORE indicators live in the RPC and auxiliary indicators are direct queries.

#### Scenario: Typical month
- GIVEN an optica has resumen_diario data and gastos for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN a JSONB object is returned with all 10 CORE keys as non-null values
- AND `ventas_mes` matches monthly SUM from resumen_diario

#### Scenario: Empty month
- GIVEN an optica has no data for a month
- WHEN `rpc_analisis_mensual('o1', '2026-08-01')` is called
- THEN all numeric values return 0
- AND `margen_neto_pct` returns 0 (not error)

### Requirement: rpc_deudores

The system SHALL create `public.rpc_deudores(p_optica_id TEXT) RETURNS TABLE(paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE, monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER) LANGUAGE sql SECURITY INVOKER STABLE`.

JOIN `ventas` LEFT JOIN `pagos` LEFT JOIN `pacientes`, HAVING `saldo > 0.005`, ORDER BY `dias_deuda DESC`.

#### Scenario: Returns debtors by aging
- GIVEN 3 ventas with partial payments, oldest 60 days ago
- WHEN `rpc_deudores('o1')` is called
- THEN 3 rows returned ordered by `dias_deuda` DESC
- AND each has `saldo > 0`

#### Scenario: No debtors
- GIVEN all ventas fully paid
- WHEN `rpc_deudores('o1')` is called
- THEN empty result set returned

### Requirement: GRANT EXECUTE on new RPCs

`GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual TO authenticated` and `GRANT EXECUTE ON FUNCTION public.rpc_deudores TO authenticated`.

#### Scenario: Authenticated user can call
- GIVEN user is authenticated
- WHEN calling either new RPC
- THEN it returns data without permission error

### Requirement: Update rpc_count_pendientes to use ventas

Rewrite `rpc_count_pendientes` to query `public.ventas` instead of old `dispensaciones` + `servicios_extra`.

#### Scenario: Count matches ventas
- GIVEN 5 ventas with pending balance
- WHEN `rpc_count_pendientes('o1')` is called
- THEN count matches pending from `ventas` table

### Requirement: Deprecate rpc_resumen_financiero and rpc_saldo_pendiente

Add deprecation comment `-- DEPRECATED: Use rpc_analisis_mensual instead` to both functions. Functions remain callable (no DROP).
(Reason: Replaced by new `rpc_analisis_mensual` which computes all indicators in one call. Migration: old callers can safely continue; migrate to new RPC when convenient.)

#### Scenario: Deprecated RPCs still execute
- GIVEN deprecation migration applied
- WHEN old RPC is called
- THEN it executes normally (no error)
- AND function body has deprecation comment

## MODIFIED Requirements

### Requirement: Supabase RPC `recalcular_resumen_diario` (Previously R9)

Full existing text at `openspec/specs/analisis-negocio/spec.md` R9–R9.3.

Add to R9.2 (security):
```sql
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario TO authenticated;
```
(Previously: the GRANT was missing from the Fase 6 migration, causing permission errors on client-side calls.)

#### Scenario: Authenticated caller succeeds
- GIVEN recalcular_resumen_diario exists
- WHEN an authenticated user calls it
- THEN the function executes without permission error

### Requirement: Room `ResumenDiarioDao` monthly aggregation (Previously R13)

Add to R13.1 method table:

| Method | Return | Description |
|--------|--------|-------------|
| `getByOpticaAndMonth(opticaId, yearMonth)` | `suspend fun`: `List<ResumenDiarioEntity>` | Filter by `fecha` in given YYYY-MM |

The existing `getByOpticaAndDateRange` method remains unchanged.

#### Scenario: Monthly filter works
- GIVEN 30 daily rows for July 2026 in Room
- WHEN `getByOpticaAndMonth('o1', '2026-07')` is called
- THEN 30 rows returned for client-side aggregation
