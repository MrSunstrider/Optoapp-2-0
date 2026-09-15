# Reportes Financieros Specification

## Purpose

Financial reports screen: period filtering, date-picker controls, and the period-based date range used to query the `pagos` table.

## Requirements

### Requirement: Period Selection

The system SHALL expose a period selector with `Diario | Semanal | Mensual | Anual | Total` and maintain it as observable state. Dead period labels `Este mes`, `Este año`, and `Todo` MUST NOT appear in the selector and MUST NOT be handled as live branches in `ReportesViewModel` period logic.

(Previously: selector documented as `Diario | Semanal | Este mes | Este año | Anual | Todo`.)

#### Scenario: User picks a period

- GIVEN the Reportes screen is open
- WHEN the user selects a period from the dropdown
- THEN the ViewModel's `periodo` state MUST reflect the selection
- AND period-dependent totals MUST recompute

#### Scenario: Selector matches product labels

- GIVEN the Reportes screen is open
- THEN the period options MUST be exactly `Diario`, `Semanal`, `Mensual`, `Anual`, and `Total`
- AND options `Este mes`, `Este año`, and `Todo` MUST NOT be offered

### Requirement: Date Picker for Calendar-Anchored Periods

The system SHALL show a date-picker button alongside the period dropdown for `Diario` and `Semanal`, sharing the same `fechaDiario` state and `DateUtils` conversions. For `Mensual | Anual | Total` the button MUST NOT be visible.

(Previously: date picker hidden for `Este mes | Este año | Anual | Todo`.)

#### Scenario: Diario shows the date picker

- GIVEN `periodo == "Diario"`
- THEN a date-picker button MUST be visible next to the period dropdown
- AND its label MUST show the current `fechaDiario` formatted via `DateUtils.formatLocalized`

#### Scenario: Semanal shows the date picker

- GIVEN `periodo == "Semanal"`
- THEN a date-picker button MUST be visible next to the period dropdown

#### Scenario: Confirmed date propagates to totals

- GIVEN the date-picker dialog is open
- WHEN the user confirms a date
- THEN `viewModel.setFechaDiario(...)` MUST be called with the new `LocalDate`
- AND period-dependent totals MUST reflect the new date

#### Scenario: Mensual hides the date picker

- GIVEN `periodo == "Mensual"`
- THEN the date-picker button MUST NOT be visible

### Requirement: Period-Based Pago Date Range

The system SHALL translate the current period into a `(start, end)` `LocalDate` pair and pass it to `OptoRepository.getPagosByDateRangeForOptica`. The pair MUST match the period's window — never `(LocalDate.MIN, LocalDate.MAX)` — except for `Total`. `end` is inclusive in all rows.

`Diario` → `(fechaDiario, fechaDiario)`. `Semanal` (Monday start) → `(fechaDiario − (dayOfWeek.value − 1), startOfWeek + 6)`. `Mensual` → first and last day of `fechaDiario`'s month. `Anual` → Jan 1 and Dec 31 of selected year `a`. `Total` → `(LocalDate.MIN, LocalDate.MAX)`.

`ReportesViewModel` MUST NOT retain reachable branches for `"Este año"`, `"Este mes"`, or `"Todo"`.

(Previously: ranges documented `Este mes` / `Este año` / `Todo`; VM still branched on dead `"Este año"`.)

#### Scenario: Semanal Monday anchor

- GIVEN `fechaDiario` is a Monday (`dayOfWeek.value == 1`)
- THEN the DAO MUST be called with `(fechaDiario, fechaDiario + 6)`

#### Scenario: Semanal midweek anchor

- GIVEN `fechaDiario` is a Wednesday (`dayOfWeek.value == 3`)
- THEN the DAO MUST be called with the previous Monday through the following Sunday inclusive

#### Scenario: Mensual uses fechaDiario month

- GIVEN `periodo == "Mensual"` and `fechaDiario` falls in month `M` of year `Y`
- THEN the DAO MUST be called with `(first day of M/Y, last day of M/Y)`

#### Scenario: Total spans all dates

- GIVEN `periodo == "Total"`
- THEN the DAO MUST be called with `(LocalDate.MIN, LocalDate.MAX)`
### Requirement: Total Cobrado Computation

The system SHALL compute `totalCobrado` as the sum of `monto` for every pago returned by the period's DAO range, and MUST NOT depend on `allDispensaciones` as a Flow input. The in-memory `dentroDelPeriodo` filter MUST still exclude any pago whose `fecha` falls outside the range (off-by-one safety net).

#### Scenario: Independent of dispensaciones

- GIVEN `totalCobrado` is being collected
- WHEN `allDispensaciones` emits a new value
- THEN `totalCobrado` MUST NOT recompute as a consequence

### Requirement: Cobros del Período Classification

The system SHALL classify each in-range pago by consulting BOTH its `dispensacionId` and its `servicioExtraId`. If the pago's linked entity (dispensación OR servicio extra) is in-range, the pago is a "venta del período" (excluded from `cobrosPeriodo`); otherwise a "cobro de períodos anteriores" (included). When both IDs are present, the dispensación's date is consulted first; if `dispensacionId` is null or unresolved, the servicio extra's `fecha` is consulted. Pagos with neither `dispensacionId` nor `servicioExtraId` count as "cobro de períodos anteriores".

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

### Requirement: Annual Year Filter

The system SHALL provide a year dropdown when `periodo == "Anual"`, defaulting to the current calendar year. When the user picks year `a`, the DAO MUST be called with `(Jan 1 of a, Dec 31 of a)` and dependent totals MUST recompute.

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

### Requirement: Servicios Extra in Detail List

`ReportesScreen` SHALL render servicios extra alongside dispensaciones in the "Detalle de Ventas" `LazyColumn`. The screen MUST collect `allServiciosDelPeriodo` from the ViewModel and merge both lists chronologically. Each item MUST show date, description, montoTotal, and payment status.

#### Scenario: Period has both dispensaciones and servicios extra

- GIVEN a period with 2 dispensaciones and 1 servicio extra
- WHEN `ReportesScreen` renders the detail list
- THEN all 3 items MUST appear in the `LazyColumn`
- AND servicios extra MUST NOT be absent from the rendered list

#### Scenario: Period has only servicios extra

- GIVEN a period with 0 dispensaciones and 3 servicios extra
- WHEN `ReportesScreen` renders the detail list
- THEN all 3 servicios extra MUST appear

### Requirement: Servicios Extra in PDF Report

`ReporteFinancieroPdfGenerator.generate()` SHALL accept a `serviciosExtra: List<ServicioExtra>` parameter. The PDF detail section MUST render servicios extra rows alongside dispensación rows.

#### Scenario: PDF with servicios extra

- GIVEN a non-empty servicios extra list
- WHEN the PDF is generated
- THEN the detail section MUST contain servicios extra rows

#### Scenario: PDF with empty servicios extra

- GIVEN an empty servicios extra list
- WHEN the PDF is generated
- THEN the detail section MUST contain only dispensación rows
