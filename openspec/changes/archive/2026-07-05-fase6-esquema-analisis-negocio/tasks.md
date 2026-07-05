# Tasks: Fase 6 — Esquema de datos para análisis de negocio

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1100–1300 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Supabase SQL; PR 2: Room entities + DAOs + migration; PR 3: DI + repository + DTOs + sync + tests |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacking |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacking
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Supabase migration (DDL + RLS + seed + RPC) | PR 1 | Standalone SQL, no deps |
| 2 | Room entities + DAOs + MIGRATION_31_32 + seed constant | PR 2 | Depends on PR 1 conceptually; ~350 lines |
| 3 | DI + repository + DTOs + sync pipeline + tests | PR 3 | Depends on PR 2; ~450 lines |

### Conflict Resolution: R17/R22

The design flags a conflict: R17 says `upsertGastoOperativo()` should schedule `finanzasSync`, but R22 says "no dedicated upload method exists" for gastos_operativos. **This is resolved in favor of upload.** `gastos_operativos` is user-CRUD from Android — it needs upload sync just like dispensaciones, pagos, etc. Tasks below include `uploadGastosOperativos()` in `UploadSyncCoordinator` and the corresponding `GastoOperativoRemota` DTO.

### Seed Data Resolution

The 9 `categorias_producto` seed rows are duplicated between Supabase SQL (raw INSERT) and Room Kotlin (migration INSERTs). A shared `CategoriaProductoSeed` constant object is created so the Room migration programmatically iterates over it. A test verifies Supabase SQL seed matches the constant.

---

## Phase 1: Supabase Migration

- [x] **1.1 Create and apply `20260705000000_fase6_esquema_analisis.sql`**
  **Phase**: 1
  **Dependencies**: none
  **Files**:
    - `supabase/migrations/20260705000000_fase6_esquema_analisis.sql` (Create)
  **Tests**: none (applied via `supabase db push` and verified manually)
  **Description**: Single migration file with all 8 CREATE TABLEs in dependency order, ALTER `ventas` ADD COLUMN, RLS policies (4 tables + 2 global-read policies), idempotent seed INSERTs for `categorias_producto` (9 rows), and `recalcular_resumen_diario()` RPC function.
  **DDL order**: `categorias_producto` → ALTER `ventas` → `costos_productos` → `configuracion_financiera` → `gastos_operativos` → `margen_por_categoria` → `resumen_diario` → `feedback_recomendaciones` → RPC function
  **Acceptance**: Migration applies cleanly via `supabase db push` on a clean branch. All 8 tables + 1 ALTER + RLS + 9 seed rows + RPC function exist. Rolling back via `supabase migration repair --status down` + DROP statements restores prior state.

---

## Phase 2: Room Entities and DAOs

- [x] **2.1 Create `CategoriaProductoSeed` shared constant**
  **Phase**: 2
  **Dependencies**: none
  **Files**:
    - `data/categoriaproducto/CategoriaProductoSeed.kt` (Create)
  **Tests**: Verify 9 entries match expected IDs, nombres, familias, órdenes
  **Description**: Shared constant object holding the 9 `categorias_producto` seed rows as structured data (list of `CategoriaProductoSeed` data objects or similar). Used programmatically by Room migration to avoid hardcoding INSERT strings. The Supabase SQL migration embeds the same data as literal SQL values; a test verifies they stay in sync.
  **Acceptance**: `CategoriaProductoSeed.ALL` returns a `List` of 9 entries with correct id, nombre, familia, orden. A test asserts all values match.

- [x] **2.2 [TDD] `CategoriaProductoEntity` + `CategoriaProductoDao`**
  **Phase**: 2
  **Dependencies**: 2.1
  **Files**:
    - `data/categoriaproducto/CategoriaProductoEntity.kt` (Create)
    - `data/categoriaproducto/CategoriaProductoDao.kt` (Create)
  **Tests**: `CategoriaProductoDaoTest` — Room in-memory DB with seed data, verify `getAll()` returns 9 rows ordered by `orden` ASC, `getById('lente_progresivo')` returns correct entity, `getById('non_existent')` returns null
  **Description**: Room entity for `categorias_producto` (id, nombre, familia, orden). DAO with read-only methods: `getAll()` (suspend, ordered by orden), `getById()`. No upsert/insert/delete — this is a fixed seed table, read-only from app code.
  **Acceptance**: Both compile. Tests pass. `getAll()` returns 9 rows in correct order.

- [x] **2.3 [TDD] `GastoOperativoEntity` + `GastoOperativoDao`**
  **Phase**: 2
  **Dependencies**: none
  **Files**:
    - `data/gastooperativo/GastoOperativoEntity.kt` (Create)
    - `data/gastooperativo/GastoOperativoDao.kt` (Create)
  **Tests**: `GastoOperativoDaoTest` — in-memory Room DB, verify `upsert` inserts new row, `upsert` updates existing row (same ID), `getByOptica` emits `Flow` with correct expenses, `getByOpticaAndDateRange` filters by date range, `deleteById` removes row
  **Description**: Room entity for `gastos_operativos` (id, opticaId, categoria, descripcion, monto, fecha, fechaProgramada?, nota?, createdAt?). DAO with full CRUD: `getByOptica` (Flow), `getByOpticaAndDateRange` (Flow), `getById`, `upsert` (@Upsert), `deleteById`.
  **Acceptance**: All CRUD operations work. Flow emits reactive updates. Tests pass.

- [x] **2.4 [TDD] `ResumenDiarioEntity` + `ResumenDiarioDao`** (RED→GREEN: OptoDatabase getter registered, tests pass)
  **Phase**: 2
  **Dependencies**: none
  **Files**:
    - `data/resumendiario/ResumenDiarioEntity.kt` (Created — `fecha: String`, 14 fields)
    - `data/resumendiario/ResumenDiarioDao.kt` (Created — `getByOpticaId` Flow, `upsert`, `deleteAll`)
  **Tests**: `ResumenDiarioDaoTest` — 2 tests written (upsertAndGetByOpticaId, deleteAll_clearsData). Fails to compile: `Unresolved reference 'resumenDiarioDao'` in OptoDatabase.
  **Description**: Room entity matching `resumen_diario` columns. DAO with download-only methods: `getByOpticaId` (Flow, ORDER BY fecha DESC), `upsert` (@Upsert), `deleteAll`. No user-initiated write methods.
  **Acceptance**: All download-side queries work. No write methods exposed beyond `upsert` and `deleteAll`. Tests pass.

- [x] **2.5 [TDD] `ConfiguracionFinancieraEntity` + `ConfiguracionFinancieraDao` + update `Venta`** (RED→GREEN: OptoDatabase getter registered, tests pass)
  **Phase**: 2
  **Dependencies**: none
  **Files**:
    - `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt` (Create)
    - `data/configuracionfinanciera/ConfiguracionFinancieraDao.kt` (Create)
    - `data/venta/Venta.kt` (Modify — add `categoriaProductoId: String? = null`)
  **Tests**: `ConfiguracionFinancieraDaoTest` — verify `upsert` + `getByOptica` returns single row, getByOptica returns null when no row exists. `VentaDaoTest` — insert Venta with `categoriaProductoId` set, verify persistence; insert without it, verify null default
  **Description**: Room entity for `configuracion_financiera` (opticaId PK, 9 nullable config fields). DAO with download-only: `getByOptica` (suspend, nullable), `upsert` (@Upsert). Update `Venta.kt` adding `val categoriaProductoId: String? = null`.
  **Acceptance**: Config entity stores/fetches correctly. Venta compiles with new field, existing usages unaffected. Tests pass.

---

## Phase 3: Room Migration v31→v32

- [x] **3.1 `MIGRATION_31_32` class**
  **Phase**: 3
  **Dependencies**: 2.1, 2.5 (Venta entity updated)
  **Files**:
    - `data/OptoDatabaseMigrations.kt` (Modify — add `val MIGRATION_31_32`)
    - `data/OptoDatabase.kt` (Modify — version=32, add 4 entities, 4 abstract DAOs, register migration, add re-export)
  **Tests**: (see task 3.2)
  **Description**: Manual `Migration(31, 32)` that runs: CREATE TABLE IF NOT EXISTS for `categorias_producto` (with seed data from `CategoriaProductoSeed.ALL`), `gastos_operativos`, `resumen_diario`, `configuracion_financiera`; ALTER TABLE `ventas` ADD COLUMN `categoriaProductoId` TEXT DEFAULT NULL; CREATE INDEX statements. Update `OptoDatabase`: bump version to 32, add 4 entity classes to `entities` array, add abstract DAO methods, register `MIGRATION_31_32` in `.addMigrations()`, add companion re-export.
  **Acceptance**: Room database at version 31 with existing data survives migration to v32. All 4 new tables exist with correct columns and indexes. `ventas` has `categoriaProductoId` column. `categorias_producto` has 9 seed rows.

- [x] **3.2 [TDD] Migration test** (test created in RED phase, now GREEN — MIGRATION_31_32 exists and test passes)
  **Phase**: 3
  **Dependencies**: 3.1
  **Files**:
    - `optoapp/src/test/java/com/example/optoapp/data/OptoDatabaseMigrationTest.kt` (Create or extend)
  **Tests**: Build Room database at v31 with sample data (pacientes, dispensaciones, ventas, etc.), run `MIGRATION_31_32`, assert all 4 new tables exist with correct column names/types, assert `ventas.categoriaProductoId` exists and is nullable, assert 9 seed rows in `categorias_producto`, assert all pre-existing v31 data is intact
  **Description**: Migration test following existing `MIGRATION_29_30` test pattern. Verifies schema correctness and data preservation.
  **Acceptance**: Migration test passes. Existing data survives. Seed rows correct.

---

## Phase 4: DI and Repository Wiring

- [x] **4.1 `DatabaseModule` — new DAO providers**
  **Phase**: 4
  **Dependencies**: 3.1 (database has new DAOs)
  **Files**:
    - `di/DatabaseModule.kt` (Modify — add 4 `@Provides`, extend `provideOptoRepository` constructor)
  **Tests**: Compilation check — Hilt component graph builds without errors. All 4 new DAOs injectable.
  **Description**: Add `@Provides` methods for `CategoriaProductoDao`, `GastoOperativoDao`, `ResumenDiarioDao`, `ConfiguracionFinancieraDao`. Update `provideOptoRepository` to accept the new DAOs as constructor parameters and pass them to `OptoRepository`.
  **Acceptance**: App compiles and boots. Hilt resolves all new DAO providers.

- [x] **4.2 `OptoRepository` — passthrough methods**
  **Phase**: 4
  **Dependencies**: 4.1
  **Files**:
    - `data/OptoRepository.kt` (Modify — add DAO fields and passthrough methods)
  **Tests**: Verify `upsertGastoOperativo(entity)` calls `gastoOperativoDao.upsert()` + `postSaveSyncScheduler.scheduleFinanzasSync()`. Verify `upsertGastoOperativoFromRemote(entity)` calls `gastoOperativoDao.upsert()` but does NOT schedule sync. Verify `upsertResumenDiarioFromRemote(entity)` and `upsertConfiguracionFinancieraFromRemote(entity)` call respective DAOs without sync trigger. Verify `getCategoriasProducto()` returns seed list.
  **Description**: Add new DAO fields to `OptoRepository` constructor. Add passthrough methods: `upsertGastoOperativo` (local-write path — stamps timestamp + triggers sync), `upsertGastoOperativoFromRemote` (remote path — no sync), `upsertResumenDiarioFromRemote`, `upsertConfiguracionFinancieraFromRemote`, `getCategoriasProducto`.
  **Acceptance**: Passthrough tests pass. Local-write path schedules sync. Remote path skips sync.

- [x] **4.3 `SyncFinanzasDto` — new DTOs and `VentaRemota` update**
  **Phase**: 4
  **Dependencies**: 2.5 (Venta updated)
  **Files**:
    - `domain/SyncFinanzasDto.kt` (Modify)
  **Tests**: `FinanzasSyncResult` default counters match spec. `VentaRemota` decodes `categoria_producto_id` → `categoriaProductoId`. `toEntity()` passes the field through. `ResumenDiarioRemota.toEntity()` maps snake_case → camelCase correctly. `ConfiguracionFinancieraRemota.toEntity()` maps correctly.
  **Description**: Add `ResumenDiarioRemota` (@Serializable DTO with `@SerialName` mappings + `toEntity()`). Add `ConfiguracionFinancieraRemota` (same pattern). Add `GastoOperativoRemota` (@Serializable DTO with `toEntity()` + `toRemoto()` for upload path). Update `VentaRemota` with `@SerialName("categoria_producto_id") val categoriaProductoId: String? = null`. Add `downloadedResumenesDiarios: Int = 0` and `downloadedConfiguracionesFinancieras: Int = 0` to `FinanzasSyncResult`. Add `uploadedGastosOperativos: Int = 0` to `FinanzasSyncResult` for the new upload method.
  **Acceptance**: All DTOs compile and serialize/deserialize correctly. `FinanzasSyncResult` has 3 new counters with safe defaults.

---

## Phase 5: Sync Pipeline

- [x] **5.1 `DownloadSyncCoordinator` — new download methods**
  **Phase**: 5
  **Dependencies**: 4.2 (repository passthroughs), 4.3 (DTOs)
  **Files**:
    - `domain/DownloadSyncCoordinator.kt` (Modify — add `downloadResumenDiario()`, `downloadConfiguracionFinanciera()`, table constants)
  **Tests**: `DownloadSyncCoordinatorTest` — mock `SupabaseClient` and `OptoRepository`. Verify `downloadResumenDiario(opticaId)` queries `resumen_diario` table, decodes as `List<ResumenDiarioRemota>`, calls `repository.upsertResumenDiarioFromRemote()` per row, returns count. Verify `downloadConfiguracionFinanciera(opticaId)` queries `configuracion_financiera` with `maybeSingle()`, calls `repository.upsertConfiguracionFinancieraFromRemote()` if found, returns 1 or 0.
  **Description**: Add `TABLE_RESUMEN_DIARIO` and `TABLE_CONFIG_FINANCIERA` constants. Implement `downloadResumenDiario()` following `downloadVentas()` pattern (select all for optica, decode, persist via repository). Implement `downloadConfiguracionFinanciera()` following `maybeSingle()` pattern (0 or 1 row).
  **Acceptance**: Both download methods compile and work with mocked Supabase. Counts returned correctly.

- [x] **5.2 `SyncFinanzasUseCase` — integrate downloads and uploads**
  **Phase**: 5
  **Dependencies**: 5.1, 5.3 (upload method exists)
  **Files**:
    - `domain/SyncFinanzasUseCase.kt` (Modify — wire new downloads and upload)
  **Tests**: `SyncFinanzasUseCaseTest` — mock `DownloadSyncCoordinator` + `UploadSyncCoordinator`. Verify download sequence: arqueos → dispensaciones → items → servicios → ventas → **resumen_diario** → **configuracion_financiera** → pagos. Verify upload sequence includes `uploadGastosOperativos()` after uploadPagos. Verify `FinanzasSyncResult` includes 3 new counters.
  **Description**: Add `downloadResumenDiario()`, `downloadConfiguracionFinanciera()` calls to `if (downloadAfterUpload)` block after ventas download, before pagos download. Add `uploadGastosOperativos()` call to upload block (after uploadPagos). Pass counters to `FinanzasSyncResult` construction.
  **Acceptance**: New downloads called in correct order. New upload called. Counters populated in result. Existing flow unchanged when skipUpload=true.

- [x] **5.3 `UploadSyncCoordinator` — `uploadGastosOperativos()`** (resolves R17/R22)
  **Phase**: 5
  **Dependencies**: 4.3 (GastoOperativoRemota DTO)
  **Files**:
    - `domain/UploadSyncCoordinator.kt` (Modify — add `TABLE_GASTOS_OPERATIVOS` constant, `uploadGastosOperativos()` method)
  **Tests**: `UploadSyncCoordinatorTest` — mock `SupabaseClient` + `OptoRepository`. Seed local `GastoOperativoEntity` rows. Verify `uploadGastosOperativos()` fetches snapshot via `repository`, maps to `GastoOperativoRemota` via `toRemoto()`, upserts to Supabase in chunks, marks sync state, returns count. Verify empty local data returns 0 without network call.
  **Description**: Add `TABLE_GASTOS_OPERATIVOS = "gastos_operativos"` constant. Implement `uploadGastosOperativos(opticaId): Int` following `uploadPagos()` pattern: fetch local snapshot via repository (need `getGastosOperativosSnapshotForOptica()` added to repository), map to `GastoOperativoRemota`, chunked upsert to Supabase, mark sync state. This resolves the R17/R22 conflict — `gastos_operativos` now has full upload sync.
  **Pre-requisite**: Add `suspend fun getGastosOperativosSnapshotForOptica(opticaId: String): List<GastoOperativoEntity>` to `OptoRepository` (delegates to `gastoOperativoDao.getByOptica(opticaId).first()` or a dedicated snapshot method).
  **Acceptance**: Upload method compiles and works. Local gastos_operativos propagate to Supabase. Empty state handled gracefully.

---

## Phase 6: Verification

- [x] **6.1 Full DAO test suite**
  **Phase**: 6
  **Dependencies**: 2.2–2.5 (all DAOs exist)
  **Tests**: Run full DAO test suite in batch: `CategoriaProductoDaoTest` → `GastoOperativoDaoTest` → `ResumenDiarioDaoTest` → `ConfiguracionFinancieraDaoTest` → `VentaDaoTest` (categoriaProductoId)
  **Description**: Execute Room in-memory DB tests for all 4 new DAOs plus updated VentaDao. Verify seed queries, CRUD operations, reactive Flow emissions, download-only constraints, and null safety.
  **Acceptance**: All DAO tests pass.

- [x] **6.2 Full verification build**
  **Phase**: 6
  **Dependencies**: 5.2 (sync pipeline complete)
  **Files**: CI config (no changes needed)
  **Tests**: `./gradlew :optoapp:testDebugUnitTest --stacktrace` + `./gradlew :optoapp:assembleDebug`
  **Description**: Run the full test suite to confirm no regressions. Build debug APK to confirm compilation. Verify all new code integrates with existing modules without breaking existing tests. Run seed consistency check: Supabase migration INSERT statements match `CategoriaProductoSeed.ALL`.
  **Acceptance**: All tests pass (both new and existing). `assembleDebug` succeeds. Seed data consistent between Supabase SQL and Room constant.
