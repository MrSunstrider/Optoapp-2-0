# Exploration: Finanzas Oleada A — UX/IA cleanup

**Change**: `finanzas-oleada-a-ux-ia`  
**RDD**: `rdd_mode=disabled/unmanaged`  
**Scope**: UX/IA only — no sync/RPC/schema, costos_lc, Biselado/LC UI, cierre export, P&L, config financiera UI  
**Invariants**: PagoEffect; SyncFinanzas order; `recalcular_resumen_diario` client read-only; no remote migrations

---

## Current State

### Loading / empty / error (IMPROVEMENT U3–U4)

`IMPROVEMENT-PLAN.md` lists U1–U4 as “no loading indicator” for PacientesList, Reportes, CierreCaja, Gastos. Reality today for Oleada A targets:

| Screen | Loading | Empty | Error / retry | Notes |
|--------|---------|-------|---------------|-------|
| **CierreCaja** | Yes — `uiState.isLoading` + progress | Yes — empty pagos/ventas copy when `!isLoading` | Yes — `errorMessage` card via Flow `.catch` | In-screen role gate already exists (`AppRoles.canViewCierreCaja`). U3 partially fixed; polish/consistency still in scope. |
| **Reportes** | Pseudo — `_isLoading` cleared after `delay(200)` once `opticaId` present | Yes — “Sin movimientos en este período” | **None** | LinearProgress only during fake load; no Flow error surface. |
| **CostosYGastos** | Partial — `isLoading` for matriz block load only | Gastos tab shows “Sin gastos” without loading gate | Dialog `error` only; Flow `.catch` emits `emptyList()` silently | Gastos tab can flash empty while sync/download runs. |

Reference patterns already in product: `AnalisisNegocioScreen` (loading + error + retry) and `CierreCajaScreen` (role + loading + empty + error).

### Dual gastos CRUD / route alias

- **Canonical UI**: `CostosYGastosScreen` Tab index **3** = “Gastos Operativos” via `CostosYGastosViewModel` (CRUD + sync trigger on empty).
- **Shadow UI**: `AnalisisNegocioScreen` injects `GastosViewModel` and owns full CRUD (list edit/delete, “Nuevo Gasto” dialog, refresh). “Ver todos” navigates `Route.Gastos` (`"gastos"`).
- **Alias**: `MainDrawerScreen` maps `Route.Gastos` → `CostosYGastosScreen(...)` **without** selecting tab 3 → lands on **Lentes** (tab 0). Confusing UX.
- **Dead screen**: `GastosScreen` is `@Deprecated`, still compiles, **not** registered in NavHost.
- **Behavioral skew**: `GastosViewModel.autoGenerarSiFalta` / `autoGenerarRecurrentes` runs on observe; `CostosYGastosViewModel` has recurring flag on save but **no** auto-generation on load. Removing AnalisisNegocio’s `GastosViewModel` without migrating auto-gen loses recurring materialization unless moved to CostosYGastos.

### Dead drawer duplication

- Live drawer: `MainDrawerScreen` → `DrawerContent` in `DrawerSections.kt` (role-gated FINANCE items).
- `ui/components/MainDrawerContent.kt` (~335 LOC) is **never called** from production; only characterized by `MainDrawerContentTest` (string contracts, not Compose).

### Reportes IA defects

1. **Duplicate KPI**: row3 “Pendiente” uses same `porCobrar` as row2 “Por Cobrar” (`ReportesScreen.kt` ~187–194).
2. **Period string**: dropdown option `"Total"` but navigator visibility checks `periodo != "Todo"` → Total still shows prev/next/date chrome; VM label already maps else → `"Total"`.
3. **Role gate**: drawer hides Reportes via `showBiYReportes` / `AppRoles.canViewBiAndReports`, but `ReportesScreen` itself has **no** in-screen lock (unlike AnalisisNegocio / CierreCaja). Deep-link / back-stack can still show data.

### AnalisisDetalle month loss

- `AnalisisNegocioScreen` calls `navController.navigate(Route.AnalisisDetalle.route)` with **no** month arg.
- Detalle uses `hiltViewModel()` → **new** `AnalisisNegocioViewModel` scoped to detalle destination → `init` loads `DateUtils.today().withDayOfMonth(1)`, ignoring parent month.
- `ON_RESUME` → `refresh()` only reloads that default month.

### What is intentionally OUT

SyncFinanzas order, PagoEffect math, RPC/schema, costos_lc / Biselado / LC tabs (placeholders remain), cierre export, P&L, config financiera UI.

---

## Affected Areas (impact map)

| Path | Why |
|------|-----|
| `ui/screens/CierreCajaScreen.kt` | Loading/empty/error consistency (U3 polish) |
| `viewmodel/CierreCajaViewModel.kt` | Only if error/loading contracts need tightening (prefer UI-only) |
| `ui/screens/ReportesScreen.kt` | KPI dedupe, Total/Todo, role gate, loading/empty polish |
| `viewmodel/ReportesViewModel.kt` | Real loading/error if pursued; period string alignment |
| `ui/screens/CostosYGastosScreen.kt` | Gastos tab loading/empty; optional `initialTab` / deep-link |
| `viewmodel/CostosYGastosViewModel.kt` | `selectTab` on entry; migrate `autoGenerarRecurrentes` if GastosVM retired from Analisis |
| `ui/screens/AnalisisNegocioScreen.kt` | Strip Gastos CRUD; deep-link to CostosYGastos gastos tab; pass yearMonth to detalle |
| `ui/screens/AnalisisDetalleScreen.kt` | Consume yearMonth / shared or arg-driven VM |
| `viewmodel/AnalisisNegocioViewModel.kt` | Accept initial month via `SavedStateHandle` (preferred) |
| `ui/navigation/Route.kt` | `AnalisisDetalle` parameterized; deprecate/remove `Gastos`; optional tab route |
| `ui/screens/MainDrawerScreen.kt` | NavHost route wiring |
| `ui/screens/GastosScreen.kt` | Delete or keep isolated deprecated stub |
| `viewmodel/GastosViewModel.kt` | Keep until auto-gen migrated; then delete or reduce to shared helper |
| `ui/components/MainDrawerContent.kt` | Remove or quarantine as unused |
| `ui/screens/DrawerSections.kt` | Source of truth for drawer — do not regress role flags |
| Tests: `ReportesViewModel*Test`, `AnalisisNegocio*Test`, `AnalisisDetalleScreenTest`, `CostosYGastos*Test`, `GastosViewModelTest`, `GastosRecurrentesTest`, `MainDrawerContentTest`, `CierreCajaViewModelTest` | TDD touchpoints |

**Do not touch**: `SyncFinanzasUseCase`, `UploadSyncCoordinator`/`DownloadSyncCoordinator`, `PagoEffect`, resumen RPC writers, schema migrations.

---

## Approaches

### A. Loading / empty / error

1. **UI-contract polish (recommended)** — Align CierreCaja/Reportes/CostosYGastos to AnalisisNegocio/Cierre patterns: distinct loading vs empty; surface errors with retry where Flow catches exist; Reportes: drop fake `delay(200)` or replace with first-emission gate.
   - Pros: Low risk; no sync changes; matches existing UX language.
   - Cons: Reportes Flows stay multi-stream (harder single error channel).
   - Effort: Low–Medium

2. **Unified `AsyncUiState` helper across three screens** — Shared sealed type + composable.
   - Pros: Consistency long-term.
   - Cons: Scope creep beyond Oleada A; larger PR.
   - Effort: Medium–High

### B. Unify gastos

1. **Strip AnalisisNegocio CRUD + deep-link tab 3 (recommended)** — Read-only month summary from `analisis.gastosMes` (and optional short list via repository Flow **without** dialog CRUD); CTA → `costos_y_gastos` with `initialTab=3` (or `?tab=gastos`). Deprecate `Route.Gastos` alias (redirect once then remove) or make alias select tab 3 during deprecation window. Migrate `autoGenerarRecurrentes` into `CostosYGastosViewModel` (or shared pure function already on `GastosViewModel` companion) before dropping Analisis’s VM usage.
   - Pros: Single write path; matches Oleada A decision “one gastos entry”.
   - Cons: Must not lose recurring auto-gen.
   - Effort: Medium

2. **Share one ViewModel across Analisis + CostosYGastos** — Activity-scoped VM.
   - Pros: One CRUD implementation.
   - Cons: Hilt/nav scoping complexity; couples BI screen to costos matrix; higher regression risk.
   - Effort: High

### C. Dead code

1. **Delete `MainDrawerContent` + `GastosScreen` + update/remove characterization tests (recommended)** — Drawer truth stays `DrawerSections.DrawerContent`.
   - Pros: Removes dual-maintenance trap.
   - Cons: Need to port any still-useful assertions to `DrawerSections` tests if missing.
   - Effort: Low

2. **Quarantine with `@Deprecated` + suppress** — Keep files.
   - Pros: Minimal diff.
   - Cons: Dead code remains; false sense of dual drawers.
   - Effort: Low (not recommended)

### D. Reportes KPI / period / role

1. **Surgical UI + string fix (recommended)** — Remove duplicate “Pendiente” KPI (or replace with a distinct metric only if product defines one — default: remove). Align `"Total"` everywhere. Gate with `AppRoles.canViewBiAndReports` mirroring AnalisisNegocio.
   - Pros: Tiny, clear; no ledger changes.
   - Cons: Removing KPI changes layout density (acceptable).
   - Effort: Low

### E. AnalisisDetalle yearMonth

1. **Nav arg + `SavedStateHandle` (recommended)** — `analisis_detalle/{yearMonth}` (ISO `yyyy-MM`); VM reads arg in `init` / `loadData`. Navigate from Analisis with selected month.
   - Pros: Survives process death; clear ownership; no shared-VM coupling.
   - Cons: Small Route/NavHost churn.
   - Effort: Low

2. **Navigation-scoped shared ViewModel** (`hiltViewModel(parentEntry)`) — Reuse parent state.
   - Pros: Zero duplicate fetch.
   - Cons: Fragile back-stack; harder tests; breaks if detalle opened elsewhere.
   - Effort: Medium

---

## Recommendation

Ship Oleada A as **five reviewable work units** (each ≤400 authored lines), preferring **surgical UX** over shared abstractions:

1. Reportes: dedupe KPI, Total/Todo, in-screen role gate (+ light loading truth if cheap).
2. Loading/empty/error polish on CierreCaja + CostosYGastos gastos tab + Reportes error/empty clarity.
3. Unify gastos: strip AnalisisNegocio CRUD → deep-link tab 3; migrate recurring auto-gen; deprecate `gastos` alias.
4. AnalisisDetalle `yearMonth` nav arg + SavedStateHandle.
5. Delete dead `MainDrawerContent` / unused `GastosScreen` (+ test cleanup). Order 5 after 3 so GastosScreen/GastosVM ownership is clear.

**Default product calls**: remove duplicate Pendiente KPI (do not invent a second metric); deep-link must open **Gastos Operativos** tab, not Lentes.

---

## Proposed change boundaries

| IN | OUT |
|----|-----|
| Compose UI state indicators on three screens | SyncFinanzas upload/download order |
| Nav routes / deep-link tab selection | Remote migrations, RLS, RPCs |
| AnalisisNegocio gastos interaction model | PagoEffect / ledger formulas |
| Reportes labels + role gate | costos_lc, Biselado/LC feature UI |
| AnalisisDetalle month identity | Cierre export, P&L, config financiera |
| Dead drawer/screen deletion | Changing `DrawerSections` role policy sets (reuse `canViewBiAndReports`) |

---

## Suggested work units (≤400 lines each)

| WU | Title | Primary files | Est. authored Δ | Depends |
|----|-------|---------------|-----------------|---------|
| **WU1** | Reportes IA fixes | `ReportesScreen.kt`, optional `ReportesViewModel.kt`, tests | ~120–250 | — |
| **WU2** | Loading/empty/error polish | `CierreCajaScreen.kt`, `CostosYGastosScreen.kt`, `ReportesScreen/VM`, tests | ~200–350 | WU1 optional parallel |
| **WU3** | Unify gastos + deep-link | `AnalisisNegocioScreen.kt`, `CostosYGastos*`, `Route.kt`, `MainDrawerScreen.kt`, migrate auto-gen, `Gastos*` tests | ~250–400 | — |
| **WU4** | AnalisisDetalle yearMonth | `Route.kt`, `MainDrawerScreen.kt`, `AnalisisNegocioScreen.kt`, `AnalisisDetalleScreen.kt`, `AnalisisNegocioViewModel.kt`, tests | ~100–220 | — |
| **WU5** | Dead code removal | `MainDrawerContent.kt`, `GastosScreen.kt`, possibly `GastosViewModel` if unused post-WU3, tests | ~150–350 | After WU3 |

Chained PRs recommended if WU2+WU3 land together (budget risk Medium). Prefer stack: WU1 → WU4 → WU3 → WU2 → WU5, or parallel WU1∥WU4 then WU3.

---

## Test touchpoints (strict TDD)

| Area | RED-first targets |
|------|-------------------|
| Reportes KPI | Pure assertion / screenshot-free: `porCobrar` rendered once; no second identical Pendiente value. Prefer extracting KPI model or ViewModel-exposed `kpis` list for unit test. |
| Period Total | `ReportesViewModel` / screen contract: selecting `"Total"` hides period chrome; label `"Total"`; `dentroDelPeriodo` else branch unchanged. |
| Role gate | Mirror `AnalisisNegocio` / `CierreCaja`: unauthorized role → restricted UI (characterization or composable-state test without Hilt if needed). |
| Gastos unify | `AnalisisNegocio` no longer calls save/delete/showNew on gastos; navigation target includes tab=3; `CostosYGastosViewModel.selectTab(3)` or init from nav. |
| Recurring | Move/keep `GastosViewModel.autoGenerarRecurrentes` tests (`GastosRecurrentesTest`) pointing at new owner. |
| yearMonth | VM init with `SavedStateHandle("yearMonth"→"2026-03")` loads March; navigate arg wiring unit/characterization. |
| Dead code | Delete `MainDrawerContentTest` or retarget to `DrawerSections`; ensure no NavHost reference to `GastosScreen`. |
| Regression | Existing `CierreCajaViewModelTest`, `CostosYGastosViewModelTest`, `AnalisisNegocioViewModelTest`, `ReportesViewModel*Test` must stay green; no SyncFinanzas test changes expected. |

Compose UI tests remain constrained by Hilt (project convention: pure JUnit + MockK). Prefer ViewModel / pure helper tests over Compose for gates.

---

## Risks

- **Recurring gastos regression**: dropping Analisis’s `GastosViewModel` without migrating `autoGenerarRecurrentes` stops monthly auto-create.
- **Alias tab-0 trap**: deprecating `gastos` without forcing tab 3 leaves “Ver todos” broken UX during transition.
- **Reportes loading lie**: keeping `delay(200)` while claiming U3 fixed misleads QA; either real first-emission loading or document as non-goal.
- **Role gate false security**: UI-only gate; RLS/DAO must remain source of truth (out of scope but do not weaken drawer flags).
- **Shared VM temptation for detalle**: parent-scoped Hilt can break if route opened cold — prefer nav arg.
- **Dead `MainDrawerContent` deletion**: inventory UX docs/tests may still name it; update characterization only.
- **PR size**: `AnalisisNegocioScreen` (~612 LOC) + gastos strip can blow 400-line budget — split dialog removal vs deep-link wiring if needed.
- **Invariant bleed**: resist “while here” sync or costos_lc work.

---

## Evidence anchors (code)

- Alias without tab: `MainDrawerScreen.kt` `composable(Route.Gastos.route) { CostosYGastosScreen(...) }`
- Dual CRUD: `AnalisisNegocioScreen.kt` `gastosViewModel` + dialog save/delete
- Duplicate KPI: `ReportesScreen.kt` Por Cobrar + Pendiente both `porCobrar.fmt()`
- Total vs Todo: options `"Total"` vs `if (periodo != "Todo")`
- Detalle no month: `navigate(Route.AnalisisDetalle.route)` + fresh `hiltViewModel()`
- Dead drawer: `MainDrawerScreen` uses `DrawerContent`; `MainDrawerContent` has zero call sites
- Role pattern to copy: `AnalisisNegocioScreen` / `CierreCajaScreen` + `AppRoles.canViewBiAndReports`

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `finanzas-oleada-a-ux-ia` with the recommended WU split, explicit OUT list, and recurring-auto-gen migration as a MUST in WU3.
