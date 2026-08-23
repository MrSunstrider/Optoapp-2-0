# Proposal: Finanzas Oleada A — UX/IA cleanup

**Issue**: [Closes #105](https://github.com/MrSunstrider/Optoapp-2-0/issues/105) (`status:approved`)  
**Change**: `finanzas-oleada-a-ux-ia` · **Depends on**: none · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/finanzas-oleada-a-ux-ia` · **Delivery**: `auto-chain`, WUs ≤400 lines · **Schema/RLS**: none

## Intent / Why

Honest Finanzas UX/IA: loading/empty/error triad, one gastos write path, Reportes KPI/period/role, Análisis→Detalle month preserved, dead drawer/routes gone. Today: dual CRUD, `gastos`→Lentes tab 0, fake Reportes load, duplicate Por Cobrar/Pendiente, drawer-only Reportes role, month loss on detalle. Ledger/sync untouched.

## Evidence (explore)

- `Route.Gastos` → CostosYGastos without tab 3.
- Analisis owns `GastosViewModel` CRUD; CostosYGastos lacks load-time `autoGenerarRecurrentes`.
- Reportes: both KPIs = `porCobrar`; `"Total"` vs `!= "Todo"`; `delay(200)`; no in-screen role.
- Detalle: no `yearMonth` → fresh VM → current month.
- `MainDrawerContent` unused; `GastosScreen` not in NavHost.

## Scope

**IN**: Triad polish (Cierre/Reportes/Costos gastos); single gastos CRUD + tab-3 deep-link + migrate auto-gen; Reportes KPI/Total/role; `analisis_detalle/{yearMonth}`; delete dead UI; TDD.

**OUT**: SyncFinanzas/RPC/schema/RLS; PagoEffect; costos_lc/Biselado/LC; cierre export; P&L/config financiera; AsyncUiState helper; parent-scoped detalle VM; DrawerSections role-policy edits.

## Causal invariants

1. `PagoEffect` unchanged · 2. SyncFinanzas order unchanged · 3. Client read-only for `recalcular_resumen_diario` writers · 4. No remote migrations · 5. Recurring auto-gen survives Analisis CRUD removal · 6. Gastos deep-link opens tab 3, never tab 0.

## Capabilities

**New**: `finanzas-ux-oleada-a` — triad, gastos unify+deep-link, yearMonth nav, dead-UI removal.  
**Modified**: `reportes-financieros` (KPI/Total/role/loading); `cierre-caja` (triad polish only).

## Approach

Surgical UX (explore A1/B1/C1/D1/E1). Auto-chain: WU1 Reportes ∥ WU4 yearMonth → WU3 unify gastos → WU2 triad → WU5 dead code. Split WU3 near 400 lines.

## Affected Areas

| Area | Impact |
|------|--------|
| Reportes*, CierreCaja*, CostosYGastos*, Analisis*, Route, MainDrawerScreen | Modified |
| GastosScreen, MainDrawerContent (± GastosViewModel post-WU3) | Removed |
| Sync / PagoEffect / migrations | Untouched |

## Risks / Rollback

Recurring regression → migrate first; alias tab 0 → force tab 3; fake loading → first-emission gate; WU3 size → split. Revert chained PRs independently; no DB rollback; keep `GastosViewModel` until auto-gen tests green.

## Success Criteria

- [ ] One gastos write UX; Análisis no CRUD; deep-link tab 3
- [ ] Triad: loading ≠ empty; errors where Flows catch
- [ ] Reportes: one Por Cobrar; Total consistent; role-restricted UI
- [ ] Detalle keeps month; dead UI gone; suite green
- [ ] Closes #105; INV-1–6 hold
