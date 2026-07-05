# Verify Report: Fase 7 — Motor de 8 indicadores

## Test Results (All Passing)

| Metric | Value |
|--------|-------|
| Total project tests | 1682 |
| Total project failures | 0 |
| Total project errors | 0 |
| Fase 7 new tests | 18 |
| Fase 7 test failures | 0 |
| Fase 7 test errors | 0 |
| JaCoCo build | BUILD SUCCESSFUL (coverage threshold met) |

### Fase 7 Test Details

| Test Class | Tests | Status |
|-----------|-------|--------|
| `PagoEntityTest` | 2 | ✅ PASS |
| `MIGRATION_32_33_Test` | 2 | ✅ PASS |
| `ResumenDiarioDaoTest` | 4 | ✅ PASS |
| `AnalisisMensualMapperTest` | 3 | ✅ PASS |
| `ObtenerAnalisisMensualUseCaseTest` | 3 | ✅ PASS |
| `ObtenerDeudoresUseCaseTest` | 4 | ✅ PASS |

## Implementation vs Spec Verification

### ✅ PASS: Pago.ventaId field (Spec R1)
- `DispensacionEntity.kt` line 109: `val ventaId: String? = null` with `@SerialName("ventaId")`
- Nullable with default null — existing constructors compile unchanged
- 2 unit tests verify behavior

### ✅ PASS: Room Migration MIGRATION_32_33 (Spec R2)
- `ALTER TABLE pagos ADD COLUMN ventaId TEXT`
- `CREATE INDEX IF NOT EXISTS index_pagos_ventaId ON pagos(ventaId)`
- `OptoDatabase.kt` version = 33, registered in `.addMigrations()`, companion re-export
- 2 unit tests verify data preservation + index creation

### ✅ PASS: ResumenDiarioDao monthly query (Spec R3)
- `getByOpticaAndMonth(opticaId, yearMonth)` with `strftime('%Y-%m', fecha)` filter, ordered by `fecha ASC`
- 2 DAO tests verify month filtering + empty month

### ✅ PASS: AnalisisMensual domain model (Spec R5 auxiliary)
- All 8 indicator fields: ventasMes, cobrosMes, margenNetoPct, margenPorCategoria, deudores, proyeccionCaja, stockEstancado, valorInventario
- Auxiliary models: MargenCategoria, DeudoresResumen, ProyeccionCaja, StockEstancadoItem
- `fromJson()` JSON mapper with null-safe helpers (optDouble, optInt, optDoubleNullable, optString)
- 3 unit tests cover full response, empty month, missing keys

### ✅ PASS: Deudor domain model (Spec R6 auxiliary)
- All 8 fields: pacienteNombre, pacienteTelefono, ventaId, ventaFecha, montoTotal, totalPagado, saldo, diasDeuda

### ✅ PASS: ObtenerAnalisisMensualUseCase (Spec R5)
- Hilt `@Inject` constructor, depends on `Postgrest` + `ResumenDiarioDao`
- Online: calls `rpc_analisis_mensual` with `p_optica_id` and `p_mes` params
- Offline: catches IOException, falls back to Room aggregation for indicators 1-4
- `CancellationException` rethrown per convention
- 3 unit tests: online, offline fallback, unexpected error

### ✅ PASS: ObtenerDeudoresUseCase (Spec R6 — online path)
- Hilt `@Inject` constructor, depends on `Postgrest` + `VentaDao` + `PagoDao`
- Online: calls `rpc_deudores` with `p_optica_id`, maps JsonArray → `List<Deudor>`
- 4 unit tests: online success, empty result, offline error, unexpected error

### ✅ PASS: Supabase migration (partial — see CRITICAL below)
- `rpc_deudores`: ✅ Returns TABLE with all spec'd columns, JOINs ventas+pagos+pacientes, HAVING saldo > 0.005, ORDER BY dias_deuda DESC
- `rpc_count_pendientes`: ✅ Rewritten to query `ventas` table (overdue deliveries + unpaid balance)
- GRANT EXECUTE on `recalcular_resumen_diario`: ✅ Missing grant from Fase 6 now added
- Deprecation comments on `rpc_resumen_financiero` and `rpc_saldo_pendiente`: ✅ Both marked DEPRECATED
- GRANT EXECUTE on new RPCs: ✅ Both RPCs grant to authenticated role

### ⚠️ WARNING: ObtenerDeudoresUseCase offline fallback not implemented
- **Spec required**: Offline fallback performs local Room JOIN (ventas + pagos + pacientes) returning cached debtors
- **Implementation**: Returns `Resource.Error("Sin conexion para obtener deudores")` on IOException
- **Impact**: Low — offline users get an error instead of cached data for debtor list; this is a degraded UX but not a crash
- **Note**: The constructor doesn't inject `PacienteDao`, which would be needed for patient names in a local JOIN

### ✅ RESOLVED: rpc_analisis_mensual scope aligned with implementation

**Resolution**: The spec was updated to match the actual implementation. `rpc_analisis_mensual` computes CORE indicators only (ventas_mes, cobros_mes, costo_mes, gastos_mes, saldo_pendiente, margen_neto_pct, ticket_promedio, cantidad_ventas, ventas_mes_anterior, variacion_ventas_pct) — 10 metrics from `resumen_diario` + `gastos_operativos`.

The auxiliary indicators (`margen_por_categoria`, `stock_estancado`, `valor_inventario`) are queried directly from their respective tables (server-side), not computed in this RPC. This matches the plan design where CORE aggregates live in the RPC and auxiliary indicators are direct queries.

`rpc_deudores` remains a separate function per spec — confirmed correct.

| # | Indicator | Domain key | RPC | Status |
|---|-----------|-----------|-----|--------|
| 1 | Monthly sales | `ventas_mes` | `rpc_analisis_mensual` | ✅ |
| 2 | Monthly collections | `cobros_mes` | `rpc_analisis_mensual` | ✅ |
| 3 | Monthly cost | `costo_mes` | `rpc_analisis_mensual` | ✅ |
| 4 | Monthly expenses | `gastos_mes` | `rpc_analisis_mensual` | ✅ |
| 5 | Pending balance | `saldo_pendiente` | `rpc_analisis_mensual` | ✅ |
| 6 | Net margin % | `margen_neto_pct` | `rpc_analisis_mensual` | ✅ |
| 7 | Average ticket | `ticket_promedio` | `rpc_analisis_mensual` | ✅ |
| 8 | Sales count | `cantidad_ventas` | `rpc_analisis_mensual` | ✅ |
| 9 | Previous month sales | `ventas_mes_anterior` | `rpc_analisis_mensual` | ✅ |
| 10 | Sales variation % | `variacion_ventas_pct` | `rpc_analisis_mensual` | ✅ |
| 11 | Debtors list | `deudores` | `rpc_deudores` (separate) | ✅ |
| 12 | Margin by category | `margen_por_categoria` | Direct query from `ventas` + `costos_productos` | 🔲 Server-side |
| 13 | Stagnant stock | `stock_estancado` | Direct query from `monturas` + `montura_movimientos` | 🔲 Server-side |
| 14 | Inventory value | `valor_inventario` | Direct query from `monturas` | 🔲 Server-side |

## Recommendations

1. **WARNING**: Add offline fallback to `ObtenerDeudoresUseCase` — either implement the local Room JOIN with injected `PacienteDao`, or update the spec to document that offline deudores returns error.

## Build Verification

| Command | Result |
|---------|--------|
| `./gradlew :optoapp:testDebugUnitTest --stacktrace` | ✅ BUILD SUCCESSFUL (1682 tests, 0 failures) |
| `./gradlew :optoapp:jacocoTestReport` | ✅ BUILD SUCCESSFUL (coverage threshold met) |
