# Tasks: Complete Sync Conflict Protection

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 700–950 (across 3 PRs) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 (stacked-to-main) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Download guard + DI + filterConflictMovimientos fix | PR 1 | ~180 lines; 5 UseCase injections, 10 guards, 1 ConflictHelper fix |
| 2 | Bump coverage + upload filters + UI labels | PR 2 | ~250 lines; 6 bump branches, 3 child mappings, 2 repo fixes, 2 upload filters, TYPE_LABELS |
| 3 | Three-way merge + snapshot UI | PR 3 | ~400 lines; migration, ThreeWayMerge, serializer, snapshot capture, resolve rewrite, field-level UI |

---

## PR 1: Download Guard + DI

### Phase 1: ConflictDao Injection (5 UseCases)

- [x] 1.1 **[RED]** Create `SyncPacientesUseCaseDownloadGuardTest.kt`; write failing test: `download skips entity whose ID is in getConflictEntityIds("paciente")`
- [x] 1.2 **[GREEN]** Add `conflictDao: ConflictDao` param to `SyncPacientesUseCase @Inject constructor` in `domain/SyncPacientesUseCase.kt`
- [x] 1.3 **[GREEN]** In `download()`: fetch `conflictIds = conflictDao.getConflictEntityIds(opticaId, "paciente").toSet()`; skip remotos with `id in conflictIds` (fail-open: wrap in try/catch, log + proceed on error)
- [x] 1.4 **[RED]** Create `SyncHistorialUseCaseDownloadGuardTest.kt`; write failing test for `downloadEvaluaciones` guard
- [x] 1.5 **[GREEN]** Add `conflictDao: ConflictDao` to `SyncHistorialUseCase`; guard `downloadEvaluaciones()` with entityType `"evaluacion"`
- [x] 1.6 **[RED]** Create `SyncInventarioUseCaseDownloadGuardTest.kt`; write failing tests for `downloadMonturas` + `downloadMovimientos` guards
- [x] 1.7 **[GREEN]** Add `conflictDao: ConflictDao` to `SyncInventarioUseCase`; guard `downloadMonturas()` with `"montura"` and `downloadMovimientos()` with `"montura_movimiento"`
- [x] 1.8 **[RED]** Create `SyncProveedoresUseCaseDownloadGuardTest.kt`; write failing tests for `downloadProveedores` + `downloadCategorias`
- [x] 1.9 **[GREEN]** Add `conflictDao: ConflictDao` to `SyncProveedoresUseCase`; guard `downloadProveedores()` with `"proveedor"` and `downloadCategorias()` with `"categoria_montura"`
- [x] 1.10 **[RED]** Create `SyncOrdenesCompraUseCaseDownloadGuardTest.kt`; write failing tests for `downloadOrdenesCompra` + `downloadItems`
- [x] 1.11 **[GREEN]** Add `conflictDao: ConflictDao` to `SyncOrdenesCompraUseCase`; guard `downloadOrdenesCompra()` with `"orden_compra"` and `downloadItems()` with `"orden_compra_item"`
- [x] 1.12 **[RED]** Create `DownloadSyncCoordinatorNewGuardsTest.kt`; write failing tests for `downloadDispensacionItems` + `downloadArqueos` guards
- [x] 1.13 **[GREEN]** In `DownloadSyncCoordinator.downloadDispensacionItems()`: add guard with `"dispensacion_item"` (conflictDao already injected at line 24)
- [x] 1.14 **[GREEN]** In `DownloadSyncCoordinator.downloadArqueos()`: add guard with `"arqueo_caja"` — filter remoteArqueos before upsert loop

### Phase 2: filterConflictMovimientos Persistence

- [x] 2.1 **[RED]** Create `ConflictHelperMovimientoPersistenceTest.kt`; write failing tests: `filterConflictMovimientos calls conflictDao.upsertConflict for stock conflicts` and `does NOT upsert for non-conflicted movimientos`
- [x] 2.2 **[GREEN]** In `ConflictHelper.filterConflictMovimientos()`: after `detectConflictMovimientos`, loop `conflictedIds` and call `conflictDao.upsertConflict(id, opticaId, "montura_movimiento", "", "")` — mirroring `filterConflicts` pattern (lines 146-152)

### Phase 3: PR 1 Verification

- [x] 3.1 **[VERIFY]** Run `./gradlew :optoapp:testDebugUnitTest` — all tests pass
- [x] 3.2 **[VERIFY]** Run `./gradlew :optoapp:assembleDebug` — clean build, no constructor arity errors

---

## PR 2: Bump + Upload + UI

### Phase 4: bumpEntityUpdatedAt Extension (6 parent types)

- [x] 4.1 **[RED]** Create `SyncViewModelBumpCoverageTest.kt`; write failing tests for bump of `paciente`, `evaluacion`, `montura`, `proveedor`, `orden_compra`, `arqueo_caja`
- [x] 4.2 **[GREEN]** Add 6 `when` branches to `bumpEntityUpdatedAt()` in `SyncViewModel.kt`:
  - `"paciente"` → `repository.getPacienteById(id)` → `repository.updatePaciente(entity)`
  - `"evaluacion"` → `repository.getEvaluacionById(id)` → `repository.updateEvaluacion(entity)`
  - `"montura"` → `repository.getMonturaById(id)` → `repository.updateMontura(entity)` (via MonturaInventoryCoordinator)
  - `"proveedor"` → `proveedorRepository.getById(id)` → `proveedorRepository.update(entity.copy(updatedAt = Instant.now().toString()))`
  - `"orden_compra"` → `ordenCompraRepository.getById(id)` → `ordenCompraRepository.update(entity)` (already stamps)
  - `"arqueo_caja"` → `repository.getArqueoByFechaSync(...)` → `repository.updateArqueo(entity.copy(updatedAt = Instant.now()))`
- [x] 4.3 **[GREEN]** In `ProveedorRepository.update()`: wrap with `proveedor.copy(updatedAt = Instant.now().toString())` before `proveedorDao.update(stamped)` — add `import java.time.Instant`
- [x] 4.4 **[GREEN]** In `OptoRepository.updateArqueo()`: add `copy(updatedAt = Instant.now())` stamp before `arqueoCajaDao.updateArqueo(stamped)`

### Phase 5: Child→Parent Bump (3 mappings)

- [x] 5.1 **[RED]** Create `SyncViewModelChildBumpTest.kt`; write failing tests: `montura_movimiento bumps parent montura`, `orden_compra_item bumps parent orden_compra`, `dispensacion_item bumps parent dispensacion`, `categoria_montura logs warning and skips`
- [x] 5.2 **[GREEN]** Add 4 child branches to `bumpEntityUpdatedAt()`:
  - `"montura_movimiento"` → query movimiento → extract `monturaId` → `repository.getMonturaById(monturaId)` → `repository.updateMontura(montura)`
  - `"orden_compra_item"` → query item → extract `ordenId` → `ordenCompraRepository.getById(ordenId)` → `ordenCompraRepository.update(oc)`
  - `"dispensacion_item"` → query item → extract `dispensacionId` → `repository.getDispensacionById(dispensacionId)` → `repository.updateDispensacion(disp)`
  - `"categoria_montura"` → `Log.w(TAG, "categoria_montura has no parent, skipping bump")`

### Phase 6: Upload Conflict Filters

- [x] 6.1 **[RED]** Create `UploadSyncCoordinatorConflictFilterTest.kt`; write failing tests: `uploadDispensacionItems filters via filterConflicts` and `uploadArqueos filters via filterConflicts`
- [x] 6.2 **[GREEN]** In `UploadSyncCoordinator.uploadDispensacionItems()`: add `conflictHelper.filterConflicts(TABLE_DISPENSACION_ITEMS, opticaId, "dispensacion_item", ...)` before upsert — inject `ConflictHelper` if not already present
- [x] 6.3 **[GREEN]** In `UploadSyncCoordinator.uploadArqueos()`: add `conflictHelper.filterConflicts(TABLE_ARQUEO_CAJA, opticaId, "arqueo_caja", ...)` before upsert loop

### Phase 7: TYPE_LABELS Extension

- [x] 7.1 Extend `TYPE_LABELS` map in `ConflictosScreen.kt` with 9 new entries: `montura_movimiento`, `proveedor`, `categoria_montura`, `orden_compra`, `orden_compra_item`, `inventario_fisico`, `inventario_fisico_detalle`, `dispensacion_item`, `arqueo_caja`

### Phase 8: PR 2 Verification

- [x] 8.1 **[VERIFY]** Run `./gradlew :optoapp:testDebugUnitTest` — all tests pass
- [x] 8.2 **[VERIFY]** Run `./gradlew :optoapp:assembleDebug` — clean build

---

## PR 3: Three-Way Merge

### Phase 9: Room Migration v27→v28

- [x] 9.1 **[RED]** Create `Migration27To28Test.kt`; write failing test: create v27 DB with 5 conflict_records, run migration, verify 3 new columns exist with DEFAULT `'{}'`, existing data preserved
- [x] 9.2 **[GREEN]** Add 3 fields to `ConflictRecord` entity in `data/sync/ConflictRecord.kt`: `baseSnapshot: String = "{}"`, `localData: String = "{}"`, `remoteData: String = "{}"`
- [x] 9.3 **[GREEN]** Update `upsertConflict` SQL in `ConflictDao` to include 3 new columns with defaults
- [x] 9.4 **[GREEN]** Add `getConflictSnapshot` query to `ConflictDao`: `SELECT baseSnapshot, localData, remoteData FROM conflict_records WHERE entityId = :entityId AND opticaId = :opticaId`
- [x] 9.5 **[GREEN]** Create `MIGRATION_27_28` in `data/OptoDatabaseMigrations.kt`: three `ALTER TABLE conflict_records ADD COLUMN` with `TEXT NOT NULL DEFAULT '{}'`
- [x] 9.6 **[GREEN]** Bump `version = 28` in `OptoDatabase.kt`, register `MIGRATION_27_28` in `addMigrations()`

### Phase 10: ThreeWayMerge Pure Class

- [x] 10.1 **[RED]** Create `ThreeWayMergeTest.kt`; write 8 failing tests: all-unchanged, local-only change, remote-only change, non-overlapping auto-merge, overlapping conflict, empty base, missing fields, no-changes
- [x] 10.2 **[GREEN]** Create `domain/sync/ThreeWayMerge.kt`: pure class with `fun merge(input: MergeInput): MergeResult<JsonObject>` — field-by-field comparison per FR-09 rules, returning `mergedEntity`, `conflictedFields`, `autoMergedFields`, `hasConflict`

### Phase 11: Snapshot Capture in ConflictHelper

- [x] 11.1 **[RED]** Create `ConflictHelperSnapshotTest.kt`; write failing tests: `filterConflicts captures localData + remoteData JSON on conflict`, `baseSnapshot defaults to "{}" when unavailable`, `remote fetch failure → remoteData = "{}"`
- [x] 11.2 **[GREEN]** Create `domain/sync/EntitySnapshotSerializer.kt`: helpers `fun serializeEntity(entity: Any): String` using `kotlinx.serialization.json.Json`, `fun parseSnapshot(json: String): JsonObject`
- [x] 11.3 **[GREEN]** In `ConflictHelper.filterConflicts()`: on conflict detection (line 144-153), fetch full remote row via `selectRemoteRows`, serialize to `remoteData`; serialize local entity to `localData`; pass both + `baseSnapshot = "{}"` to `upsertConflict`

### Phase 12: Resolution Rewrite

- [x] 12.1 **[RED]** Create `SyncViewModelThreeWayMergeTest.kt`; write failing tests: `resolveKeepMine with snapshots → three-way merge → upload merged → clear conflict`, `resolveKeepMine without snapshots → fallback to bump`, `resolveAcceptTheirs with snapshots → merge remote wins → write Room → clear conflict`
- [x] 12.2 **[GREEN]** Rewrite `resolveKeepMine` in `SyncViewModel.kt`: if `conflict.baseSnapshot != "{}"`, call `ThreeWayMerge.merge()`, apply local-wins for conflicted fields, upload merged entity, clear conflict on success. Fallback to existing bump if `baseSnapshot == "{}"`
- [x] 12.3 **[GREEN]** Rewrite `resolveAcceptTheirs` in `SyncViewModel.kt`: if snapshots present, call `ThreeWayMerge.merge()`, apply remote-wins for conflicted fields, write merged to Room (no upload), clear conflict. Fallback to existing behavior if empty snapshots

### Phase 13: Field-Level Conflict UI

- [x] 13.1 **[RED]** Create `ConflictosScreenSnapshotTest.kt`; write Compose test: render card with snapshot data, verify per-field diffs shown; render card without snapshots, verify timestamp display
- [x] 13.2 **[GREEN]** In `ConflictCard` composable: if `conflict.baseSnapshot != "{}"`, compute `ThreeWayMerge.merge()` in `LaunchedEffect`, show `conflictedFields` list with local vs remote values. Hide auto-merged fields. Fallback to timestamp display if `baseSnapshot == "{}"`

### Phase 14: PR 3 Verification

- [x] 14.1 **[VERIFY]** Run `./gradlew :optoapp:testDebugUnitTest` — all tests pass
- [x] 14.2 **[VERIFY]** Run `./gradlew :optoapp:assembleDebug` — clean build

---

## Spec Cross-Reference

| Task(s) | Spec Requirement |
|---------|-----------------|
| 1.1–1.14 | FR-01: Download Guard for All Entity Types |
| 1.2, 1.5, 1.7, 1.9, 1.11 | FR-02: ConflictDao Injection |
| 2.1–2.2 | FR-05b: filterConflictMovimientos persistence |
| 4.1–4.4 | FR-03: bumpEntityUpdatedAt Coverage |
| 5.1–5.2 | FR-04: Child Entity Parent Bump |
| 6.1–6.3 | FR-05: Upload Flow Enhancement |
| 7.1 | FR-06: UI Entity Type Labels |
| 9.1–9.6 | FR-07: Conflict Record Schema Migration |
| 10.1–10.2 | FR-09: Field-Level Three-Way Merge Logic |
| 11.1–11.3 | FR-08: Snapshot Capture at Conflict Detection |
| 12.1–12.2 | FR-10: "Usar el mío" with Three-Way Merge |
| 12.3 | FR-11: "Usar la nube" with Three-Way Merge |
| 13.1–13.2 | FR-12: Field-Level Conflict UI |
