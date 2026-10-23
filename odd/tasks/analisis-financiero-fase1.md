# Análisis Financiero — Fase 1 (coach de decisión)

Objective: Make `estadisticas_bi` answer “¿qué hago este mes y por qué?” with clear P&L language, incomplete-margin honesty, waterfall/KPI strip, evidence-based plan, gasto alert→Costos (no CRUD), and local seguimiento ready for future sync. Reportes untouched.  
Branch: `feat/analisis-financiero-fase1`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
Forecast: ~900 authored LOC (add+del, excl. generated)

## Specs

S1. User decisions (verbatim package): `1 a` `2 a` `3 b` `4 a` `5 c` — IGV homogéneo/disclaimer only; one objective = resultado operativo %; gastos = alerta + atajo Costos (no CRUD en home); drivers volumen+ticket+costo deferred to Fase 2; seguimiento local ahora, diseño listo para sync.

S2. User (verbatim role): `Responde "¿Qué hago este mes y por qué?". Es un coach de decisiones para el dueño de la óptica.`

S3. User (verbatim): rename current “neto” — glosario: margen bruto, margen de contribución, resultado operativo; utilidad neta not shown without fiscal policy; IGV as explicit assumption without inventing it.

S4. User (verbatim): `Unifica el redondeo de porcentajes.` — Análisis UI/domain display uses 1 decimal.

S5. User (verbatim): `Cobertura de costo: si es baja, marca el margen como "incompleto" en lugar de mostrarlo como cierto.`

S6. User (verbatim structure): `tira de KPIs, waterfall (ventas → COGS → bruto → opex → resultado), 3–5 alertas, y drill-down` — Fase 1: KPI strip + waterfall on home; alerts typed rojo/ámbar; drill-down via existing Detalle.

S7. User (verbatim): plan format `Hallazgo (dato) → Causa probable → Acción concreta esta semana → Impacto estimado en S/ con supuesto visible y editable → Esfuerzo → CTA medible` — impact assumptions visible/editable (not hardcoded-only).

S8. User (verbatim): `Una sola fuente de verdad para vendido, COGS, margen y ticket: resumen_diario / rpc_analisis_mensual.` — no client-side alternate formulas.

S9. User (verbatim out of scope for this screen): `Tablas OT por OT, demografía de pacientes, mix clínico, ranking completo de inventario, export PDF multisección, comparativos anuales` and Reportes redesign — do not touch.

S10. Locked product package from plan: remove gastos CRUD from Análisis home; keep alert + CTA to CostosYGastos gastos tab for month; rename Más tarde → En curso; keep Room seguimiento by optica_id+periodo with stable IDs sync-ready; no remote sync in Fase 1.

Assumptions: `margenNetoObjetivo` field name may stay in DB/API; UI label becomes objetivo de resultado operativo %. Waterfall uses existing RPC keys (`ventas_mes`, `costo_mes`, `margen_bruto`, `gastos_mes`, derived resultado). Typed alerts refactor Prioridad/UI colors without new persistence. Editable impact assumptions live in `ConfiguracionFinanciera` (or equivalent per-optica config) with defaults matching today’s ImpactoRecomendacion constants (25%, 70%, 10%, 10%).

## Tasks

- [x] T1 S3–S4 S1 — Glossary rename: Resultado operativo (+ disclaimer IGV); objetivo label; % to 1 decimal in Análisis display — route: inline — commit: e9ceb651
- [x] T2 S5 — Mark resultado/margen incompleto when cost coverage below threshold; keep number visible — route: inline — commit: 83ad6f7d
- [x] T3 S6 S8 — Home: separate P&L vs Caja; KPI strip; waterfall Ventas→COGS→Bruto→Opex→Resultado — route: inline — commit: e8b7251f
- [x] T4 S1 S10 — Remove gastos CRUD from Análisis home; gasto-out-of-range alert + CTA to Costos — route: inline — commit: pending-hash
- [ ] T5 S7 — Plan cards: Hallazgo→Causa→Acción→Impacto+editable supuesto→Esfuerzo→CTA; surface Impacto assumptions in config — route: inline — commit:
- [ ] T6 S6 — Typed alerts rojo/ámbar (3–5) from recommendation/priority model — route: inline — commit:
- [ ] T7 S1 S10 — Seguimiento: En curso label; sync-ready entity/ID shape only (no Supabase sync) — route: inline — commit:

## Log

L1. User (2026-10-22): full diagnosis + Fase 1 plan; decisions `1a 2a 3b 4a 5c`.
L2. User (2026-10-23): `empezemos fase 1 odd` — authorized ODD implementation on `feat/analisis-financiero-fase1` from `origin/main` @ 7f755b7b.
L3. T1 GREEN: AnalisisPercentFormatter (1 decimal); coach/strings “resultado operativo”; disclaimer IGV; offline bruto/serie % → 1 decimal. Focused tests BUILD SUCCESSFUL. Commit e9ceb651.
L4. T2 GREEN: ResultadoOperativoConfiabilidad (umbral 80% / sinDatos); chip “incompleto” + hint in resumen/contexto. Focused tests BUILD SUCCESSFUL.
L5. T3 GREEN: AnalisisDecisionLayout (waterfall + KPIs + caja) from rpc fields; DecisionHomeSection on AnalisisNegocioScreen. Focused tests BUILD SUCCESSFUL.
L6. T4 GREEN: Removed gastos CRUD from AnalisisNegocioScreen; GastoFueraDeRango alert (>40%) + CTA to Costos; REDUCIR_GASTO shares threshold. Focused tests BUILD SUCCESSFUL.
