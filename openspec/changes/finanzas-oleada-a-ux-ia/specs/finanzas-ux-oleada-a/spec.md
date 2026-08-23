# Finanzas UX Oleada A Specification

## Purpose

Oleada A Finanzas UX/IA: CostosYGastos gastos triad, single gastos write path + tab-3 deep-link + recurring auto-gen, AnalisisDetalle `yearMonth`, dead UI removal. Out: sync, PagoEffect, schema/RLS.

## Requirements

### Requirement: CostosYGastos Gastos Tab Loading Triad

Tab 3 MUST distinguish loading/empty/error. Empty MUST NOT show while loading. Catchable failures MUST show error+retry.

#### Scenario: No empty flash

- GIVEN loading WHEN render THEN loading shown, empty hidden

#### Scenario: Empty after load

- GIVEN zero gastos loaded THEN empty shown, loading hidden

#### Scenario: Error retry

- GIVEN Flow failure THEN error+retry shown

### Requirement: Single Gastos Write Path

AnalisisNegocio MUST NOT own gastos CRUD. Canonical writes MUST be CostosYGastos tab 3. Analisis MAY show read-only `gastosMes` + CTA.

#### Scenario: No Analisis CRUD

- GIVEN Analisis WHEN view gastos THEN no write actions; CTA → tab 3

### Requirement: Gastos Deep-Link Opens Tab 3

Former `Route.Gastos` MUST open CostosYGastos tab 3; MUST NOT land tab 0.

#### Scenario: Ver todos

- GIVEN navigate from Analisis THEN selected tab=3 not 0

### Requirement: Recurring Auto-Gen Survives Unification

Migrate `autoGenerarRecurrentes` to CostosYGastos (or shared helper) before dropping Analisis `GastosViewModel`. Eight CHECK categorias MUST remain; recurring tests green on new owner.

#### Scenario: Auto-gen on observe

- GIVEN due templates WHEN CostosYGastos observes THEN materialize as before

### Requirement: AnalisisDetalle Preserves YearMonth

Route `analisis_detalle/{yearMonth}` (ISO yyyy-MM). VM MUST init from arg; MUST NOT reset to current month from Analisis selection.

#### Scenario: March kept

- GIVEN 2026-03 selected WHEN open detalle THEN loads 2026-03

#### Scenario: Missing arg

- GIVEN invalid/missing yearMonth THEN MAY fallback current month; MUST NOT crash

### Requirement: Dead Finanzas UI Removed

Remove unused `MainDrawerContent` + `GastosScreen`. Drawer truth=`DrawerSections`. No NavHost `GastosScreen`. Retarget/delete dead tests.

#### Scenario: Graph clean

- GIVEN post-change nav THEN no GastosScreen destination; MainDrawerContent unused
