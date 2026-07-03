# Delta for Reportes Financieros

## ADDED Requirements

### Requirement: Servicios Extra Inclusion in Period Totals

The system SHALL fold `ServicioExtra` revenue into the financial totals emitted by `ReportesViewModel`, `CierreCajaViewModel`, and `BIViewModel`. `aCuenta` is the down-payment analogue of `DispensacionOptica.montoPagado`.

| Field | Formula |
|-------|---------|
| `ReportesViewModel.totalVendido` | `Σ DispensacionOptica.montoTotal + Σ ServicioExtra.montoTotal` (in-range) |
| `ReportesViewModel.totalPagado` | `Σ DispensacionOptica.montoPagado + Σ ServicioExtra.aCuenta` (in-range) |
| `BIViewModel.recaudacionProyectada` | `Σ DispensacionOptica.montoTotal + Σ ServicioExtra.montoTotal` (in-range) |
| `CierreCajaUiState.totalVentasHoy` | `Σ DispensacionOptica.montoTotal` (today only, unchanged) |
| `CierreCajaUiState.totalServiciosExtra` | `Σ ServicioExtra.montoTotal` (today only, NEW) |
| `CierreCajaUiState.totalGeneral` | `totalVentasHoy + totalServiciosExtra` (NEW) |
| `CierreCajaUiState.saldoPendiente` | `totalGeneral - ventasHoy` (FIXED) |

`CierreCajaUiState` MUST also expose `serviciosExtraHoy: List<ServicioExtra>`.

#### Scenario: totalVendido and totalPagado include servicios extra

- GIVEN a Diario period with one in-range dispensacion (montoTotal=100, montoPagado=60) and one in-range servicio extra (montoTotal=40, aCuenta=20)
- WHEN `ReportesViewModel` emits
- THEN `totalVendido` MUST be 140.0 AND `totalPagado` MUST be 80.0

#### Scenario: CierreCajaUiState desglose

- GIVEN today has dispensaciones (montoTotal sum=300), servicios extra (montoTotal sum=150), and pagos today summing 200
- WHEN `CierreCajaViewModel` emits
- THEN `totalVentasHoy`=300.0 AND `totalServiciosExtra`=150.0 AND `totalGeneral`=450.0 AND `saldoPendiente`=250.0

#### Scenario: BI recaudacionProyectada includes servicios extra

- GIVEN a period with dispensaciones (montoTotal sum=500) and servicios extra (montoTotal sum=120), both in-range
- WHEN `BIViewModel` emits
- THEN `recaudacionProyectada` MUST be 620.0

#### Scenario: No servicios extra in period

- GIVEN a period with dispensaciones only
- WHEN the ViewModels emit
- THEN `totalServiciosExtra`=0.0 AND `totalGeneral` MUST equal `totalVentasHoy` AND all other totals MUST match pre-change behavior

### Requirement: Cierre de Caja Payment Classification

The `CierreCajaViewModel` SHALL classify each in-range pago by consulting BOTH `dispensacionId` and `servicioExtraId`.

| Condition | Classification |
|-----------|----------------|
| `dispFecha == fecha` OR `servFecha == fecha` | `ventasHoy` |
| `dispFecha < fecha` OR `servFecha < fecha` | `cobrosAtrasados` |
| neither ID resolves (orphan) | `ventasHoy` (unchanged else branch) |

When both IDs resolve, the earlier date governs the classification.

#### Scenario: Pago linked to today's servicio extra

- GIVEN a pago today with `servicioExtraId` set whose servicio `fecha` == today
- THEN the pago's `monto` MUST contribute to `ventasHoy`

#### Scenario: Pago linked to older servicio extra

- GIVEN a pago today with `servicioExtraId` set whose servicio `fecha` < today
- THEN the pago's `monto` MUST contribute to `cobrosAtrasados`

#### Scenario: Orphan pago stays in ventasHoy

- GIVEN a pago today with neither `dispensacionId` nor `servicioExtraId`
- THEN the pago's `monto` MUST contribute to `ventasHoy` (unchanged)

## MODIFIED Requirements

### Requirement: Cobros del Período Classification

The system SHALL classify each in-range pago by consulting BOTH its `dispensacionId` and its `servicioExtraId`. If the pago's linked entity (dispensación OR servicio extra) is in-range, the pago is a "venta del período" (excluded from `cobrosPeriodo`); otherwise a "cobro de períodos anteriores" (included). When both IDs are present, the dispensación's date is consulted first; if `dispensacionId` is null or unresolved, the servicio extra's `fecha` is consulted. Pagos with neither `dispensacionId` nor `servicioExtraId` count as "cobro de períodos anteriores".
(Previously: classification consulted only `dispensacionId`; servicio-extra and orphan pagos were both lumped into `cobrosPeriodo`.)

#### Scenario: Pago linked to in-range dispensación

- GIVEN a pago whose dispensación date is inside the period
- THEN this pago's `monto` MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range dispensación

- GIVEN a pago whose dispensación date is outside the period
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

#### Scenario: Pago linked to in-range servicio extra

- GIVEN a pago with `servicioExtraId` set and `dispensacionId == null` whose servicio `fecha` is inside the period
- THEN this pago's `monto` MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range servicio extra

- GIVEN a pago with `servicioExtraId` set whose servicio `fecha` is outside the period
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

#### Scenario: Pago with no dispensación and no servicio extra (orphan)

- GIVEN a pago where `dispensacionId == null` AND `servicioExtraId == null`
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

#### Scenario: Pago with both IDs falls back to dispensación date

- GIVEN a pago with both `dispensacionId` and `servicioExtraId` set
- WHEN the dispensación date is in-range but the servicio extra date is out-of-range
- THEN the pago's `monto` MUST NOT contribute to `cobrosPeriodo` (dispensación date wins)
