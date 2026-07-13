# Tasks: Complete Deferred Financial Analysis Items

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~400 (widget is boilerplate) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

### Suggested Work Units

All units delivered as a single PR (size-exception required).

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| A1 | RPC cost recalculation (migration SQL) | PR 1 | Base: main |
| A2 | meses_historicos model + UI warning | PR 1 | Same PR, after A1 |
| B1 | Widget (provider + worker + layout) | PR 1 | Independent, same PR |
| B2 | Doc sync (PARTE-B-COMPLETA.md) | PR 1 | Trivial, same PR |

## Phase 1: RED Tests (TDD)

- [x] 1.1 Write SQL test fixture verifying `recalcular_resumen_diario()` sums `costo_real_*` and falls back to `costo_unitario_snapshot`
- [x] 1.2 Write SQL test fixture verifying `rpc_analisis_mensual` returns `meses_historicos` count
- [x] 1.3 Write `AnalisisMensualMapperTest` — RPC JSON with/without `meses_historicos` maps correctly to `ProyeccionCaja`
- [x] 1.4 Write `AnalisisDetalleScreenTest` — `ProyeccionCard` warning shown when `mesesHistoricos < 3`, hidden when ≥ 3
- [x] 1.5 Write `ResumenDiarioDaoTest` — `getByOpticaAndDate()` returns row for today, null for missing date
- [x] 1.6 Write `MiNegocioWidgetWorkerTest` — worker reads DAO on `Dispatchers.IO`, calls `updateAppWidget`

## Phase 2: Migration (Supabase SQL)

- [x] 2.1 `20260713034249_completar_diferidos_financieros.sql` — `CREATE OR REPLACE recalcular_resumen_diario()` with LATERAL subquery on `dispensacion_items.costo_real_*` + COALESCE fallback to `costo_unitario_snapshot`
- [x] 2.2 Same migration — `CREATE OR REPLACE rpc_analisis_mensual()` adding `meses_historicos` via `COUNT(DISTINCT DATE_TRUNC('month', fecha))` from `resumen_diario`
- [x] 2.3 Add `GRANT EXECUTE ... TO authenticated, service_role` for both functions

## Phase 3: Model + UI Warning

- [x] 3.1 `AnalisisMensual.kt` — Add `mesesHistoricos: Int = 0` to `ProyeccionCaja` data class + parse `"meses_historicos"` from RPC JSONB response
- [x] 3.2 `AnalisisDetalleScreen.kt` — Show `WarningAmber` banner in `ProyeccionCard` when `mesesHistoricos < 3`

## Phase 4: Widget "Mi Negocio"

- [x] 4.1 `widget_mi_negocio.xml` + `widget_mi_negocio_info.xml` — RemoteViews layout with "Hoy: S/ X" and "Por cobrar: S/ Y"
- [x] 4.2 `ResumenDiarioDao.kt` — Add `getByOpticaAndDate(opticaId: String, fecha: String): ResumenDiarioEntity?`
- [x] 4.3 `MiNegocioWidgetProvider.kt` — `@AndroidEntryPoint(AppWidgetProvider::class)` with CoroutineScope Room read + PendingIntent to `MainActivity`
- [x] 4.4 `MiNegocioWidgetWorker.kt` — `@HiltWorker CoroutineWorker` reading DAO, calling `AppWidgetManager.updateAppWidget()`
- [x] 4.5 `AndroidManifest.xml` — Add `<receiver>` for `MiNegocioWidgetProvider` with `<meta-data>` for widget info
- [x] 4.6 `OptoApplication.kt` — Schedule `PeriodicWorkRequest(6h)` with `ExistingPeriodicWorkPolicy.KEEP` in `onCreate()`

## Phase 5: Doc Sync

- [x] 5.1 `PARTE-B-COMPLETA.md` — Mark feedback 👍/👎 (line 93) ✅ and margen S/100 wording ✅

## Phase 6: VERIFY

- [x] 6.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests GREEN, no regressions
- [x] 6.2 Verify migration SQL applies cleanly (local `supabase db diff --linked` or manual review)
