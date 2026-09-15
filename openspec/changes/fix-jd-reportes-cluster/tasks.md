# Tasks: Fix JD Reportes Cluster

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 450–900 (Android ~250–400 + SQL REPLACE bodies ~300–500) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR-A Android WU1–4 → PR-B SQL WU5 (GGA + remote) |
| Delivery strategy | ask-on-risk |
| Chain strategy | size:exception (shipped as single PR #136; Android+SQL already on branch) |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: size:exception
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Static errors S1+S3 | PR-A | `./gradlew :optoapp:testDebugUnitTest --tests "*CierreCajaViewModelTest*" --tests "*SyncFinanzasUseCaseKtTest*"` | N/A (unit) | Revert VM + SyncFinanzas files |
| 2 | Análisis UI S4+S6 | PR-A | `./gradlew :optoapp:testDebugUnitTest --tests "*AnalisisNegocio*"` | N/A (unit/Compose) | Revert `AnalisisNegocioScreen.kt` |
| 3 | ResumenDiarioDao I2+I1 | PR-A | `./gradlew :optoapp:testDebugUnitTest --tests "*ResumenDiarioDaoTest*"` | N/A (Room in-memory) | Revert DAO + test |
| 4 | Reportes I3 | PR-A | `./gradlew :optoapp:testDebugUnitTest --tests "*ReportesViewModel*"` | N/A (unit) | Revert Reportes VM + tests |
| 5 | SQL S2+S5+I4 | PR-B | Supabase SQL tests / ledger assertions after GGA | Local `supabase` + linked remote after CLEAN | Reverse migration of RPC bodies |

**Before apply:** choose chain strategy (`stacked-to-main` vs `feature-branch-chain` vs `size:exception`).
**Reconcile note (2026-09-15):** Implementation already on `fix/jd-reportes-cluster` @ `65ba2d71` (PR #136 CI green). This apply batch only marks checkboxes against HEAD evidence; no code re-implementation.

---

## Phase 1: Static user-facing errors (S1 + S3)

- [x] 1.1 RED: Update `optoapp/src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt` so load failure asserts static `errorMessage` and MUST NOT contain `"DB corrupted"`
- [x] 1.2 GREEN: Change `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt` to set a static Spanish `errorMessage` and `Log.e` the raw exception
- [x] 1.3 RED: Add/adjust assertions in `optoapp/src/test/java/com/example/optoapp/domain/SyncFinanzasUseCaseKtTest.kt` that IO/generic failures return static `Resource.Error` (no `localizedMessage` embedding)
- [x] 1.4 GREEN: Change `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasUseCase.kt` catch paths to static `Resource.Error` strings; keep partial-upload static message; log raw exception

## Phase 2: AnalisisNegocio UI (S4 + S6)

- [x] 2.1 RED (optional): SKIPPED — `AnalisisNegocioScreenTest.kt` is string/UiState smoke only; no Compose role-null harness; GREEN 2.2 already mirrors CierreCaja
- [x] 2.2 GREEN: In `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt` set `opticaRol` `collectAsState(initial = null)` and show progress while null (mirror `CierreCajaScreen`)
- [x] 2.3 GREEN: In `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt` drive "Gastos del mes" emptiness check and `forEach` from `gastosMes` (not unfiltered `gastos`)

## Phase 3: ResumenDiarioDao deleteAll (I2 + I1)

- [x] 3.1 RED: Rewrite `deleteAll_clearsData` in `optoapp/src/test/java/com/example/optoapp/data/resumendiario/ResumenDiarioDaoTest.kt` to call `dao.deleteAll()` (keep existing Robolectric + in-memory Room; do not invent a new harness)
- [x] 3.2 GREEN: Add `suspend fun deleteAll()` with `@Query("DELETE FROM resumen_diario")` to `optoapp/src/main/java/com/example/optoapp/data/resumendiario/ResumenDiarioDao.kt`
- [x] 3.3 Document I1 as known repo-wide Robolectric DAO-test debt in `IMPROVEMENT-PLAN.md` (or a short note under this change folder if IMPROVEMENT-PLAN is inappropriate)

## Phase 4: Reportes dead period branches (I3)

- [x] 4.1 RED/rename: Update `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelOtrosPeriodosTest.kt` — rename misleading `"Este año"` test names to `"Anual"` where they already call `setPeriodo("Anual")`; add/adjust coverage that dead labels are unused
- [x] 4.2 GREEN: Remove `"Este año"` (and any unused `"Este mes"` / `"Todo"`) branches from `dentroDelPeriodo` / `periodDateRange` in `optoapp/src/main/java/com/example/optoapp/viewmodel/ReportesViewModel.kt`; keep `Mensual|Anual|Total` behavior aligned with `ReportesScreen` options
- [x] 4.3 Confirm delta already written: `openspec/changes/fix-jd-reportes-cluster/specs/reportes-financieros/spec.md` (read-only) — archive later merges labels into main specs

## Phase 5: Supabase RPCs (S2 + S5 + I4) — GGA required

- [x] 5.1 Grep optoweb / edge for `rpc_cierre_caja_resumen` exclusive `p_to` callers (sibling repo paths read-only); record findings in apply notes
- [x] 5.2 Create `supabase/migrations/<timestamp>_document_rpc_cierre_caja_resumen_exclusive.sql` with `CREATE OR REPLACE` COMMENT documenting half-open `[p_from, p_to)` (keep body; optoweb exclusive callers)
- [x] 5.3 Create `supabase/migrations/<timestamp>_fix_rpc_analisis_mensual_stock_proyeccion.sql`: REPLACE `rpc_analisis_mensual` using body from `supabase/migrations/20260815010805_converge_readonly_rpc_pago_effect.sql` (read-only) and stock CTE from `supabase/migrations/20260709000003_fix_analisis_mensual_categorias.sql` (read-only); NULL-safe `v_proyeccion` with `egresos_programados`; preserve pago_effect paths
- [x] 5.4 Optionally extend ledger SQL tests: SKIPPED — keep exclusive cierre contract; remote verify via execute_sql covered 5.6 instead of inclusive assertion
- [x] 5.5 Run GGA R1/R3/R4 on the new migration(s); resolve ALL observations to CLEAN
- [x] 5.6 Remote-apply migrations via normal Supabase path after CLEAN; verify same-day cierre + R23 stock + proyeccion with zero unpaid

## Phase 6: Verification

- [x] 6.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` (full suite green before Android PR merge)
- [x] 6.2 Spot-check UX: Cierre error string static; Análisis no admin flash; Gastos del mes list; Reportes periods only Diario|Semanal|Mensual|Anual|Total
- [x] 6.3 After SQL apply: confirm remote function defs match exclusive COMMENT + restored stock + COALESCE proyeccion
