# Delta Spec: Fase 7 — Motor de 8 Indicadores

## Domain: analisis-negocio (Modified)

### ADDED Requirements

#### Requirement: rpc_analisis_mensual

The system SHALL create `public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE) RETURNS jsonb LANGUAGE plpgsql SECURITY INVOKER STABLE`.

The function SHALL compute CORE financial indicators for the given month by reading aggregated data from `resumen_diario` and `gastos_operativos`.

| Indicator | Domain key | Source |
|-----------|-----------|--------|
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

> **NOTE**: `margen_por_categoria`, `stock_estancado`, and `valor_inventario` are NOT computed by this RPC. They are queried directly from their respective tables (server-side) — `margen_por_categoria` from `ventas` + `costos_productos` per `categoria_producto_id`, `stock_estancado` from `monturas` with `montura_movimientos`, and `valor_inventario` from `SUM(monturas.costo * stock_actual)`. This matches the plan: CORE indicators in the RPC, auxiliary indicators as direct queries.

##### Scenario: Typical month returns all CORE indicators

- GIVEN an optica has resumen_diario data and gastos for July 2026
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN a JSONB object is returned with all 10 CORE keys as non-null numeric values
- AND `ventas_mes` equals the aggregated monthly sum from `resumen_diario`

##### Scenario: Empty month gracefully handled

- GIVEN an optica has no data for a month
- WHEN `rpc_analisis_mensual('o1', '2026-08-01')` is called
- THEN all numeric values return 0
- AND `margen_neto_pct` returns 0 (not NULL or error)

#### Requirement: rpc_deudores

The system SHALL create `public.rpc_deudores(p_optica_id TEXT) RETURNS TABLE(paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE, monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER) LANGUAGE sql SECURITY INVOKER STABLE`.

The function SHALL JOIN `ventas` LEFT JOIN `pagos` LEFT JOIN `pacientes`, filter `HAVING v.monto_total - COALESCE(SUM(pg.monto), 0) > 0.005`, and ORDER BY `dias_deuda DESC`.

##### Scenario: Returns debtors with correct aging

- GIVEN an optica has 3 ventas with partial payments, oldest 60 days ago
- WHEN `rpc_deudores('o1')` is called
- THEN rows are returned ordered by `dias_deuda` DESC
- AND each row has `saldo > 0`
- AND the oldest debtor appears first

##### Scenario: No debtors returns empty set

- GIVEN all ventas are fully paid
- WHEN `rpc_deudores('o1')` is called
- THEN an empty result set is returned

#### Requirement: GRANT EXECUTE on new RPCs

The system SHALL execute `GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual TO authenticated` and `GRANT EXECUTE ON FUNCTION public.rpc_deudores TO authenticated`.

##### Scenario: Authenticated user can call new RPCs

- GIVEN a user is authenticated with Supabase
- WHEN they call `rpc_analisis_mensual` or `rpc_deudores`
- THEN the RPC returns data without permission error

#### Requirement: Update rpc_count_pendientes

The system SHALL rewrite `rpc_count_pendientes` to query `public.ventas` instead of old `dispensaciones` + `servicios_extra`.

##### Scenario: Count matches ventas table

- GIVEN an optica has 5 ventas with pending balance
- WHEN `rpc_count_pendientes('o1')` is called
- THEN the count matches `WHERE monto_total - COALESCE(total_pagado, 0) > 0.005` from `ventas`

#### Requirement: Deprecate rpc_resumen_financiero and rpc_saldo_pendiente

The system SHALL add a deprecation comment to both functions: `-- DEPRECATED: Use rpc_analisis_mensual instead`. The functions SHALL NOT be dropped — they remain callable for backward compatibility.
(Previously: both were active RPCs for financial summaries)

##### Scenario: Deprecated RPCs still function

- GIVEN the deprecation migration has been applied
- WHEN a caller invokes `rpc_resumen_financiero` or `rpc_saldo_pendiente`
- THEN the function executes normally (no error)
- AND the function body has an internal comment marking it deprecated

### MODIFIED Requirements

#### Requirement: Supabase RPC `recalcular_resumen_diario` (Previously R9)

Full existing text at `openspec/specs/analisis-negocio/spec.md#R9`. Add to R9.2:

The migration SHALL also execute:
```sql
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario TO authenticated;
```

(Previously: the function existed but the GRANT was missing, making client-side calls fail with permission error)

##### Scenario: Added scenario — authenticated user can call recalcular

- GIVEN recalcular_resumen_diario exists
- WHEN an authenticated user calls it
- THEN the function executes without permission error

#### Requirement: Room `ResumenDiarioDao` monthly aggregation (Previously R13)

Add two new methods to the existing DAO table in R13.1:

| Method | Return | Description |
|--------|--------|-------------|
| `getByOpticaAndMonth(opticaId, yearMonth)` | `suspend fun`: `List<ResumenDiarioEntity>` | Filtered by `strftime('%Y-%m', fecha) = yearMonth` |
| (existing `getByOpticaAndDateRange` kept unchanged) | | |

##### Scenario: Monthly aggregation returns correct SUM

- GIVEN ResumenDiarioDao has 30 daily rows for July 2026
- WHEN `getByOpticaAndMonth('o1', '2026-07')` is called
- THEN all 30 rows are returned
- AND the caller can compute SUM(ventasMontoTotal) from the list

## Domain: indicadores-negocio (New — Full Spec)

### Purpose

Android-side business indicator engine: Room entities, DAO queries, and UseCases that fetch the 8 indicators from Supabase RPCs (online) or fall back to local Room aggregation (offline).

### Requirements

#### Requirement: Pago entity gains ventaId

The `Pago` Room entity SHALL add:
```kotlin
val ventaId: String? = null
```
- Column name in Room: `ventaId` (camelCase per project convention)
- The field SHALL be nullable
- Existing constructors and usages SHALL continue to compile (default null)

##### Scenario: ventaId field exists in Pago

- GIVEN the Pago entity exists in the database
- WHEN the table `pagos` is inspected
- THEN a column `ventaId` of type TEXT exists, nullable
- AND existing rows have NULL in `ventaId`

#### Requirement: Room migration v32→v33

A migration `MIGRATION_32_33` SHALL exist:
1. `ALTER TABLE pagos ADD COLUMN ventaId TEXT`
2. `CREATE INDEX IF NOT EXISTS index_pagos_ventaId ON pagos(ventaId)`

The database version SHALL be bumped from 32 to 33. The migration SHALL be registered in `.addMigrations()`.

##### Scenario: Migration preserves existing data

- GIVEN a device has OptoDatabase at version 32 with 50 pago rows
- WHEN MIGRATION_32_33 runs
- THEN all 50 pago rows are preserved
- AND `ventaId` column exists with NULL for all existing rows

#### Requirement: ResumenDiarioDao monthly aggregation for offline fallback

The DAO SHALL add `getByOpticaAndMonth(opticaId: String, yearMonth: String): suspend fun List<ResumenDiarioEntity>`.

This enables offline calculation of indicators 1–4 (ventas, cobros, margen) from local Room data when Supabase RPC is unreachable.

##### Scenario: Local aggregation works offline

- GIVEN the device is offline and ResumenDiarioDao has 30 daily cached rows
- WHEN `getByOpticaAndMonth('o1', '2026-07')` is called
- THEN the 30 rows are returned for client-side SUM computation

#### Requirement: getDeudores query (Room)

The system SHALL create a DAO query that returns debtors by JOINing `ventas` + `pagos` + `pacientes` locally:
```sql
SELECT v.id AS ventaId, v.fecha, v.montoTotal,
       COALESCE(SUM(p.monto), 0) AS totalPagado,
       v.montoTotal - COALESCE(SUM(p.monto), 0) AS saldo,
       p2.nombreCompleto, p2.telefono
FROM ventas v
LEFT JOIN pagos p ON p.ventaId = v.id
LEFT JOIN pacientes p2 ON p2.id = v.pacienteId
WHERE v.opticaId = :opticaId
GROUP BY v.id
HAVING saldo > 0.005
ORDER BY v.fecha ASC
```

##### Scenario: Room deudores query matches RPC

- GIVEN local Room has 3 ventas with partial payments synced
- WHEN the deudores query is executed
- THEN 3 rows with positive saldo are returned, ordered by oldest first

#### Requirement: ObtenerAnalisisMensualUseCase

A new Hilt-annotated UseCase SHALL exist at `domain/ObtenerAnalisisMensualUseCase.kt`:

```kotlin
class ObtenerAnalisisMensualUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val resumenDiarioDao: ResumenDiarioDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<AnalisisMensual>
}
```

Behavior:
- WHEN online: calls `supabase.postgrest.rpc("rpc_analisis_mensual")` with parameters
- WHEN offline (IOException): falls back to `resumenDiarioDao.getByOpticaAndMonth()` + client-side aggregation for indicators 1–4
- RETURNS `Resource.Success<AnalisisMensual>` on success, `Resource.Error` on unexpected failure

##### Scenario: Online call succeeds

- GIVEN the device has network connectivity
- WHEN `invoke('o1', '2026-07-01')` is called
- THEN Supabase RPC is invoked
- AND the response is mapped to an `AnalisisMensual` domain model

##### Scenario: Offline fallback to Room

- GIVEN the device has no network
- WHEN `invoke('o1', '2026-07-01')` is called
- THEN the RPC call fails with IOException
- AND the UseCase falls back to `resumenDiarioDao.getByOpticaAndMonth()`
- AND indicators 5–8 return 0 or empty with a flag indicating "offline — limited data"

#### Requirement: ObtenerDeudoresUseCase

A new Hilt-annotated UseCase SHALL exist:

```kotlin
class ObtenerDeudoresUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val ventaDao: VentaDao,
    private val pagoDao: PagoDao
) {
    suspend operator fun invoke(opticaId: String): Resource<List<Deudor>>
}
```

Behavior:
- WHEN online: calls `rpc_deudores` and maps to `List<Deudor>`
- WHEN offline: queries local Room (ventas + pagos + pacientes JOIN)
- RETURNS `Resource.Success` or `Resource.Error`

##### Scenario: Online returns debtor list

- GIVEN the device is online and optica has 2 debtors
- WHEN `invoke('o1')` is called
- THEN RPC is called and 2 `Deudor` domain objects are returned
- AND each Deudor has: nombre, telefono, ventaId, saldo, diasDeuda

##### Scenario: Offline still returns data from local JOIN

- GIVEN the device is offline but Room has synced ventas + pagos
- WHEN `invoke('o1')` is called
- THEN RPC fails with IOException
- AND the UseCase falls back to local Room JOIN query
- AND debtors from local cache are returned
