# Delta for analisis-negocio

## ADDED Requirements

### Requirement: AnalisisNegocio role gate waits for auth

`AnalisisNegocioScreen` MUST collect the current user role with `collectAsState(initial = null)` (or equivalent nullable initial). While role is null, the screen MUST show a loading/progress state and MUST NOT render admin/gerente content or an unauthorized flash. After role resolves, unauthorized roles MUST be denied; authorized roles MUST proceed.

#### Scenario: Null role shows progress not admin content

- GIVEN auth role has not resolved yet
- WHEN `AnalisisNegocioScreen` is composed
- THEN a progress/loading indicator MUST be visible
- AND admin-only business analysis content MUST NOT be visible
- AND an unauthorized denial MUST NOT flash before role resolution

#### Scenario: Unauthorized role after resolve

- GIVEN role resolves to a non-admin/non-gerente value
- WHEN the screen recomposes
- THEN authorized Análisis content MUST NOT be shown
- AND the unauthorized path MUST be shown

### Requirement: Gastos del mes list uses gastosMes

The "Gastos del mes" card MUST derive both its total and its item list from the month-filtered `gastosMes` collection. The unfiltered `gastos` list MUST NOT drive the month card's rows.

#### Scenario: Month card lists only month gastos

- GIVEN `gastos` contains items outside the selected month and `gastosMes` is the filtered subset
- WHEN the "Gastos del mes" section renders
- THEN row count and contents MUST match `gastosMes`
- AND `totalGastos` MUST equal the sum of `gastosMes` amounts

### Requirement: ResumenDiarioDao deleteAll clears cache

`ResumenDiarioDao` MUST expose `suspend fun deleteAll()` that deletes all rows from the local `resumen_diario` table. Tests MUST invoke the DAO method (not raw SQL alone) to prove the API.

#### Scenario: deleteAll removes all resumen_diario rows

- GIVEN one or more `ResumenDiarioEntity` rows in the in-memory database
- WHEN `dao.deleteAll()` is called
- THEN subsequent reads MUST return an empty list / no rows

### Requirement: proyeccion_caja retains egresos when unpaid is zero

Within `rpc_analisis_mensual`, `proyeccion_caja` (or equivalent `v_proyeccion` JSON) MUST always include `egresos_programados` (and coherent `ingresos` / `saldo_neto` fields) even when no unpaid ventas match the unpaid filter. A NULL `SELECT ... INTO` result MUST NOT drop egresos. Implementation SHOULD use `COALESCE` / scalar subqueries so egresos survive zero unpaid rows.

#### Scenario: Zero unpaid ventas still returns egresos_programados

- GIVEN an optica with scheduled egresos but zero unpaid ventas above the unpaid threshold
- WHEN `rpc_analisis_mensual` runs for that month
- THEN `proyeccion_caja` MUST be non-null JSON
- AND `egresos_programados` MUST reflect computed egresos (not omitted due to NULL proyeccion)

## MODIFIED Requirements

### Requirement: REQ-2 User-Facing Error Messages Are Static

Every domain-layer use case that produces a user-facing error message MUST return a static, user-friendly string. Raw exception text, JSON bodies, and `e.localizedMessage` MUST NOT appear in the error message visible to the UI. This MUST apply to Análisis use cases (`ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`) and to finanzas sync (`SyncFinanzasUseCase`) and cierre presentation (`CierreCajaViewModel` load errors) as covered by their domain deltas.

The full exception (message + stack trace) MUST be logged via `Log.e` to Logcat for diagnostics.

(Previously: REQ-2 only illustrated Análisis use cases; SyncFinanzas and Cierre still interpolated exception text.)

#### Scenario: Supabase RPC failure shows static message

```
GIVEN the Supabase RPC `rpc_analisis_mensual` returns HTTP 400 with verbose JSON body
 WHEN `ObtenerAnalisisMensualUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the message does NOT contain "400", "JSON", "localizedMessage", or any raw exception text
  AND the full exception is written to Logcat via `Log.e`
```

#### Scenario: Network failure in deudores shows static message

```
GIVEN the Supabase RPC `rpc_deudores` throws a network exception
 WHEN `ObtenerDeudoresUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the full exception is written to Logcat via `Log.e`
```

### Requirement: R23 Supabase RPC rpc_analisis_mensual stock_estancado restoration

The system SHALL keep `public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE)` returning JSONB with the 16 core keys. Field `stock_estancado` MUST be restored to the R23 CTE behavior from migration `20260709000003_fix_analisis_mensual_categorias.sql`: real `ultima_venta` / `dias_sin_venta` from `montura_movimientos` (`SALIDA_VENTA`) and `dispensaciones.montura_id`; never-sold → `dias_sin_venta = 999` and null date; low-stock filter (`stock_actual <= stock_minimo`) MUST NOT apply. A new migration MUST `CREATE OR REPLACE` the current function body (preserving pago_effect / converge paths) and only patch stock + proyeccion sections.

(Previously: converge migration hardcoded `ultima_venta=NULL`, `dias_sin_venta=999`, and filtered `stock_actual <= stock_minimo`.)

#### Scenario: stock_estancado shows computed dias_sin_venta for sold monturas

- GIVEN a montura with a SALIDA_VENTA movimiento on 2026-03-15
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura appears in `stock_estancado` with real `diasSinVenta` and `ultimaVenta = "2026-03-15"`

#### Scenario: never-sold montura shows 999 days and null date

- GIVEN a montura with no SALIDA_VENTA and no `dispensaciones.montura_id` reference
- WHEN `rpc_analisis_mensual('o1', '2026-07-01')` is called
- THEN that montura has `diasSinVenta = 999` and `ultimaVenta = null`

#### Scenario: stock_estancado includes above-minimo stock

- GIVEN an active montura with `stock_actual > stock_minimo` and `stock_actual > 0`
- WHEN `rpc_analisis_mensual` is called
- THEN that montura MUST still be eligible for `stock_estancado` (no low-stock filter)
