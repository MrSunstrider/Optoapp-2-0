# Design: Finanzas Oleada A — UX/IA cleanup

**Change**: `finanzas-oleada-a-ux-ia` · **Issue**: #105 · **RDD**: `rdd_mode=disabled/unmanaged`  
**Delivery**: `auto-chain`, WUs ≤400 · **Schema/RLS/PagoEffect**: untouched

## Technical Approach

Surgical Compose/ViewModel UX (proposal A1/B1/C1/D1/E1). No sync order, RPC writers, remote migrations, or `PagoEffect`. One gastos write path (CostosYGastos tab 3); honest Reportes/Cierre triad; Análisis month → Detalle via nav arg.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|----------|---------|--------|-----------|
| Deep-link tab | Alias→tab0; Activity VM; `initialTab=3` | **`CostosYGastosScreen(initialTab=3)` + `selectTab(3)`**; `Route.Gastos` + Análisis CTA pass 3 | Matches `dispensacionId` param; INV-6 |
| Recurring auto-gen | Keep GastosVM; Shared VM; Migrate | **Move load-time `autoGenerarSiFalta` into `CostosYGastosViewModel`**; keep pure `autoGenerarRecurrentes`; retarget `GastosRecurrentesTest` | INV-5; delete GastosVM after green |
| Detalle month | Parent Hilt VM; Nav+SSH | **`analisis_detalle/{yearMonth}`** + `SavedStateHandle` | Process-death safe; cold-open OK |
| Pendiente KPI | Invent; Keep dup; Remove | **Remove** | Product default; no ledger |
| Period string | Keep `"Todo"`; Align | **`"Total"` everywhere** | Dropdown already Total |
| Reportes role | Drawer only; In-screen | **`AppRoles.canViewBiAndReports`** | Copy Analisis/Cierre; no DrawerSections policy edits |
| Reportes load | `delay(200)`; First emit | **First Flow/combine emission** | Honest triad |
| Dead UI | Deprecate; Delete | **Delete `MainDrawerContent` + `GastosScreen`** | Live drawer = `DrawerSections` |
| AsyncUiState | Shared helper | **Out** | Scope creep |
| RDD | managed | **`disabled/unmanaged`** | Explicit |

## Data Flow

```
Analisis (read-only gastosMes) ──CTA──► Route.Gastos|CostosYGastos(initialTab=3)
                                              │
                                              ▼
                                    CostosYGastosVM: Flow + autoGenerarSiFalta + CRUD

Analisis (mes) ──navigate analisis_detalle/yyyy-MM──► Detalle VM(SavedStateHandle)
                                              │
                                              └─ loadData(parsed month)  // not today()
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/navigation/Route.kt` | Modify | `AnalisisDetalle(yearMonth)`; keep `Gastos` alias until WU5 |
| `ui/screens/MainDrawerScreen.kt` | Modify | Wire yearMonth; alias → `initialTab=3` |
| `ui/screens/CostosYGastosScreen.kt` | Modify | `initialTab`; gastos loading≠empty |
| `viewmodel/CostosYGastosViewModel.kt` | Modify | Migrate auto-gen |
| `ui/screens/AnalisisNegocioScreen.kt` | Modify | Strip CRUD; CTA tab 3; yearMonth nav |
| `ui/screens/AnalisisDetalleScreen.kt` | Modify | Destination consumes arg via Hilt |
| `viewmodel/AnalisisNegocioViewModel.kt` | Modify | SSH initial month |
| `ui/screens/ReportesScreen.kt` | Modify | KPI/Total/role/triad |
| `viewmodel/ReportesViewModel.kt` | Modify | Drop delay; first-emission load |
| `ui/screens/CierreCajaScreen.kt` | Modify | Triad polish if needed |
| `ui/screens/GastosScreen.kt` | Delete | Not in NavHost |
| `ui/components/MainDrawerContent.kt` | Delete | Unused |
| `viewmodel/GastosViewModel.kt` | Delete post-WU3 | After tests moved |
| `*Reportes*Test`, `CostosYGastos*Test`, `Analisis*Test`, `GastosRecurrentesTest`, `MainDrawerContentTest` | Modify/Delete | RED-first; retarget; drop dead |

**Untouched**: `PagoEffect`, SyncFinanzas + coordinators, resumen RPC writers, `supabase/migrations/`, DrawerSections role sets, costos_lc/Biselado/LC UI.

## Contracts

```kotlin
fun CostosYGastosScreen(..., initialTab: Int = 0)
data class AnalisisDetalle(val yearMonth: String) : Route("analisis_detalle/$yearMonth")
```

Bad/missing `yearMonth` → current month day-1 + log; clamp tab 0–3.

## Testing (JUnit+MockK, no Robolectric)

| Target | RED approach |
|--------|--------------|
| Recurring | Retarget `GastosRecurrentesTest` to Costos companion/util |
| Tab 3 | `CostosYGastosViewModelTest` selectTab/init |
| yearMonth | VM + `SavedStateHandle("yearMonth"="2026-03")` loads March |
| Reportes Total/load | VM state: Total hides chrome; no delay-based loading |
| KPI | Helper/`kpis` list: single `porCobrar` (no Compose) |
| Role | Restricted-branch characterization (mirror Cierre) |
| Regression | Existing Cierre/Costos/Analisis/Reportes VM tests green; no SyncFinanzas test edits |

## Threat Matrix

Compose nav only — no shell/VCS/PR/exec-classification.

| Boundary | Applicability | Response | RED |
|----------|---------------|----------|-----|
| Doc-like paths / Git / Commit / Push / PR | N/A | — | — |
| Nav args (`yearMonth`, tab) | Applicable | Parse `yyyy-MM`; clamp tab; fallback month | Invalid SSH → current month; `initialTab=3` → gastos |

## Migration / Rollout

No remote migration. Chain: WU1 Reportes ∥ WU4 yearMonth → WU3 unify+auto-gen → WU2 triad → WU5 delete. Split WU3 near 400. Rollback = revert slice; keep GastosVM until recurrentes green on Costos.

## Open Questions

- [x] Pendiente → remove · Detalle → nav arg
- [ ] Prefer Costos companion for `autoGenerarRecurrentes`; extract to `domain/` only if dual-VM coexistence needs it
