# Tasks: Finanzas Oleada A — UX/IA cleanup

**Issue**: Closes #105 · **Branch**: `feat/finanzas-oleada-a-ux-ia` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Gates**: GGA-eq R1–R4 per PR; final `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
**Untouched**: SyncFinanzas, PagoEffect, migrations, DrawerSections roles

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~900–1400 / 6 WUs |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 WU1 → PR2 WU4 → PR3 WU3a → PR4 WU3b → PR5 WU2 → PR6 WU5 |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test | Runtime | Rollback |
|------|------|-----------|--------------|---------|----------|
| WU1 | Reportes KPI/Total/role/load | PR1→feat | `*ReportesViewModel*` | N/A JUnit | Revert Reportes* |
| WU4 | yearMonth nav+SSH | PR2→PR1 | `*AnalisisNegocioViewModel*` | N/A JUnit | Revert Route/Analisis* |
| WU3a | Migrate auto-gen | PR3→PR2 | `*GastosRecurrentes*` | N/A JUnit | Revert Costos auto-gen |
| WU3b | Strip CRUD + tab3 | PR4→PR3 | `*CostosYGastosViewModel*` | N/A JUnit | Revert CTA/`initialTab` |
| WU2 | Cierre/Costos triad | PR5→PR4 | `*CierreCaja*`+Costos | N/A JUnit | Revert triad |
| WU5 | Delete dead UI | PR6→PR5 | `testDebugUnitTest` | N/A suite | Revert deletes |

## Phase 1 — WU1 Reportes (≤400)

- [x] 1.1 RED `ReportesViewModelTest`: one `porCobrar`; `"Total"` hides chrome; no delay-only clear.
- [x] 1.2 RED: fail `canViewBiAndReports` → restricted, no totals.
- [x] 1.3 GREEN `ReportesViewModel.kt`: first emission; key `"Total"`.
- [x] 1.4 GREEN `ReportesScreen.kt`: drop Pendiente; role gate; Total chrome.
- [x] 1.5 Focused verify + GGA R1–R4; PR1.

## Phase 2 — WU4 yearMonth (≤400)

- [x] 2.1 RED `AnalisisNegocioViewModelTest`: SSH `2026-03`→March; invalid→current month (threat).
- [x] 2.2 GREEN `Route.kt` + VM/Detalle SSH `AnalisisDetalle(yearMonth)`.
- [x] 2.3 GREEN Analisis/MainDrawer → `analisis_detalle/{yyyy-MM}`.
- [x] 2.4 Focused verify + GGA R1–R4; PR2.

## Phase 3 — WU3a recurring (≤400)

- [x] 3.1 RED retarget `GastosRecurrentesTest` → Costos companion.
- [x] 3.2 GREEN move `autoGenerarSiFalta` → `CostosYGastosViewModel` (keep GastosVM).
- [x] 3.3 Focused verify + GGA R1–R4; PR3.

## Phase 4 — WU3b unify+deep-link (≤400)

- [x] 4.1 RED `CostosYGastosViewModelTest`: `initialTab=3`→gastos; clamp 0–3 (threat).
- [x] 4.2 RED `AnalisisNegocioScreenTest`: no gastos writes; CTA→tab3.
- [x] 4.3 GREEN `CostosYGastosScreen(initialTab)`; Gastos alias→tab3.
- [x] 4.4 GREEN strip Analisis CRUD; CTA tab3; read-only `gastosMes`.
- [x] 4.5 Focused verify + GGA R1–R4; PR4.

## Phase 5 — WU2 triad (≤400)

- [ ] 5.1 RED Costos tab3: loading≠empty; empty after; error+retry.
- [ ] 5.2 RED Cierre: loading≠empty; `errorMessage`; PagoEffect/role unchanged.
- [ ] 5.3 GREEN Costos/Cierre triad polish.
- [ ] 5.4 Focused verify + GGA R1–R4; PR5.

## Phase 6 — WU5 dead UI (≤400)

- [ ] 6.1 Delete/retarget `MainDrawerContentTest`; no NavHost `GastosScreen`.
- [ ] 6.2 Delete `MainDrawerContent.kt`, `GastosScreen.kt`; drop unused `GastosViewModel`.
- [ ] 6.3 Full `testDebugUnitTest`; INV check; GGA R1–R4; PR6 Closes #105.
