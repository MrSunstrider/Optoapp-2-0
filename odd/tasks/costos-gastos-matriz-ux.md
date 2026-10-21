# Costos y Gastos — matriz UX + coherencia OT

Objective: Lentes/Biselado/LC matrices filter and capture the same keys OT cost lookup uses; Gastos first; recurrentes for viewed month; readable LC/list layout.  
Branch: `feat/ui-theme-toggle-disp-chip` (active UI branch)  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim): `en gastos y costos, lentes absolutamente tenemos que mejorar como se ve, como se muestra y como se ingresan los datos` — monofocal must not show all stock types.

S2. User (verbatim): `revisa que tenga sentido con lo que se vende para que cuando se haga el calculo en dispensacion costos y margen, jalen datos reales y pueda guardar tambien correctamente`.

S3. User (verbatim): `analiza tambien como se ingresan los datos en biselados, lentes de contacto, que sean coherentes`.

S4. User (verbatim): `gastos operativos deberia ser lo primero que se muestre`.

S5. User (verbatim): `analiza que gasto recurrente se aplique a los meses siguientes sin tener que hacerlo manualmente`.

S6. User (verbatim): `arreglar tambien como se ven los cuadros (LC)`.

S7. User (verbatim): `usa gentle ai para este ciclo hazlo todo`.

## Tasks

- [x] T1 S1-S2 — Block = real tipo/material filter; new-cost form prefills + IR/curva/subtipo — route: inline — commit: (pending this delivery)
- [x] T2 S4 — CostosTab GASTOS first + screen `when(ordinal)` — route: inline — commit: (pending this delivery)
- [x] T3 S3 — Biselado/LC labels coherence (display codes) — route: inline — commit: (pending this delivery)
- [x] T4 S5 — Recurrentes for `mesGastos` viewed month — route: inline — commit: (pending this delivery)
- [x] T5 S6 — Layout filters stack + MatrizRow multi-line — route: inline — commit: (pending this delivery)
- [x] T6 S7 — Focused tests + suite — route: inline — evidence: CostBlockFilter + Costos* tests green; full `:optoapp:testDebugUnitTest` BUILD SUCCESSFUL

## Log

L1. User authorized full cycle: `usa gentle ai para este ciclo hazlo todo` (2026-10-21). Prior analysis confirmed blockToFilter collapses Monofocal/Bifocal/Multifocal → stock only; matrix form omits OT keys; GASTOS ordinal 3; recurrentes only DateUtils.today().

L2. Implemented `CostBlockFilter` / `aplicarFiltroBloque`; form OT keys; `CostosTab` order GASTOS→LENTES→BISELADO→LC→SERVICIOS; `selectMesGastos` auto-generates recurrentes for viewed month; LC/Biselado OT hints; MatrizRow maxLines=2; filters Column.

L3. Updated Forms/Matrices/ViewModel tests for block prefills and tab ordinals; fixed `reportes_clinico_empty_filtros_subtitle` voseo (Probá→Pruebe); PRECIO_DUPLICADO Tócalo→Ábralo. Full suite green 2026-10-21.
