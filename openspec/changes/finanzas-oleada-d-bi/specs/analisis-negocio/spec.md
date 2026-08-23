# Delta for analisis-negocio

## ADDED Requirements

### Requirement: Month P&L Statement Block

`AnalisisNegocioScreen` (or Detalle) SHALL show an explicit month P&L: Ventas − COGS − Gastos = Utilidad, sourced from `AnalisisMensual` online (`ventasMes`, COGS via `costoDeVentas()` / RPC costs, `gastosMes`). Online presentation MUST NOT invent alternate formulas that diverge from RPC margen.

#### Scenario: Online P&L lines match AnalisisMensual

- GIVEN ventasMes=1000, COGS=400, gastosMes=200
- WHEN P&L renders online
- THEN lines MUST show 1000, 400, 200 and utilidad 400

### Requirement: Offline P&L Labeling

When `AnalisisMensual.esOffline` is true, the P&L block MUST either compose Ventas/COGS from `resumen_diario` month sums and Gastos from local `gastos_operativos`, or show limited lines clearly labeled offline/partial. MUST NOT present zero margen as if authoritative full online P&L.

#### Scenario: Offline shows partial label

- GIVEN offline analisis with composed or zero margin fields
- WHEN P&L renders
- THEN UI MUST indicate offline or partial status

### Requirement: In-App Resumen Diario Surface

The system SHALL provide a read-only in-app list (or Analisis subsection) of `resumen_diario` rows for the selected month from Room Dao Flow. Pull-to-refresh or retry MUST trigger existing finanzas sync (recalc RPC + download). MUST NOT upload `resumen_diario`.

#### Scenario: Month list shows daily rows

- GIVEN Room has 10 rows for 2026-08
- WHEN user opens resumen for August
- THEN all 10 days appear ordered by fecha

#### Scenario: Refresh uses download path

- GIVEN resumen screen open
- WHEN user refreshes
- THEN finanzas sync download path runs
- AND no resumen upload occurs

## MODIFIED Requirements

### Requirement: Configuracion Financiera Writable From Android

Android MAY insert/update `configuracion_financiera` locally and MUST upload changes. Room Dao SHALL support upsert for user-initiated saves (not download-only). Web companion MAY still edit; last-write-wins via sync timestamps/upsert.

(Previously: R7.2 / R14 / R22 — Android download-only; no upload; no local edit UI.)

#### Scenario: User save triggers upsert and upload path

- GIVEN admin edits thresholds
- WHEN save completes
- THEN Dao upsert succeeds AND upload sync includes configuracion_financiera
