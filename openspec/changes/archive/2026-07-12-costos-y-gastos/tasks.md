# Tasks: Costos y Gastos

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1200 (7 new + 14 modified files, 6 tests, navigation/drawer) |
| 400-line budget risk | High |
| Chained PRs recommended | No (user granted size-exception) |
| Suggested split | Single PR (exception accepted) |
| Delivery strategy | single-pr (size-exception) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: High

## Corrections from Verification

The following corrections were applied to this version versus the original tasks:

1. **Missing navigation tasks added** — CostosYGastosScreen needs a route in the NavHost (both standalone and filtered by dispensacionId), plus a drawer menu entry in MainDrawerContent.kt under "FINANZAS" section. **Task 4.3 split** — original NuevaDispensacionScreen changes are 4.3, new navigation is 4.4.

2. **UploadSyncCoordinator + DownloadSyncCoordinator tasks added** — SyncFinanzasUseCase delegates to these coordinators. Both need new methods (`uploadCostosProductos`, `downloadCostosProductos`, `downloadCostosBiselado`). Original tasks only mentioned SyncFinanzasUseCase. **New task 3.2a/3.2b.**

3. **FinanzasSyncResult fields added** — New counters for `uploadedCostosProductos`, `downloadedCostosProductos`, `downloadedCostosBiselado` are needed in the result class. **Part of task 3.1.**

4. **DispensacionRemota/DispensacionItemRemota DTO updates added** — Existing remote DTOs need new fields for `evaluacion_id` (DispensacionRemota) and the 9 new spec/cost columns (DispensacionItemRemota). The `toEntity()`/`toRemoto()` mapping functions must be updated. **Part of task 3.1.**

5. **DispensacionViewModel modification task added** — Cost calculation logic (link evaluacion_id → read receta → determine stock vs fabricación → series lookup → auto-fill costs) goes here. **New task 4.1** — was previously folded into the generic "create ViewModel" task.

6. **OptoRepository constructor + DatabaseModule wiring** — Constructor needs new `CostoProductoDao` and `CostoBiseladoDao` params. `DatabaseModule.provideOptoRepository` call site needs updating. **Part of task 3.3 + 2.7.**

7. **Existing GastosScreen preserved** — The current `route("gastos")` → `GastosScreen` has NO drawer entry (confirmed by inspecting MainDrawerContent.kt — no "Gastos" item exists). The new CostosYGastosScreen is additive; GastosScreen remains until a future consolidation.

8. **Test framework confirmed Robolectric** — All 40+ DAO test files use `@RunWith(RobolectricTestRunner::class)` with `Room.inMemoryDatabaseBuilder`. Migration tests use `FrameworkSQLiteOpenHelperFactory`. The original tasks were correct.

## Verified File State

| # | File | Status | Notes |
|---|------|--------|-------|
| 1 | `data/dispensacion/DispensacionEntity.kt` | EXISTS | `DispensacionOptica` in `com.example.optoapp.data` |
| 2 | `data/dispensacion/DispensacionItemEntity.kt` | EXISTS | `DispensacionItem` in `com.example.optoapp.data` |
| 3 | `data/OptoDatabase.kt` | v38 | Bump → v39 |
| 4 | `data/OptoDatabaseMigrations.kt` | Last: MIGRATION_37_38 | Add MIGRATION_38_39 |
| 5 | `domain/SyncFinanzasDto.kt` | EXISTS | `com.example.optoapp.domain` |
| 6 | `domain/SyncFinanzasUseCase.kt` | EXISTS | `com.example.optoapp.domain` |
| 7 | `data/OptoRepository.kt` | EXISTS | `com.example.optoapp.data`, `open class OptoRepository` |
| 8 | `di/DatabaseModule.kt` | EXISTS | `com.example.optoapp.di` |
| 9 | `ui/screens/NuevaDispensacionScreen.kt` | EXISTS | Uses `LenteForm`, `DispensacionViewModel` |
| 10 | `domain/UploadSyncCoordinator.kt` | EXISTS | Delegates for uploads |
| 11 | `domain/DownloadSyncCoordinator.kt` | EXISTS | Delegates for downloads |

## New Packages to Create

- `com.example.optoapp.data.costoproducto` → `CostoProductoEntity`, `CostoProductoDao`
- `com.example.optoapp.data.costobiselado` → `CostoBiseladoEntity`, `CostoBiseladoDao`

## Phase 1: RED — Write 6 Failing Tests (TDD)

- [x] 1.1 Write `Migration38_39Test` — verify matrix columns on costos_productos, costos_biselado table, ALTER columns on dispensaciones/items, data preserved (T1)
- [x] 1.2 Write `CostoProductoDaoTest` — lookup by (mat,tipo,stock,trat,serie), upsertAll, getByBloque (T2)
- [x] 1.3 Write `CostoBiseladoDaoTest` — lookup by (mat,tipoAro,stock,serie,altoIndice), empty fallback (T3)
- [x] 1.4 Write `SyncFinanzasCostosTest` — CostoProductoRemoto/CostoBiseladoRemoto serialization, download order, upload vs download-only semantics (T4)
- [x] 1.5 Write `CostosYGastosScreenTest` — 2 tabs render, block dropdown works (T5)
- [x] 1.6 Write `DispensacionViewModelCostosTest` — auto-fill from evaluacion, override persists (T6)

## Phase 2: GREEN — Foundation (DB → Entities → DAOs)

- [x] 2.1 Create `supabase/migrations/20260712000001_costos_matriz.sql` — DROP old costos_productos, CREATE matrix schema + costos_biselado + ALTER dispensaciones/items + RLS (D1)
- [x] 2.2 Create `data/costoproducto/CostoProductoEntity.kt` + `data/costobiselado/CostoBiseladoEntity.kt` (D3)
- [x] 2.3 Modify `DispensacionEntity.kt` — add `evaluacionId: String?` (nullable); modify `DispensacionItemEntity.kt` — add 9 fields: `altoIndice`, `reduccionDiametro`, `lenticular`, `curvaBase` (all String?), `costoRealOd`, `costoRealOi`, `costoRealMontura`, `costoRealBiselado`, `costoRealLc` (all Double?) (D3)
- [x] 2.4 Create `CostoProductoDao.kt` — `lookup()`, `getByBloque()`, `upsertAll()` + `CostoBiseladoDao.kt` — `lookup()`, `upsertAll()` (D4)
- [x] 2.5 Modify `OptoDatabase.kt` — bump v38→39, add CostoProductoEntity, CostoBiseladoEntity, both DAOs
- [x] 2.6 Modify `OptoDatabaseMigrations.kt` — add `MIGRATION_38_39` (camelCase ALTER TABLE matching Room columns) (D2)
- [x] 2.7 Modify `di/DatabaseModule.kt` — add `provideCostoProductoDao()`, `provideCostoBiseladoDao()`, update `provideOptoRepository()` with new DAO constructor params

## Phase 3: GREEN — Sync + Repository

- [x] 3.1 Modify `SyncFinanzasDto.kt` — add:
  - `CostoProductoRemoto` + `CostoBiseladoRemoto` with `@SerialName` mappings
  - New fields on `DispensacionRemota`: `evaluacion_id` (nullable)
  - New fields on `DispensacionItemRemota`: `alto_indice`, `reduccion_diametro`, `lenticular`, `curva_base`, `costo_real_od`, `costo_real_oi`, `costo_real_montura`, `costo_real_biselado`, `costo_real_lc` (all nullable)
  - Update `DispensacionRemota.toEntity()`, `DispensacionOptica.toRemoto()`, `DispensacionItemRemota.toEntity()`, `DispensacionItem.toRemoto()` with all new fields
  - Update `FinanzasSyncResult` with counters: `uploadedCostosProductos`, `downloadedCostosProductos`, `downloadedCostosBiselado`
- [x] 3.2a Modify `UploadSyncCoordinator.kt` — add `uploadCostosProductos(opticaId)` method
- [x] 3.2b Modify `DownloadSyncCoordinator.kt` — add `downloadCostosProductos(opticaId)` + `downloadCostosBiselado(opticaId)` methods with table constants
- [x] 3.2c Modify `SyncFinanzasUseCase.kt` — call new coordinator methods in correct order: upload: costos_productos after dispensaciones, before pagos; download: costos_productos AND costos_biselado after pagos, before gastos
- [x] 3.3 Modify `OptoRepository.kt` — add constructor params for `CostoProductoDao` + `CostoBiseladoDao`, add passthrough methods for both DAOs (pattern: `getCostosProductos()`, `upsertCostoProductoFromRemote()`, `getCostosBiselado()`, `upsertCostoBiseladoFromRemote()`)

## Phase 4: GREEN — ViewModel + UI + Navigation

- [x] 4.1 Modify `DispensacionViewModel.kt` — add cost calculation logic:
  - Added `evaluacionId`, `evaluacionesDisponibles` to UiState
  - Added cost fields (`costoRealOd/Oi/Montura/Biselado/Lc`) to `DispensacionItemUi`
  - Added `setEvaluacionId()`, `calculateCosts()`, `loadEvaluacionesDisponibles()`
  - Companion functions `determineTipoLente()`, `determineSeriePorCilindro()`
  - Updated `toUi()` and `saveDispensacion()` to flow cost fields through items
- [x] 4.2 Create `viewmodel/CostosYGastosViewModel.kt` — matrix block loading, cost lookup, manual override, gastos operativos CRUD
- [x] 4.3 Create `ui/screens/CostosYGastosScreen.kt` — 2 tabs: Tab 1 matrix (8 blocks + costos por orden filtered by dispensacionId), Tab 2 gastos operativos CRUD
- [x] 4.4 Modify `MainDrawerScreen.kt` + `DrawerSections.kt` — add:
  - Routes `"costos_y_gastos"` and `"costos_y_gastos/{dispensacionId}"` in NavHost
  - Drawer menu item under FINANZAS section (after "Reportes", gated by `showBiYReportes`)
- [x] 4.5 Modify `NuevaDispensacionScreen.kt` — add:
  - Evaluación vinculada section with dropdown + prisma read-only display
  - Collapsible item cards (2-line summary per item with toggle expand)
  - `[Gestionar costos →]` button navigating to `"costos_y_gastos/{dispensacionId}"`
- [x] 4.6 Deprecate standalone GastosScreen — keep route `"gastos"` working with `@Deprecated` annotation. CostosYGastosScreen Tab 2 is the new home.

## Phase 5: VERIFY

- [x] 5.1 Verify all 6 RED tests pass (T1–T6)
- [x] 5.2 Run full regression: `./gradlew :optoapp:testDebugUnitTest --stacktrace` → BUILD SUCCESSFUL, 0 failures, 0 warnings

## Implementation Order

Foundation first (migration → entities → DAOs → migration code) before any sync or UI code. Sync wiring follows DAOs. ViewModels need DAOs + Sync. UI depends on ViewModel. Navigation is last since it adds the screen entry points. Tests written first (RED), verified last (GREEN).

## Review Notes

- Room v38→39: MIGRATION_38_39 MUST mirror Supabase SQL with camelCase column names for Room compatibility
- `reduccion_diametro` and `lenticular` as TEXT (nullable) per design — NOT BOOLEAN
- Biselado lookup: no-match leaves field empty (R4), not a crash
- Cost override persists even if matrix changes later (R6)
- All existing tests use `@RunWith(RobolectricTestRunner::class)` — confirm new tests follow same pattern
- New DAO tests use `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()` pattern from GastoOperativoDaoTest
- `data/costoproducto/` and `data/costobiselado/` are NEW packages — use existing `com.example.optoapp.data.categoriaproducto/` as structural reference
- `MIGRATION_38_39` ALTER TABLE on Room entities must target Room column names: `evaluacion_id` (DispensacionOptica uses camelCase `evaluacionId` → Room stores as `evaluacionId`), `alto_indice`, `reduccion_diametro`, `lenticular`, `curva_base`, `costo_real_od`, `costo_real_oi`, `costo_real_montura`, `costo_real_biselado`, `costo_real_lc` (DispensacionItem uses `@ColumnInfo` with snake_case names → ALTER TABLE uses those names)
