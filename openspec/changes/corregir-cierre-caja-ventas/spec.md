# Delta Spec: Corregir Cierre de Caja, Reportes, BI y Fix Anulaciones

## Purpose

This delta spec describes the changes REQUIRED to migrate `CierreCajaViewModel`, `ReportesViewModel`, `BIViewModel`, and `rpc_resumen_financiero` from querying `dispensaciones` + `servicios_extra` tables independently to querying the canonical `ventas` table (ledger) as the sole revenue source. It also fixes the anulaciones bug where reversal payments used `DateUtils.today()` instead of the original payment's date.

All requirements below SHALL supplement or replace the corresponding requirements in `openspec/specs/cierre-caja/spec.md` and `openspec/specs/reportes-financieros/spec.md`. Unchanged requirements from those specs REMAIN in effect.

## Modified Requirements

### REQ-CC-1: CierreCajaViewModel — totales desde ventas

**Replaces**: `openspec/specs/cierre-caja/spec.md` requirement "Servicios Extra in Cierre Totals" (lines 49–64).

`CierreCajaViewModel` SHALL query `VentaDao.getVentasByOpticaAndDateRange()` instead of `repository.getAllDispensacionesForOptica()` and `repository.getAllServiciosForOptica()`. The `totalVentasHoy` field MUST be computed as the sum of `venta.montoTotal` for rows where `venta.fecha == fecha` (regardless of `origen` value). `totalServiciosExtra` MUST be computed as the sum of `venta.montoTotal` for rows where `venta.fecha == fecha` AND `venta.origen == "servicio_extra"`. `totalGeneral` MUST remain `totalVentasHoy + totalServiciosExtra`.

`serviciosExtraHoy: List<ServicioExtra>` is REMOVED from `CierreCajaUiState`. A new field `serviciosExtraHoy: List<Venta>` SHALL hold the `Venta` rows with `origen == "servicio_extra"`.

The breakdown list in the UI (currently `List<ServicioExtra>`) SHALL display `Venta.montoTotal` and `Venta.id` (origenId description derived from the linked servicio extra) for each servicio-extra-origin venta.

#### Scenario: Mixed ventas from dispensacion and servicio_extra origins

- GIVEN today's `ventas` table has 2 rows for opticaId "o1":
  - origen="dispensacion", montoTotal=300.0, fecha=today
  - origen="servicio_extra", montoTotal=150.0, fecha=today
- AND today's pagos sum = 200.0
- WHEN `CierreCajaViewModel` emits
- THEN `totalVentasHoy` MUST be 300.0
- AND `totalServiciosExtra` MUST be 150.0
- AND `totalGeneral` MUST be 450.0
- AND `saldoPendiente` MUST be 250.0

#### Scenario: Only dispensacion-origin ventas today

- GIVEN today's `ventas` table has 1 row for opticaId "o1":
  - origen="dispensacion", montoTotal=300.0, fecha=today
- AND no servicio_extra-origin rows exist for today
- WHEN `CierreCajaViewModel` emits
- THEN `totalVentasHoy` MUST be 300.0
- AND `totalServiciosExtra` MUST be 0.0
- AND `totalGeneral` MUST be 300.0

#### Scenario: Servicios extra hoy list comes from Venta rows

- GIVEN today's `ventas` table has 2 servicio_extra-origin rows for opticaId "o1":
  - montoTotal=80.0 and montoTotal=70.0
- WHEN `CierreCajaViewModel` emits
- THEN `uiState.serviciosExtraHoy` MUST contain exactly 2 entries
- AND each entry MUST be of type `Venta` with `origen == "servicio_extra"`

#### Scenario: Pago classification uses Venta fecha via venta_id

- GIVEN today's pagos include a pago with `ventaId = "v1"`
- AND venta "v1" has `fecha < today` (e.g., yesterday)
- WHEN `CierreCajaViewModel` classifies the pago
- THEN the pago's monto MUST contribute to `cobrosAtrasados`
- AND MUST NOT contribute to `ventasHoy`

#### Scenario: Orphan pago (no ventaId, no dispensacionId, no servicioExtraId)

- GIVEN a pago today with no `ventaId`, no `dispensacionId`, and no `servicioExtraId`
- WHEN `CierreCajaViewModel` classifies the pago
- THEN the pago's monto MUST contribute to `ventasHoy` (unchanged fallback)

---

### REQ-CC-2: CierreCajaScreen — TOTAL VENTAS DEL DÍA label

**Modifies**: `CierreCajaScreen.kt` label rendering.

The "TOTAL VENTAS DEL DÍA" label SHALL display `uiState.totalGeneral` formatted as currency. The servicios extra detail section SHALL iterate `uiState.serviciosExtraHoy` (now `List<Venta>`) instead of `List<ServicioExtra>`.

#### Scenario: Label shows totalGeneral

- GIVEN `uiState.totalGeneral = 450.0`
- WHEN `CierreCajaScreen` renders
- THEN the "TOTAL VENTAS DEL DÍA" label MUST show "$450.00" (or locale-appropriate format)

---

### REQ-RF-1: ReportesViewModel — ventas como única fuente de ingresos

**Replaces**: `openspec/specs/reportes-financieros/spec.md` requirement "Servicios Extra Inclusion in Period Totals" (lines 107–145) and the `allDispensaciones` / `allServiciosDelPeriodo` Flow definitions.

`ReportesViewModel` SHALL expose a single `allVentasDelPeriodo: StateFlow<List<Venta>>` that queries `VentaDao.getVentasByOpticaAndDateRange()` with the period date range, replacing both `allDispensaciones` and `allServiciosDelPeriodo`.

| Field | Old Formula | New Formula |
|-------|-------------|-------------|
| `totalVendido` | `Σ DispensacionOptica.montoTotal + Σ ServicioExtra.montoTotal` | `Σ Venta.montoTotal` (in-range, all `origen`) |
| `totalPagado` | `Σ DispensacionOptica.montoPagado + Σ ServicioExtra.aCuenta` | `Σ pago.monto` WHERE pago is linked to an in-range Venta via `ventaId` |

The detail list (`detalleVentasPeriodo`) SHALL render `Venta` rows with `montoTotal`, `fecha`, `origen`, and derived description.

#### Scenario: totalVendido from ventas

- GIVEN a Diario period with 3 ventas in-range: montoTotal=100.0, 200.0, 50.0
- WHEN `ReportesViewModel.totalVendido` emits
- THEN it MUST be 350.0

#### Scenario: totalPagado from pagos linked to in-range ventas

- GIVEN 2 pagos in-range each linked via `ventaId` to in-range ventas: monto=60.0, monto=40.0
- WHEN `ReportesViewModel.totalPagado` emits
- THEN it MUST be 100.0

#### Scenario: Empty period returns zero

- GIVEN no ventas and no pagos in the period
- WHEN ReportesViewModel emits
- THEN `totalVendido` MUST be 0.0 AND `totalPagado` MUST be 0.0

---

### REQ-RF-2: Cobros del Período Classification desde ventas

**Replaces**: `openspec/specs/reportes-financieros/spec.md` requirement "Cobros del Período Classification" (lines 68–101).

`cobrosPeriodo` SHALL classify each in-range pago by consulting `ventaId` and then `Venta.fecha`. The classification logic:

- If `pago.ventaId` resolves to a Venta whose `fecha` is within the period → pago is a "venta del período" (excluded from `cobrosPeriodo`).
- If `pago.ventaId` resolves to a Venta whose `fecha` is outside the period → pago is a "cobro de períodos anteriores" (included in `cobrosPeriodo`).
- If `pago.ventaId` is null, fallback to `dispensacionId`/`servicioExtraId` with existing logic (transition compatibility).
- If neither `ventaId` nor `dispensacionId`/`servicioExtraId` resolves → pago's monto contributes to `cobrosPeriodo`.

#### Scenario: Pago linked to in-range venta

- GIVEN a pago with `ventaId = "v1"` AND venta "v1" has `fecha` inside the period
- WHEN `cobrosPeriodo` classifies
- THEN this pago's `monto` MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range venta

- GIVEN a pago with `ventaId = "v1"` AND venta "v1" has `fecha` outside the period
- WHEN `cobrosPeriodo` classifies
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

#### Scenario: Pago without ventaId falls back to legacy fields

- GIVEN a pago with `ventaId == null` but with `dispensacionId = "d1"`
- AND dispensacion "d1" has `fecha` inside the period
- WHEN `cobrosPeriodo` classifies
- THEN this pago's `monto` MUST NOT contribute to `cobrosPeriodo` (legacy fallback)

---

### REQ-RF-3: ReportesViewModel detail list from ventas

`ReportesViewModel` SHALL expose `allVentasDelPeriodo: StateFlow<List<Venta>>` and the detail list in `ReportesScreen` SHALL iterate this list instead of merging separate `allDispensaciones` and `allServiciosDelPeriodo`.

#### Scenario: Detail list shows ventas only

- GIVEN a period with 2 dispensacion-origin ventas and 1 servicio_extra-origin venta
- WHEN `ReportesScreen` renders the detail LazyColumn
- THEN all 3 items MUST appear in the list

---

### REQ-BI-1: BIViewModel — recaudacionProyectada desde ventas

**Replaces**: `openspec/specs/reportes-financieros/spec.md` scenario "BI recaudacionProyectada includes servicios extra" (lines 135–139) and the BI computation in the BIViewModel's `observeStats()`.

`BIViewModel` SHALL query `VentaDao.getVentasByOpticaAndDateRange()` within the period range and compute `recaudacionProyectada` as `Σ Venta.montoTotal` for all ventas in-range (regardless of `origen`). The separate `repository.getAllServiciosForOptica()` call SHALL be removed from the BI combine chain.

`recaudacionCobrada` SHALL remain `Σ pago.monto` for pagos in-range (unchanged).

#### Scenario: recaudacionProyectada includes all ventas

- GIVEN a period with 3 ventas in-range: montoTotal=500.0, 120.0, 30.0
- WHEN `BIViewModel.uiState` emits
- THEN `recaudacionProyectada` MUST be 650.0

#### Scenario: recaudacionProyectada with mixed origins

- GIVEN a period with 1 dispensacion-origin venta (montoTotal=500.0) and 1 servicio_extra-origin venta (montoTotal=120.0)
- WHEN `BIViewModel.uiState` emits
- THEN `recaudacionProyectada` MUST be 620.0

#### Scenario: recaudacionProyectada is zero for empty period

- GIVEN no ventas in the period
- WHEN `BIViewModel.uiState` emits
- THEN `recaudacionProyectada` MUST be 0.0

---

### REQ-ANUL-1: Anulaciones — reversal date from existing pago

**Modifies**: `DispensacionRepository.deletePagoRegistrandoAnulacionEnCaja()`.

The `fechaAnulacion` parameter default `= DateUtils.today()` is REMOVED. The reversal pago's `fecha` field MUST always use `existing.fecha` (the original payment's date) so the reversal appears in the same cierre de caja period as the original payment.

#### Scenario: Anular pago from yesterday keeps yesterday as reversal date

- GIVEN an existing pago with `fecha = LocalDate.of(2026, 6, 29)` and `monto = 100.0`
- WHEN `deletePagoRegistrandoAnulacionEnCaja(pago, opticaId)` is called
- THEN the inserted reversal pago MUST have `fecha = LocalDate.of(2026, 6, 29)`
- AND `monto` MUST be -100.0
- AND `tipo` MUST be "Anulación"

#### Scenario: Anular pago with monto 0.0 does not insert reversal

- GIVEN an existing pago with `monto = 0.0`
- WHEN `deletePagoRegistrandoAnulacionEnCaja(pago, opticaId)` is called
- THEN NO reversal pago MUST be inserted
- AND the original pago MUST still be deleted

#### Scenario: Anular non-existent pago does not fail

- GIVEN no pago exists with the given ID
- WHEN `deletePagoRegistrandoAnulacionEnCaja(pago, opticaId)` is called
- THEN the call MUST NOT throw
- AND no reversal MUST be inserted

---

### REQ-RPC-1: rpc_resumen_financiero — query ventas table

**Modifies**: `supabase/migrations/20260514000000_rpc_resumen_financiero.sql`.

The RPC SHALL replace the independent `SELECT ... FROM public.dispensaciones` and `SELECT ... FROM public.servicios_extra` queries with a single `SELECT ... FROM public.ventas` query. The computed fields MUST map as follows:

| Output Field | New Query Source |
|-------------|-----------------|
| `ingresos_cobrados` | Unchanged — from `public.pagos` |
| `ventas_emitidas` | `SUM(ventas.monto_total)` WHERE `optica_id` AND `fecha` in range, EXCLUDING rows with `estado = 'Anulado'` |
| `saldo_pendiente` | `ventas_emitidas - ingresos_cobrados` (simplified; or maintain independent computation per business rules) |
| `total_movimientos` | `COUNT(*)` from `public.ventas` WHERE `optica_id` AND `fecha` in range, EXCLUDING rows with `estado = 'Anulado'` |
| `ticket_promedio` | `ventas_emitidas / total_movimientos` (or 0 if total_movimientos = 0) |

#### Scenario: RPC returns correct ventas_emitidas

- GIVEN the `ventas` table has 3 rows for optica "o1" in date range: monto_total=100, 200, 150
- WHEN `rpc_resumen_financiero('o1', '2026-07-01', '2026-08-01')` is called
- THEN the returned JSON MUST contain `ventas_emitidas = 450.0`

#### Scenario: RPC excludes anulados from ventas_emitidas

- GIVEN the `ventas` table has 3 rows for optica "o1" in date range:
  - monto_total=200, estado='Completado'
  - monto_total=150, estado='Anulado'
  - monto_total=100, estado='Completado'
- WHEN `rpc_resumen_financiero('o1', '2026-07-01', '2026-08-01')` is called
- THEN `ventas_emitidas` MUST be 300.0 (excludes the Anulado row)
- AND `total_movimientos` MUST be 2 (excludes the Anulado row)

#### Scenario: RPC returns all 6 expected fields

- GIVEN valid optica_id and date range with data
- WHEN the RPC executes
- THEN the returned JSON MUST contain exactly these 6 keys: `ingresos_cobrados`, `ventas_emitidas`, `saldo_pendiente`, `total_movimientos`, `ticket_promedio`, `fecha_inicio`, `fecha_fin_exclusiva`

---

## New Requirements

### REQ-CONSIST-1: Equivalence Test

The system MUST pass an equivalence test proving that for identical data, the new `ventas`-based computations produce the same results as the old `dispensaciones` + `servicios_extra` computations for `totalVendido`, `totalPagado`, `totalGeneral`, and `recaudacionProyectada`.

#### Scenario: Same data, same results for totalVendido

- GIVEN a Room in-memory database with identical `DispensacionOptica`, `ServicioExtra`, and `Venta` rows (where Venta.montoTotal == DispensacionOptica.montoTotal + ServicioExtra.montoTotal)
- AND CierreCajaViewModels for both old and new implementations
- WHEN both emit after seeding identical today's data
- THEN `totalGeneral` from the new implementation MUST equal `totalGeneral` from the old implementation

---

## Deprecated Requirements

The following requirements from `openspec/specs/cierre-caja/spec.md` and `openspec/specs/reportes-financieros/spec.md` are SUPERSEDED and MUST be removed or marked as deprecated:

| File | Lines | Requirement | Reason |
|------|-------|-------------|--------|
| `cierre-caja/spec.md` | 49–64 | "Servicios Extra in Cierre Totals" | Replaced by REQ-CC-1 |
| `cierre-caja/spec.md` | 31–47 | "Servicios Extra Section in Cierre de Caja" | Modified: source is now Venta, not ServicioExtra |
| `reportes-financieros/spec.md` | 107–145 | "Servicios Extra Inclusion in Period Totals" | Replaced by REQ-RF-1 |
| `reportes-financieros/spec.md` | 68–101 | "Cobros del Período Classification" | Replaced by REQ-RF-2 |
| `reportes-financieros/spec.md` | 174–184 | "Servicios Extra in Detail List" | Replaced by REQ-RF-3 |

## Test Scenarios Summary

| # | Test | GIVEN | WHEN | THEN |
|---|------|-------|------|------|
| CC-1-a | Mixed ventas totales | ventas: dispensacion 300 + servicio_extra 150, pagos 200 | ViewModel emits | totalGeneral=450, saldoPendiente=250 |
| CC-1-b | Only dispensacion-origin | ventas: dispensacion 300 only | ViewModel emits | totalGeneral=300, totalServiciosExtra=0 |
| CC-1-c | ServiciosExtraHoy from Venta | 2 servicio_extra-origin ventas | ViewModel emits | serviciosExtraHoy has 2 Venta entries |
| CC-1-d | Pago via venta_id older | pago with ventaId to yesterday's venta | ViewModel classifies | cobrosAtrasados, not ventasHoy |
| CC-1-e | Orphan pago fallback | pago with no IDs | ViewModel classifies | ventasHoy |
| CC-2-a | Label shows totalGeneral | totalGeneral=450.0 | Screen renders | "$450.00" in label |
| RF-1-a | totalVendido sum | 3 ventas 100+200+50 | emit | 350.0 |
| RF-1-b | totalPagado via ventaId | 2 pagos 60+40 linked to in-range ventas | emit | 100.0 |
| RF-2-a | cobrosPeriodo venta inside | pago with ventaId, venta fecha in-range | classify | excluded from cobrosPeriodo |
| RF-2-b | cobrosPeriodo venta outside | pago with ventaId, venta fecha outside | classify | included in cobrosPeriodo |
| RF-2-c | cobrosPeriodo fallback legacy | pago without ventaId, with dispensacionId | classify | uses legacy fallback |
| BI-1-a | recaudacionProyectada all ventas | 3 ventas 500+120+30 | emit | 650.0 |
| BI-1-b | recaudacionProyectada mixed origins | dispensacion 500 + servicio_extra 120 | emit | 620.0 |
| ANUL-1-a | Reversal uses original fecha | existing pago fecha=Jun 29 | anular | reversal fecha=Jun 29 |
| ANUL-1-b | Zero monto skips reversal | existing pago monto=0.0 | anular | no reversal inserted |
| ANUL-1-c | Non-existent pago no crash | no pago found | anular | no throw, no reversal |
| RPC-1-a | ventas_emitidas correct | 3 ventas 100+200+150 | call RPC | ventas_emitidas=450 |
| RPC-1-b | Exclude anulados | 2 Completado + 1 Anulado | call RPC | ventas_emitidas=300, movimientos=2 |
| CONSIST-1 | Old vs new equivalence | identical data in both models | compare totals | equal results |
