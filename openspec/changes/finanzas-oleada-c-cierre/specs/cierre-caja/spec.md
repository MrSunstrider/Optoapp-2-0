# Delta for cierre-caja

## ADDED Requirements

### Requirement: Day-Close PDF Export

The system SHALL provide a day-close PDF for the selected Cierre fecha generated from already-aggregated `CierreCajaUiState` values (hero cobrado, method totals, ventas, cobros list, optional counted-cash). Export MUST NOT recompute cobrado via raw `sum(monto)`. Export actions MUST be available only when `AppRoles.canExportCierreCaja(rol)` is true (fail-closed when rol is null).

#### Scenario: PDF totals match on-screen PagoEffect aggregates

- GIVEN Cierre shows hero cobrado 60 and Efectivo 60 from PagoEffect
- WHEN the user exports PDF
- THEN PDF hero and Efectivo MUST equal 60
- AND MUST NOT equal raw monto sum if that differs

#### Scenario: Export denied without role

- GIVEN `opticaRol` is null or a role outside the export set
- WHEN Cierre renders
- THEN PDF/CSV export actions MUST NOT be enabled

### Requirement: Day-Close CSV Export

The system SHALL export CSV (UTF-8, invariant decimal point; SHOULD include BOM for Excel) with summary row(s), method breakdown, and cobros rows for the selected fecha. Values MUST match the same aggregates as the PDF/UI.

#### Scenario: CSV method sum equals hero

- GIVEN method totals from PagoEffect summing to hero H
- WHEN CSV is generated
- THEN sum of method columns/rows MUST equal H

### Requirement: Fecha Continuity from Operación Hoy

`Route.CierreCaja` SHALL accept an optional ISO fecha. Operación Hoy Caja navigation MUST pass its displayed `fecha`. On entry, Cierre SHALL apply that fecha (e.g. `SavedStateHandle` / `setFecha`). Missing arg MUST default to today.

#### Scenario: Navigate with yesterday fecha

- GIVEN Operación Hoy displays fecha `2026-08-22`
- WHEN user taps Caja
- THEN Cierre selectedDate MUST be `2026-08-22`

#### Scenario: Direct open without fecha

- GIVEN navigation to Cierre without fecha arg
- WHEN screen loads
- THEN selectedDate MUST be today

### Requirement: Optional Counted Cash Without Arqueo Table

Cierre MAY let the user enter counted cash for the selected day. Difference SHALL be `contado − PagoEffect net for método Efectivo` that day. Persistence MUST be session-only or local prefs keyed by `(opticaId, fecha)`. The system MUST NOT create, sync, or revive `arqueo_caja`.

#### Scenario: Difference uses Efectivo PagoEffect

- GIVEN Efectivo net 150 and user enters contado 140
- WHEN difference displays
- THEN difference MUST be −10
- AND no `arqueo_caja` write occurs

#### Scenario: Empty contado hides difference

- GIVEN contado field empty/null
- WHEN screen renders
- THEN difference section MUST be hidden or show no computed delta
