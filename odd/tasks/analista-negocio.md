# Analista de negocio (Must #4)

Objective: Behave as a **business analyst** — concise facts that guide growth, advice and assistance — **not** a report dump. Distinct from Reportes (evidence/export/calc).  
Branch: `feat/analista-negocio` (from `main`)  
Delivery: `auto-chain` + `stacked-to-main` (standing)  
Cycle: ODD → Strict TDD → GGA (pre-commit) → RDD host-relay when risk high → Judgment Day opt-in on money/recs surfaces  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Product job (user intent)

- **Does**: interpret → prioritize → recommend → help track action (coach).
- **Does not**: tabular exports, period ledgers, commission payroll sheets, PDF archives (those are Reportes / RRHH).
- **Tone**: short, actionable Spanish (tú-neutral); one screenful of “qué hacer ahora”, not a BI dashboard clone of Reportes.

## Specs

S1. SRS RF-ANA-04 (verbatim): `El módulo SHALL comportarse como **analista de negocio**: interpretar estadísticas, priorizar acciones y guiar al dueño/gerente hacia crecimiento` — Estado F (Must #4).

S2. SRS RF-ANA-05 (verbatim): `Recomendaciones accionables con impacto estimado y seguimiento` — Estado F (Must #4).

S3. SRS RF-ANA-03 (verbatim): `Recomendaciones rule-based (cobranza, margen, stock, caída, gasto)` — Estado P; Must #4 raises quality without dropping the six rule families already shipped.

S4. Roadmap §10 Must #4 (verbatim): `Análisis financiero “analista de negocio”` · ancla `RF-ANA-03..05` · nota `Ir más allá de reglas simples actuales`.

S5. User (2026-10-15, verbatim intent): `lo que quiero con analista, es que se comporte como un analista del negocio y brinde datos consisos que guien al usuario a mejorar el negocio que le ayude a crecer, que brinde consejos y asistencia, pero que no sea lo mismo que reportes (lo veia igual anteriormente)`.

S6. Prerequisite from Must #3: consume `MarginFacts` (RPC + offline `resumen_diario`); coverage/confidence modulates priority and copy when facts are incomplete.

S7. Análisis UI SHALL lead with interpretation + ranked advice (not raw KPI grids that duplicate Reportes). MarginFacts signals (cobertura, contribución/PE, tendencia) appear as coach context, not as the primary “report”.

S8. Every recommendation tipo SHALL carry estimated impact when computable; else explicit “sin impacto estimable”. Seguimiento: acted / dismissed / useful, persisted per óptica+period (Room-first; sync later if multi-device required).

S9. Role gate unchanged: admin|gerente for Análisis module. No commission/payroll UI here. After Reportes ships `vendedorUserId` aggregates, Analista MAY cite concentration / coaching tips by user or role (especialista who also sells included) — still not the liquidación.

S10. Out of scope: RF-REP-03..06; seller commissions/bonuses feature; RF-FIN cost capture; ML; changing money RPC formulas unless contract bug.

S11. UX/UI (Must #4): coach-first information architecture — see **UX/UI plan** below. Visual language reuses Opto tokens, `MoneyFormatter`, `MonthSelector`, `CostCoverageBadge` / chart wrappers from costos; must not look like Reportes’ KPI grid + ledger list.

## UX/UI plan (Analista)

### Job of the screen
Answer in &lt;10 seconds: “¿Cómo va el mes y qué hago ahora?” — not “muéstrame todas las métricas”.

### Current UI (gap)
- `AnalisisNegocioScreen`: MonthSelector → `ResumenCard` (vendiste/cobraste/saldo/margen%/ticket/N ventas) → top-3 `RecomendacionCard` (👍/👎) → gastos card → nav to detalle/deudores/costos.
- `marginFacts` is on `AnalisisMensual` but **not rendered** on Análisis (charts live under Costos header).
- Feels close to a mini-Reportes (KPI cards first).

### Target IA (top → bottom)
1. **Cabecera coach** — month selector + one-sentence verdict + confidence/cobertura chip (if facts incomplete).
2. **Plan de acción** — ranked recommendations (3–5) with impact, primary CTA, seguimiento chips (hecho / más tarde / no aplica).
3. **Contexto breve** — 2–4 MarginFacts signals max (margen neto vs objetivo, PE, tendencia 6m spark/line, cobertura). No full category tables here.
4. **Atajos** — Deudores / Detalle / Costos (secondary). Detail screen keeps deeper breakdowns without becoming Reportes.

### Components to reuse / add
| Piece | Source |
|-------|--------|
| MonthSelector, MoneyFormatter, OptoTopAppBar | existing |
| CostCoverageBadge / MetricTile | costos common |
| OptoLineChart (serie6m spark) | chart wrappers |
| New: `InterpretacionCard`, `PlanAccionList`, seguimiento chips | this feature |

### Anti-patterns
- Duplicating Reportes period KPI strip + movimiento list.
- Showing every `margenPorCategoria` row on the home Análisis screen.
- Commission/payroll tables.

### Accessibility / copy
- Tú-neutral Spanish via `strings.xml`.
- Priority colors: existing alertRed / warningAmber / positiveGreen.
- Offline banner stays.

## Tasks

| ID | Specs | Route | Status | Evidence |
|----|-------|-------|--------|----------|
| T1 | S5–S6,S11 | inline | done | Gap audit logged L6 |
| T2 | S1,S6,S7,S11 | inline | done | InterpretacionAnalista + coach card + VM; tests GREEN |
| T3 | S3,S7,S11 | inline | done | `37b7e7ce` ImpactoRecomendacion + Plan de acción UI |
| T4 | S2,S8,S11 | inline | done | `e8a493d7` Room v57 seguimiento chips + periodo |
| T5 | S1,S4,S11 | inline | pending | Reorder Resumen → coach-first; demote KPI grid |
| T6 | S9–S10 | inline | pending | Role + regression; GGA; RDD if RPC touched |

## Log

L1. User (2026-10-15): `ahora toca Análisis financiero (creo que estaba analizado y planeado en gastos y costo) Reportes ricos analiza y planea integralmente en base a los requerimientos del srs o a lo que haga referencia` + `usa el ciclo entero de gentle ai odd tdd rdd y gga`.

L2. Exploration (2026-10-15): Must #3 delivered MarginFacts feed and explicitly carved out Must #4 analyst engine. RF-ANA-01/02 I; RF-ANA-03 P; RF-ANA-04/05 F. Costos header has charts; Análisis recs are six rule types with impact mostly only on COBRAR.

L3. Plan decision: separate ODD feature from `reportes-ricos`; apply Must #4 before Must #5 per SRS §10 order unless user reorders.

L4. User (2026-10-15): Analista = coach with concise growth guidance, **not** the same as Reportes; previously saw them as equal. Specs rewritten: S5 user intent; S7 coach-first UI; S9/S10 exclude commissions from Análisis.

L5. User (2026-10-15): Seller attribution = membership user; especialistas sell too — stats/tips may include them later; payroll remains Reportes.

L6. User (2026-10-15): Start apply with Analista; include UX/UI in general plan for both Analista and Reportes. Branch `feat/analista-negocio` created. T1 gap: ResumenCard uses legacy KPIs only; `marginFacts` unused on Análisis screens; recs top-3 with feedback only — coach IA missing.

L7. T1 done. Next: T2 InterpretacionAnalista (Strict TDD) then coach card UI.

L8. T2 done (2026-10-15): RED compile miss → GREEN `InterpretacionAnalistaTest` + `AnalisisNegocioViewModelTest`. Coach card above Resumen on `AnalisisNegocioScreen`. Next: T3 impact engine / Plan de acción list.

L9. T3 done (2026-10-15): `ImpactoRecomendacion` fills etiqueta+monto for all six families; `Recomendacion.impactoMonto` ranks after prioridad; UI title **Plan de acción**, shows up to 5 with Impacto label. GREEN focused + full `:optoapp:testDebugUnitTest`. Cap drops lowest MEDIA by monto (LIQUIDAR in all-rules fixture). Fixed T2 `Text("•…")` for StringsCopyTest. Next: T4 seguimiento.

L10. T4 done (2026-10-15): `SeguimientoEstado` HECHO/MAS_TARDE/NO_APLICA; Room MIGRATION_56_57 adds estado+periodo (backfill from fueUtil); stable id `optica|periodo|rec`; Plan de acción chips replace thumbs; VM loads/saves per yyyy-MM. Next: T5 demote KPI grid.
