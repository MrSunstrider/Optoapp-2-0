# Reportes Financieros Specification

## Purpose

Financial reports screen: period filtering, date-picker controls, and the period-based date range used to query the `pagos` table.

## Requirements

### Requirement: Period Selection

The system SHALL expose a period selector with `Diario | Semanal | Este mes | Este año | Anual | Todo` and maintain it as observable state.

#### Scenario: User picks a period

- GIVEN the Reportes screen is open
- WHEN the user selects a period from the dropdown
- THEN the ViewModel's `periodo` state MUST reflect the selection
- AND period-dependent totals MUST recompute

### Requirement: Date Picker for Calendar-Anchored Periods

The system SHALL show a date-picker button alongside the period dropdown for `Diario` and `Semanal`, sharing the same `fechaDiario` state and `DateUtils` conversions. For `Este mes | Este año | Anual | Todo` the button MUST NOT be visible.

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

### Requirement: Period-Based Pago Date Range

The system SHALL translate the current period into a `(start, end)` `LocalDate` pair and pass it to `OptoRepository.getPagosByDateRangeForOptica`. The pair MUST match the period's window — never `(LocalDate.MIN, LocalDate.MAX)` — except for `Todo`. `end` is inclusive in all rows.

`Diario` → `(fechaDiario, fechaDiario)`. `Semanal` (Monday start) → `(fechaDiario − (dayOfWeek.value − 1), startOfWeek + 6)`. `Este mes` → first and last day of `LocalDate.now()` month. `Este año` → Jan 1 and Dec 31 of `LocalDate.now().year`. `Anual` → Jan 1 and Dec 31 of selected year `a`. `Todo` → `(LocalDate.MIN, LocalDate.MAX)`.

#### Scenario: Semanal Monday anchor

- GIVEN `fechaDiario` is a Monday (`dayOfWeek.value == 1`)
- THEN the DAO MUST be called with `(fechaDiario, fechaDiario + 6)`

#### Scenario: Semanal midweek anchor

- GIVEN `fechaDiario` is a Wednesday (`dayOfWeek.value == 3`)
- THEN the DAO MUST be called with the previous Monday through the following Sunday inclusive

### Requirement: Total Cobrado Computation

The system SHALL compute `totalCobrado` as the sum of `monto` for every pago returned by the period's DAO range, and MUST NOT depend on `allDispensaciones` as a Flow input. The in-memory `dentroDelPeriodo` filter MUST still exclude any pago whose `fecha` falls outside the range (off-by-one safety net).

#### Scenario: Independent of dispensaciones

- GIVEN `totalCobrado` is being collected
- WHEN `allDispensaciones` emits a new value
- THEN `totalCobrado` MUST NOT recompute as a consequence

### Requirement: Cobros del Período Classification

The system SHALL classify each in-range pago: if its linked dispensación is in-range, the pago is a "venta del período" (excluded from `cobrosPeriodo`); otherwise a "cobro de períodos anteriores" (included). Pagos with `dispensacionId == null` count as "cobro de períodos anteriores".

#### Scenario: Pago linked to in-range dispensación

- GIVEN a pago whose dispensación date is inside the period
- THEN this pago's `monto` MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range dispensación

- GIVEN a pago whose dispensación date is outside the period
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

#### Scenario: Pago with no dispensación (C regression guard)

- GIVEN a pago where `dispensacionId == null`
- THEN this pago's `monto` MUST contribute to `cobrosPeriodo`

### Requirement: Annual Year Filter

The system SHALL provide a year dropdown when `periodo == "Anual"`, defaulting to the current calendar year. When the user picks year `a`, the DAO MUST be called with `(Jan 1 of a, Dec 31 of a)` and dependent totals MUST recompute.
