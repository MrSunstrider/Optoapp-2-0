# Proposal: Complete Deferred Financial Analysis Items

## Intent

Deliver 3 deferred Fase-9 items: real-cost RPC replacing stale snapshots, data-depth-aware seasonality warning, and an Android widget for daily metrics glance. Sync stale documentation with current code state.

## Scope

### In Scope

1. **Estacionalidad real** — `rpc_analisis_mensual` returns `meses_historicos`. `ProyeccionCard` shows warning when < 3 months data.
2. **Costos reales** — `recalcular_resumen_diario()` uses `costo_real_*` from `dispensacion_items` with fallback to `costo_unitario_snapshot` for `servicio_extra` ventas.
3. **Doc sync** — Mark feedback 👍/👎 and margen S/100 as ✅ in `PARTE-B-COMPLETA.md`.
4. **Widget "Mi Negocio"** — AppWidgetProvider with RemoteViews: "Hoy: S/ X" + "Por cobrar: S/ Y". WorkManager 6h refresh. Tap opens `AnalisisNegocioScreen`.

### Out of Scope

Widget on iOS/web. Feedback UI and margen wording (already done). `ProyectarFlujoCajaUseCase` (indefinitely deferred).

## Capabilities

### New Capabilities

- `widget-mi-negocio`: AppWidget showing daily sales + pending balance from Room via WorkManager, no Supabase calls

### Modified Capabilities

- `analisis-negocio`: RPCs `rpc_analisis_mensual` gains `meses_historicos`; `recalcular_resumen_diario()` switches cost source to `dispensacion_items.costo_real_*` with fallback. Model `ProyeccionCaja` gains `mesesHistoricos: Int`. `ProyeccionCard` gets conditional warning.

## Approach

**Supabase** — New migration: alter `recalcular_resumen_diario()` to JOIN `ventas → dispensaciones → dispensacion_items`, sum `costo_real_*` columns, add `costo_unitario_snapshot` for non-dispensacion rows. Add `meses_historicos` via `COUNT(DISTINCT fecha)` to `rpc_analisis_mensual`. **Android** — Add `mesesHistoricos` to `ProyeccionCaja`, wire warning in `ProyeccionCard` when < 3. Widget: RemoteViews + `AppWidgetProvider` + WorkManager polling `ResumenDiarioDao`. **Doc** — Edit `PARTE-B-COMPLETA.md` lines 93 and 99–101.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/` — Fase 6 + Fase 7 SQL | Modified | Cost logic + `meses_historicos` |
| `optoapp/.../domain/AnalisisMensual.kt` | Modified | `ProyeccionCaja.mesesHistoricos: Int` |
| `optoapp/.../ui/AnalisisDetalleScreen.kt` | Modified | `ProyeccionCard` warning |
| `optoapp/.../viewmodel/AnalisisNegocioViewModel.kt` | Modified | Propagate `mesesHistoricos` |
| `optoapp/.../widget/` | New | Provider, layout, worker |
| `optoapp/.../AndroidManifest.xml` | Modified | Widget `<receiver>` |
| `openspec/PARTE-B-COMPLETA.md` | Modified | ✅ feedback + margen |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `costo_real_*` NULL for older rows | Med | COALESCE + fallback to snapshot |
| Hilt + AppWidgetProvider injection | Med | Worker injects DAO, not provider |
| Widget stale if WorkManager unscheduled | Low | `onUpdate()` immediate refresh |

## Rollback Plan

1. **Supabase** — New migration restoring original RPC bodies (revert changed functions).
2. **Android** — `git revert` all changed files. Delete widget dir. Remove manifest `<receiver>`.
3. **Doc** — `git checkout` `PARTE-B-COMPLETA.md`.

## Dependencies

- Migration `20260712000001_costos_matriz.sql` (already applied — columns exist)
- Hilt `hilt.android.launcher` artifact for widget injection

## Success Criteria

- [ ] `recalcular_resumen_diario()` sums `costo_real_*` from `dispensacion_items`; falls back to snapshot for servicio_extra
- [ ] `rpc_analisis_mensual` returns `meses_historicos` matching distinct months in `resumen_diario`
- [ ] `ProyeccionCard` warns when `mesesHistoricos < 3`; no warning when ≥ 3
- [ ] Widget shows today's sales + pending from Room; tap opens `AnalisisNegocioScreen`
- [ ] `PARTE-B-COMPLETA.md` marks criterion #10 and margen wording as ✅
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes with no regressions
