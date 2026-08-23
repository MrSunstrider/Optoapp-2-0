# Delta for reportes-financieros

## ADDED Requirements

### Requirement: Unique Por Cobrar KPI

`porCobrar` MUST appear in ≤1 headline KPI; remove duplicate Pendiente. No new metrics.

#### Scenario: Once

- GIVEN porCobrar WHEN KPIs render THEN exactly one slot shows it

### Requirement: In-Screen Reportes Role Gate

MUST enforce `canViewBiAndReports` in-screen. Unauthorized: restricted UI, no totals. Drawer-only insufficient.

#### Scenario: Unauthorized blocked

- GIVEN failing role WHEN Reportes (deep-link ok) THEN restricted; no totals

#### Scenario: Authorized

- GIVEN passing role THEN content MAY render

### Requirement: Honest Reportes Loading Triad

MUST NOT clear loading via timed delay alone; track first emission. Empty only after load. Catchable failures SHOULD error+retry.

#### Scenario: Real loading

- GIVEN no emission yet THEN loading until emit/fail; not delay-only

#### Scenario: Empty after

- GIVEN no movements loaded THEN empty shown; loading off

## MODIFIED Requirements

### Requirement: Period Selection

Selector SHALL be `Diario|Semanal|Este mes|Este año|Anual|Total` (not Todo).

#### Scenario: Pick period

- WHEN select THEN periodo matches; totals recompute

#### Scenario: Total selectable

- WHEN select Total THEN periodo=`Total`; all-time range

(Previously: listed Todo.)

### Requirement: Date Picker for Calendar-Anchored Periods

Picker for Diario/Semanal only. For Este mes|Este año|Anual|Total picker hidden. Total MUST hide prev/next chrome (key=`Total` not Todo).

#### Scenario: Diario shows picker

- GIVEN Diario THEN picker visible; label via DateUtils.formatLocalized

#### Scenario: Semanal shows picker

- GIVEN Semanal THEN picker visible

#### Scenario: Confirm date

- WHEN confirm THEN setFechaDiario called; totals update

#### Scenario: Total hides chrome

- GIVEN Total THEN no picker, no prev/next

(Previously: Todo mismatch with Total option.)

### Requirement: Period-Based Pago Date Range

Same windows; all-time key `Total`→`(MIN,MAX)`. End inclusive. Diario/Semanal/Este mes/Este año/Anual unchanged from main spec except Todo→Total.

#### Scenario: Semanal Monday anchor

- GIVEN Monday fechaDiario THEN DAO (fechaDiario, +6)

#### Scenario: Semanal midweek anchor

- GIVEN Wednesday THEN previous Mon through following Sun inclusive

#### Scenario: Total unbounded

- GIVEN Total THEN DAO `(MIN,MAX)`

(Previously: key Todo.)
