# Reportes vistas enriquecidas

Objective: Enrich Reportes tabs (Ventas / Pacientes / Inventario / Clínico / Equipo) with charts, KPI cards, and empty states — interesting content that looks good, not plain text.  
Branch: `feat/reportes-vistas-enriquecidas`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim): `que tengan contenido interesante y que tambien se vea bien, solo veo texto plano sin nada visual y muy limitado`

S2. Scope: enrich existing Reportes modules with visuals (OptoDonut/OptoBar/OptoKpiCard/EmptyState) using data already aggregated in `ReportesModuleStats` / ViewModel — no new SRS metrics invented.

S3. Tabs: Ventas, Pacientes, Inventario, Clínico, Equipo (Resumen already has KPIs/bars).

S4. Reuse existing chart components from `ui/components/charts/` (same pattern as Costos).

## Tasks

- [x] T1 S2 — Enrich `InventarioReporte` with critical low-stock rows + unit test (inline) — GREEN
- [x] T2 S1–S4 — `ReportesChartsModel` + `ReportesModuleSections` + wire `ReportesScreen` + strings (inline)
- [x] T3 S1 — Focused tests + suite (inline) — `testDebugUnitTest` BUILD SUCCESSFUL

## Log

L1. User: `que tengan contenido interesante y que tambien se vea bien, solo veo texto plano sin nada visual y muy limitado`
L2. Implemented charts/KPIs/empty states on Ventas, Pacientes, Inventario, Clínico, Equipo; inventario criticos list; suite green.
