# Design: Fix JD Reportes Cluster

## Technical Approach

Strict TDD across five work units from exploration: static Android errors → Análisis UI → ResumenDiarioDao → Reportes dead branches → new Supabase RPC migrations (GGA then remote). Specs: `cierre-caja`, `sync`, `analisis-negocio`, `reportes-financieros` deltas. No applied-history edits.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|----------|---------|--------|-----------|
| S2 date bound | Inclusive `<= p_to` vs keep exclusive | Inclusive | Matches cierre same-day; no Android callers; COMMENT + optoweb grep |
| S5 stock | Restore `20260709000003` CTE vs rewrite | Restore CTE into current REPLACE | R23 already reviewed; preserve pago_effect body |
| I4 proyeccion | COALESCE object vs split SELECT | COALESCE / scalar egresos | Survives zero unpaid `SELECT INTO` NULL |
| S5+I4 packaging | One vs two migrations | Prefer one REPLACE for `rpc_analisis_mensual` + companion for `rpc_cierre_caja_resumen` | Shared review; cierre RPC separate file OK |
| I1 Robolectric | Convert harness vs keep | Keep Robolectric; document debt | All 18 DaoTests use it; `ApplicationProvider` needs runner — no new harness in this change |
| Delivery | Single PR vs chain | Chain Android then SQL | >400-line risk; SQL needs GGA |

## Data Flow

```
WU1: Exception ──Log.e(raw)──▶ static Resource.Error / errorMessage ──▶ UI
WU2: auth.opticaRol(null) ──progress──▶ role resolve ──▶ canView / deny
     gastos ──filter──▶ gastosMes ──▶ "Gastos del mes" rows + total
WU3: ResumenDiarioDao.deleteAll() ──▶ empty local resumen_diario
WU4: periodo ∈ {Diario,Semanal,Mensual,Anual,Total} ──periodDateRange──▶ DAO
WU5: rpc_cierre_caja_resumen(p_from,p_to inclusive)
     rpc_analisis_mensual ──stock CTE + COALESCE proyeccion──▶ JSONB
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/CierreCajaViewModel.kt` | Modify | Static `errorMessage`; Log.e keeps `e` |
| `optoapp/.../viewmodel/CierreCajaViewModelTest.kt` | Modify | Assert static string; not `"DB corrupted"` |
| `optoapp/.../domain/SyncFinanzasUseCase.kt` | Modify | Static `Resource.Error` on IO/generic catch |
| `optoapp/.../domain/SyncFinanzasUseCaseKtTest.kt` | Modify | Assert static messages if present |
| `optoapp/.../ui/screens/AnalisisNegocioScreen.kt` | Modify | `initial=null` + guard; iterate `gastosMes` |
| `optoapp/.../ui/AnalisisNegocioScreenTest.kt` | Modify/Create | Optional role-null / gastosMes coverage |
| `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` | Modify | Add `@Query("DELETE FROM resumen_diario") suspend fun deleteAll()` |
| `optoapp/.../data/resumendiario/ResumenDiarioDaoTest.kt` | Modify | Call `dao.deleteAll()`; keep Robolectric |
| `optoapp/.../viewmodel/ReportesViewModel.kt` | Modify | Drop `"Este año"` branches |
| `optoapp/.../viewmodel/ReportesViewModelOtrosPeriodosTest.kt` | Modify | Rename misleading Este año names |
| `supabase/migrations/<ts>_fix_rpc_cierre_caja_resumen_inclusive.sql` | Create | `fecha <= p_to` + COMMENT |
| `supabase/migrations/<ts>_fix_rpc_analisis_mensual_stock_proyeccion.sql` | Create | R23 CTE + NULL-safe proyeccion |
| `supabase/tests/test_ledger_aggregate_convergence.sql` | Modify | Optional same-day / proyeccion assertions |
| `IMPROVEMENT-PLAN.md` or change note | Modify | Record Robolectric DAO harness debt (I1) |

## Interfaces / Contracts

```kotlin
// ResumenDiarioDao
@Query("DELETE FROM resumen_diario")
suspend fun deleteAll()

// Cierre / Sync — illustrative static strings (exact copy may match existing Spanish UX)
"Error al cargar datos"           // no : ${e.message}
"Error sincronizando finanzas"    // no ${e.localizedMessage}
```

```sql
-- rpc_cierre_caja_resumen
AND fecha >= p_from AND fecha <= p_to

-- proyeccion: never leave v_proyeccion NULL before jsonb_set
v_proyeccion := COALESCE(v_proyeccion, jsonb_build_object(
  'ingresos_esperados', 0,
  'egresos_programados', <egresos_scalar>,
  'saldo_neto', 0
));
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | S1/S3/I3 ViewModel+UseCase | JUnit4 + MockK + runTest; red→green |
| Unit/DAO | I2 deleteAll | In-memory Room + existing Robolectric runner |
| Compose (opt) | S4/S6 | Existing screen test patterns if cheap |
| SQL | S2/S5/I4 | Migration SQL tests / ledger convergence assertions |
| Gate | Full suite | `./gradlew :optoapp:testDebugUnitTest --stacktrace` |
| Gate | SQL PR | GGA R1/R3/R4 CLEAN then remote apply |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Migrations applied via normal Supabase CLI path after GGA.

## Migration / Rollout

1. PR-A (Android WU1–4): merge independently; no DB.
2. PR-B (WU5): new migration files only → GGA → `db push` / linked CI → verify same-day cierre + stock + proyeccion with zero unpaid.
3. Rollback: Android revert; SQL reverse migration restoring prior function bodies from `20260815005859` / `20260815010805`.

## Open Questions

- [x] Inclusive S2 default — confirmed by explore/user handoff
- [ ] Grep optoweb for exclusive `p_to` callers before remote apply (apply-time check, not blocking design)
