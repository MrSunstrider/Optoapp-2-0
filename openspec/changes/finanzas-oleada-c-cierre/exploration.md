# Exploration: Finanzas Oleada C — export cierre + Operación Hoy continuity

**Change**: `finanzas-oleada-c-cierre`
**Issue**: #107 (status:approved) — `feat(finanzas): Oleada C — export cierre + continuidad Operación Hoy`
**RDD**: `rdd_mode=disabled/unmanaged`
**Depends on**: Oleada A UX (#105) for CierreCaja triad/role; Oleada B (#106) out of path
**Protect**: `PagoEffect` (do not edit)

**IN**: PDF/CSV export of Cierre de Caja gated by `AppRoles.canExportCierreCaja`; date/context continuity Operación Hoy → Cierre; optional counted-cash + difference **without** `arqueo_caja` table
**OUT**: Multi-shift formal cash audit; full accounting; P&L / config financiera / resumen_diario UI (Oleada D); schema/RLS for arqueo revival

---

## Current State

### CierreCaja — aggregates ready; no export

| Piece | Status |
|-------|--------|
| `CierreCajaViewModel` + `CierreCajaUiState` | Done — date-scoped pagos/disp/servicios; `PagoEffect` for hero, method cards, saldo |
| `CierreCajaUiPolicy` | Done — access + loading/empty/error triad (Oleada A) |
| `AppRoles.canViewCierreCaja` / `canExportCierreCaja` | Done — export aliases view set (`admin`/`gerente`/`especialista`) |
| PDF/CSV export on Cierre | **Missing** — no share actions on `CierreCajaScreen` |
| Nav date arg | **Missing** — `Route.CierreCaja` is static `"cierre_caja"` |

Export precedent: `ReportesScreen` → `ReporteFinancieroPdfGenerator.generate(...)` + `FileShareUtils.openPdf` / `sharePdf`. No CSV helper exists anywhere in `optoapp/` (greenfield for CSV).

`ReporteFinancieroPdfGenerator` is period/report-shaped (dispensaciones + servicios + KPI headers). Cierre needs a **day-close** document: hero cobrado (PagoEffect), method breakdown, ventas del día, cobros list, optional counted-cash section — prefer a dedicated `CierreCajaPdfGenerator` (or thin wrapper) rather than overloading the reportes generator.

### Operación Hoy — always today; blind navigate to Cierre

`OperacionHoyViewModel` hardcodes `DateUtils.today()` for KPIs (`cobrosHoy` via `PagoEffect`). Screen Quick Action **Caja** calls `navController.navigate(Route.CierreCaja.route)` with **no fecha**. Cierre defaults to today independently — works for the happy path but has no shared date contract (breaks if Operación Hoy ever shows another day, or if Cierre was left on "Ayer" in a retained VM).

Continuity = pass `fecha` (ISO) as optional nav query/path arg; Cierre `SavedStateHandle`/`setFecha` on entry.

### Optional efectivo contado — arqueo intentionally dead

`openspec/changes/remove-arqueo-caja` removed Room/sync/UI for `arqueo_caja`. Historical migrations still mention `efectivoContado` / `diferenciaEfectivo`; production code must **not** revive the table.

Optional scope for #107: ephemeral UI on Cierre — user enters counted cash; difference = counted − PagoEffect net for método Efectivo (same day). Persist **session-only** or local prefs keyed by `(opticaId, fecha)` — never a synced `arqueo_caja` entity.

### Roles

| Gate | Roles |
|------|--------|
| View Cierre | admin, gerente, especialista |
| Export Cierre | same (`canExportCierreCaja` ≡ `canViewCierreCaja`) |
| Operación Hoy | broader (includes asesor/ventas) — can navigate to Cierre but Cierre itself fail-closes |

---

## Affected Areas

| Path | Why |
|------|-----|
| `util/CierreCajaPdfGenerator.kt` (new) or extend `ReporteFinancieroPdfGenerator` | Day-close PDF from `CierreCajaUiState` |
| `util/CierreCajaCsvExporter.kt` (new) | CSV rows: summary + cobros + methods; share via `FileShareUtils.shareFile` |
| `util/FileShareUtils.kt` | Possibly `shareCsv` convenience (optional) |
| `ui/screens/CierreCajaScreen.kt` | Export overflow/menu; optional counted-cash field; gate with `canExportCierreCaja` |
| `viewmodel/CierreCajaViewModel.kt` | Optional `efectivoContado` state + `diferencia`; consume nav `fecha`; **keep PagoEffect math unchanged** |
| `ui/navigation/Route.kt` + `MainDrawerScreen` NavHost | `cierre_caja?fecha=` or `cierre_caja/{fecha}` optional |
| `ui/screens/OperacionHoyScreen.kt` | Navigate with `uiState.fecha` |
| Specs: delta `cierre-caja` | Export + continuity + optional counted-cash; no arqueo table |
| Tests: VM export helpers, CSV content, nav fecha, role gate | Strict TDD |

**Do not touch**: `PagoEffect.kt`; SyncFinanzas coordinators; `arqueo_caja` migrations revival; AnalisisNegocio / Reportes PDF (except shared FileShare utils); DrawerSections role sets beyond export button visibility.

---

## Approaches

### 1. **Dedicated cierre exporters + optional nav fecha + session counted-cash** (recommended)

Clone Reportes PDF share pattern with cierre-specific PDF/CSV; add optional `fecha` nav arg from Operación Hoy; counted-cash as UI-only delta vs PagoEffect Efectivo.

- Pros: Matches existing share UX; closes #107 IN; keeps arqueo dead; WUs split cleanly ≤400
- Cons: Second PDF generator to maintain; CSV is new surface (encoding/locale)
- Effort: Medium

### 2. **Reuse ReporteFinancieroPdfGenerator only; skip CSV; skip counted-cash**

Wire Reportes-style PDF from Cierre totals; date continuity only.

- Pros: Smallest diff
- Cons: Does not close #107 (CSV + optional cash called out); PDF shape wrong for day-close
- Effort: Low

### 3. **Revive lightweight local arqueo table (no sync)**

Room-only `arqueo_local` for counted cash history.

- Pros: Persists across app restarts with history
- Cons: Contradicts remove-arqueo intent; schema/migration churn; easy to accidentally re-sync; exceeds OUT
- Effort: High

---

## Recommendation

**Approach 1**, sliced for ≤400-line review budget (`auto-chain`):

1. **WU-Export**: Pure PDF/CSV builders + unit tests from fixture `CierreCajaUiState` (PagoEffect numbers as inputs, not recalculated differently)
2. **WU-UI-Export**: Cierre screen actions + `AppRoles.canExportCierreCaja` fail-closed
3. **WU-Date**: Route optional fecha + Operación Hoy navigate + VM `SavedStateHandle`
4. **WU-Cash (optional)**: Counted-cash field + diferencia display; prefs or memory only — **no** `arqueo_caja`

Invariant: every exported cobrado/method total MUST equal on-screen PagoEffect aggregates (characterization tests).

---

## Risks

- **PagoEffect drift**: Export recomputing with raw `sum(monto)` would regress ledger — feed exporters from already-aggregated UI/VM values or shared pure helpers already used by VM
- **CSV locale**: Decimal comma vs point; use invariant formatting + UTF-8 BOM if Excel is a target
- **Role confusion**: Operación Hoy roles who cannot view Cierre still see Caja CTA — existing; export must not widen roles
- **Retained ViewModel fecha**: Without nav arg + `setFecha`, process death / back-stack can show wrong day
- **Scope creep**: Formal multi-shift arqueo — refuse; point to OUT

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `finanzas-oleada-c-cierre` with:

- Approach 1 + WU split above
- IN/OUT from #107
- `rdd_mode=disabled/unmanaged`
- Invariants: protect `PagoEffect`; no `arqueo_caja` table/sync
- Branch suggestion: `feat/finanzas-oleada-c-cierre`
- Closes #107
