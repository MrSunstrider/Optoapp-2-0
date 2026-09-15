# Delta for reportes-financieros

## MODIFIED Requirements

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
