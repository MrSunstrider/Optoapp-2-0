# Design: Completar Diferidos Financieros

## Technical Approach

Four independent work units: (A1) RPC cost recalculation in a new Supabase migration using `CREATE OR REPLACE` on both affected functions; (A2) `meses_historicos` field in model + RPC + ProyeccionCard warning; (B1) Android widget with RemoteViews + WorkManager polling; (B2) doc sync of status markers. All are additive — no schema or Room migration needed.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|-------------|-----------|
| RPC migration | New file (`20260713000000`) via `supabase migration new` | Modify existing migrations | Existing migrations already applied; new file MUST have timestamp AFTER last applied (20260712015048). Follows naming convention YYYYMMDDHHmmSS from migration README |
| Cost source | LATERAL subquery in `recalcular_resumen_diario()` | CTE, temp table, separate function | Single-pass aggregation per venta; no intermediate storage; spec-compliant fallback via COALESCE |
| Widget DI | `@AndroidEntryPoint(AppWidgetProvider::class)` + `@HiltWorker` for worker | `EntryPointAccessors.fromApplication()` | Provider injection works for `BroadcastReceiver` subclasses in Hilt 2.59; worker gets clean constructor injection |
| Widget layout | XML `RemoteViews` | Jetpack Compose `ComposeViews` (API 33+) | Compose widget requires API 33+; min SDK is 24 — XML is universally compatible |
| Warning placement | Inline in `ProyeccionCard` after saldoNeto | Separate card above/below, ViewModel-level flag | Keeps decision local to the composable; no ViewModel changes needed; reuses existing `WarningAmber` theme color |
| WorkManager scheduling | `OptoApplication.onCreate()` | `onEnabled()` in provider, lazy init | Guarantees schedule survives reboot; run once at app start (KEEP policy prevents duplicates) |

## Data Flow

```
┌──────────────┐     ┌──────────────────────────────┐
│  Widget Tap  │────→│  Intent → MainActivity       │
│  (HomeScreen) │     │  → navigates to              │
│              │     │  AnalisisNegocioScreen        │
└──────────────┘     └──────────────────────────────┘

┌─────────────────┐     ┌──────────────────┐     ┌───────────────┐
│ MiNegocioWidget │────→│ MiNegocioWidget  │────→│ ResumenDiario │
│ Provider        │     │ Worker (6h)      │     │ DAO (Room)    │
│ (onUpdate)      │     │ @HiltWorker      │     │               │
└─────────────────┘     └──────────────────┘     └───────────────┘
       │                                                │
       └────────── Immediate read ──────────────────────┘
                    (CoroutineScope IO)

┌──────────────────┐     ┌──────────────────────────────┐
│ recalcular_resum │────→│ JOIN ventas → dispensaciones │
│ en_diario()      │     │ → dispensacion_items         │
│                  │     │ SUM(costo_real_*)            │
│                  │     │ COALESCE fallback →          │
│                  │     │   costo_unitario_snapshot    │
└──────────────────┘     └──────────────────────────────┘
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20260713034249_completar_diferidos_financieros.sql` | Create | `CREATE OR REPLACE` both RPCs via `supabase migration new`: cost from `dispensacion_items.costo_real_*` with fallback; add `meses_historicos` to `rpc_analisis_mensual`. Idempotent, no schema changes. Follows naming convention: YYYYMMDDHHmmSS via `supabase migration new` |
| `optoapp/.../domain/AnalisisMensual.kt` | Modify | `ProyeccionCaja.mesesHistoricos: Int = 0` + parse from RPC JSON |
| `optoapp/.../ui/screens/AnalisisDetalleScreen.kt` | Modify | `ProyeccionCard` shows `WarningAmber` banner when `mesesHistoricos < 3` |
| `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` | Modify | Add `getByOpticaAndDate(opticaId, fecha): ResumenDiarioEntity?` |
| `optoapp/src/main/AndroidManifest.xml` | Modify | `<receiver>` for `MiNegocioWidgetProvider` with widget info meta-data |
| `optoapp/.../OptoApplication.kt` | Modify | Schedule `PeriodicWorkRequest(6h)` for widget refresh in `onCreate()` |
| `openspec/PARTE-B-COMPLETA.md` | Modify | Mark feedback line (93) ✅, remove widget/margen/estacionalidad from deferred |
| `optoapp/.../widget/MiNegocioWidgetProvider.kt` | Create | `@AndroidEntryPoint(AppWidgetProvider::class)`, `onUpdate()` with coroutine Room read + `PendingIntent` to `MainActivity` |
| `optoapp/.../widget/MiNegocioWidgetWorker.kt` | Create | `@HiltWorker CoroutineWorker`, reads DAO, calls `AppWidgetManager.updateAppWidget()` |
| `optoapp/src/main/res/layout/widget_mi_negocio.xml` | Create | LinearLayout + 2 TextViews ("Hoy: S/ X", "Por cobrar: S/ Y") |
| `optoapp/src/main/res/xml/widget_mi_negocio_info.xml` | Create | Widget metadata (minWidth, updatePeriodMillis=0, initialLayout) |

### RPC Cost Logic (key snippet)

```sql
COALESCE(SUM(
    COALESCE((
        SELECT SUM(
            COALESCE(di.costo_real_od, 0) + COALESCE(di.costo_real_oi, 0) +
            COALESCE(di.costo_real_montura, 0) + COALESCE(di.costo_real_biselado, 0) +
            COALESCE(di.costo_real_lc, 0)
        ) FROM public.dispensaciones d
        JOIN public.dispensacion_items di ON di.dispensacion_id = d.id
        WHERE d.venta_id = v.id
    ), v.costo_unitario_snapshot, 0)
), 0) INTO v_ventas_costo
FROM public.ventas v
WHERE v.optica_id = p_optica_id AND v.fecha = p_fecha;
```

## Interfaces / Contracts

### ProyeccionCaja model (Kotlin)

```kotlin
data class ProyeccionCaja(
    val ingresosEsperados: Double,
    val egresosProgramados: Double,
    val saldoNeto: Double,
    val mesesHistoricos: Int = 0  // ADDED
)
```

### ResumenDiarioDao (new query)

```kotlin
@Query("""
    SELECT * FROM resumen_diario
    WHERE opticaId = :opticaId AND fecha = :fecha
    LIMIT 1
""")
suspend fun getByOpticaAndDate(opticaId: String, fecha: String): ResumenDiarioEntity?
```

### WorkManager (schedule)

```kotlin
val request = PeriodicWorkRequestBuilder<MiNegocioWidgetWorker>(
    6, TimeUnit.HOURS
).build()
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "mi_negocio_widget_refresh",
    ExistingPeriodicWorkPolicy.KEEP, request
)
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit — Supabase | Cost fallback logic, meses_historicos count | SQL unit tests in new migration verify, via `supabase db test` or manual pgTAP if available |
| Unit — Model | `ProyeccionCaja.mesesHistoricos` default/parse | Extend `AnalisisMensualMapperTest` with RPC JSON fixture with/without `meses_historicos` |
| Unit — UI | Warning shown when < 3, hidden when >= 3 | Extend `AnalisisDetalleScreenTest` with `ProyeccionCard` test scenarios |
| Unit — Widget | DAO query returns row/null | `ResumenDiarioDaoTest` — add test for `getByOpticaAndDate` |
| Unit — Worker | Worker reads DAO, calls updateAppWidget | `@HiltWorker` test using `TestListenableWorkerBuilder` + mock DAO |
| Integration | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | Must pass with no regressions |

## Widget DI Approach

The `@AndroidEntryPoint(AppWidgetProvider::class)` injects `ResumenDiarioDao` and `SessionManager` into the provider. The worker uses `@HiltWorker` + `@AssistedInject`. Both access Room via `Dispatchers.IO` — the provider uses `CoroutineScope(Dispatchers.IO)` in `onUpdate()`, the worker runs on `Dispatchers.IO` natively as a `CoroutineWorker`.

## Migration / Rollout

**Order**: Apply Supabase migration first (RPCs independent), then Android code (backward-compatible: old RPC response missing `meses_historicos` defaults to 0).

**Rollback**: New migration reverting both functions to their previous body. Android: `git revert` — widget is new code, no data impact.

## Open Questions

- None. All decisions are resolved by reading the existing codebase.
