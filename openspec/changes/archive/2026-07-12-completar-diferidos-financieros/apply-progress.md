# Apply Progress: completar-diferidos-financieros

**Mode**: Strict TDD | **Date**: 2026-07-13 | **Status**: Complete (20/20)

## TDD Cycle Evidence

| Task | Phase | RED (Test) | GREEN (Impl) | REFACTOR | Status |
|------|-------|-----------|-------------|----------|--------|
| 1.1 | SQL cost fixture | `supabase/tests/test_costos_reales.sql` — DO/ASSERT on recalcular_resumen_diario | Migration `20260713034249` — CREATE OR REPLACE with LATERAL subquery | COALESCE fallback pattern | ✅ |
| 1.2 | SQL meses fixture | `supabase/tests/test_meses_historicos.sql` — DO/ASSERT on rpc_analisis_mensual | Same migration — COUNT(DISTINCT DATE_TRUNC) | GRANT EXECUTE pattern | ✅ |
| 1.3 | Mapper test | `AnalisisMensualMapperTest.fromJson_withMesesHistoricos_parsesCorrectly` | `ProyeccionCaja.mesesHistoricos: Int = 0` + parse in `parseProyeccionCaja()` | Top-level read with null obj fallback | ✅ |
| 1.4 | UI warning test | `AnalisisDetalleScreenTest.warningShown_whenMesesHistoricosIsOne` (4 cases) | `ProyeccionCard` WarningAmber banner when < 3 | Inline below saldoNeto | ✅ |
| 1.5 | DAO test | `ResumenDiarioDaoTest` — `getByOpticaAndDate` returns row/null | `ResumenDiarioDao.getByOpticaAndDate()` query | LIMIT 1 pattern | ✅ |
| 1.6 | Worker test | `MiNegocioWidgetWorkerTest.doWorkCore_withEntity_updatesWidgetWithData` | `MiNegocioWidgetWorker.kt` — @HiltWorker CoroutineWorker | `doWorkCore()` extracted for testability with appWidgetIds param | ✅ |
| 2.1 | Migration cost | Same as 1.1 | LATERAL subquery: JOIN dispensaciones → items, SUM 5 costo_real_* | COALESCE to costo_unitario_snapshot | ✅ |
| 2.2 | Migration meses | Same as 1.2 | `meses_historicos INTEGER` in RPC JSON via `jsonb_build_object` | REVOKE public + GRANT authenticated | ✅ |
| 2.3 | Migration grants | (included in 2.1/2.2) | GRANT EXECUTE ON FUNCTION TO authenticated, service_role | ✅ |
| 3.1 | Model field | Same as 1.3 | `ProyeccionCaja.mesesHistoricos: Int = 0` | Default 0 for backward compat | ✅ |
| 3.2 | UI warning | Same as 1.4 | WarningAmber surface in ProyeccionCard, 4 test boundary cases | ✅ |
| 4.1 | Widget layout | Visual inspection | `widget_mi_negocio.xml` — LinearLayout + 2 TextViews | ✅ |
| 4.2 | Widget DAO query | Same as 1.5 | `getByOpticaAndDate` in ResumenDiarioDao | ✅ |
| 4.3 | Widget provider | Compilation check | `@AndroidEntryPoint(AppWidgetProvider::class)`, CoroutineScope Room read | Import fix: dagger.hilt.android | ✅ |
| 4.4 | Widget worker | Same as 1.6 | @HiltWorker + @AssistedInject, doWorkCore refactored for testability | appWidgetIds parameter extracted | ✅ |
| 4.5 | Manifest | Compilation check | `<receiver>` for MiNegocioWidgetProvider in AndroidManifest.xml | ✅ |
| 4.6 | WorkManager | Compilation check | PeriodicWorkRequestBuilder(6h) in OptoApplication.onCreate() | ✅ |
| 5.1 | Doc sync | Visual inspection | PARTE-B-COMPLETA.md: feedback ✅, margen ✅, deferred list updated | ✅ |
| 6.1 | Test suite | `./gradlew :optoapp:testDebugUnitTest` | 1836 tests, 0 failures | ✅ |
| 6.2 | Migration verify | `supabase migration list` | 181 matched, 0 drift | ✅ |

## Post-Verify Fixes (2026-07-13)

| Issue | Fix | File |
|-------|-----|------|
| Mapper null object when proyeccion_caja missing | parseProyeccionCaja(): create minimal ProyeccionCaja even without nested obj if meses present | `AnalisisMensual.kt` |
| Widget worker ClassCastException with mockk Context | Use ApplicationProvider.getApplicationContext() + pass appWidgetIds as parameter | `MiNegocioWidgetWorker.kt`, `MiNegocioWidgetWorkerTest.kt` |
| Import error (androidx.hilt → dagger.hilt) | Fixed import to dagger.hilt.android.AndroidEntryPoint | `MiNegocioWidgetProvider.kt` |
| Smoke-only widget tests (verify WARNING) | Added `assertEquals(expected, views?.getString(...))` assertions | `MiNegocioWidgetWorkerTest.kt` |
| SQL test fixtures missing (verify CRITICAL) | Created test_costos_reales.sql + test_meses_historicos.sql | `supabase/tests/` |

## Files Changed (cumulative)

| File | Action |
|------|--------|
| `supabase/migrations/20260713034249_completar_diferidos_financieros.sql` | Created |
| `optoapp/.../domain/AnalisisMensual.kt` | Modified |
| `optoapp/.../ui/screens/AnalisisDetalleScreen.kt` | Modified |
| `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` | Modified |
| `optoapp/.../widget/MiNegocioWidgetProvider.kt` | Created |
| `optoapp/.../widget/MiNegocioWidgetWorker.kt` | Created |
| `optoapp/src/main/res/layout/widget_mi_negocio.xml` | Created |
| `optoapp/src/main/res/xml/widget_mi_negocio_info.xml` | Created |
| `optoapp/src/main/AndroidManifest.xml` | Modified |
| `optoapp/.../OptoApplication.kt` | Modified |
| `openspec/PARTE-B-COMPLETA.md` | Modified |
| `supabase/tests/test_costos_reales.sql` | Created (post-verify) |
| `supabase/tests/test_meses_historicos.sql` | Created (post-verify) |
| `optoapp/.../widget/MiNegocioWidgetWorkerTest.kt` | Enhanced (post-verify) |
| `optoapp/.../domain/AnalisisMensualMapperTest.kt` | Modified (post-verify) |
