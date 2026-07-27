# Tasks: Sync Critical Fixes

Priority order: **S2 (A) → C2 (B) → H6 (C) → all-chunk (D) → Integration (E)**.

Each task is independently verifiable. Tests must pass before the next task begins (strict TDD).

---

## Block A — EvaluacionEntity Nullable Alignment (REQ-SYNC-001)

### [x] A1 — Audit EvaluacionEntity nullable columns

- **Files**: `data/evaluacion/EvaluacionEntity.kt`
- **What**: Identify every column in `EvaluacionClinica` that Room declares as non-null (`String = ""`, `Boolean = false`, `List<String> = emptyList()`, `Int = 0`) but Supabase schema allows NULL. Cross-reference against `EvaluacionRemota` in `domain/SyncHistorialDto.kt` where the DTO already declares the same field as nullable (`String?`).
- **Acceptance**: Documented list of exactly which columns to change (estimate ~80 `String` → `String?`, ~7 `Boolean` → `Boolean?`, ~4 `List<String>` → `List<String>?`, plus `Int?`/`Double?` already correct).
- **Test**: None (audit only).

---

### [x] A2 — Make EvaluacionEntity columns nullable

- **Files**: `data/evaluacion/EvaluacionEntity.kt`
- **What**: Change field types:
  - `String = ""` → `String? = null` (all ~80 text fields)
  - `Boolean = false` / `Boolean = true` → `Boolean? = null` (all boolean flags: `balanceOd`, `balanceOi`, `otrosPresbicia`, `otrosAnisometropia`, `otrosAmbliopia`, `autoPresbicia`, `autoAnisometropia`, `autoAmbliopia`)
  - `List<String> = emptyList()` → `List<String>? = null` (`necesidadVisual`, `diagnosticoOd`, `diagnosticoOi`, `diagnosticoOtros`)
  - Do NOT change: `id`, `pacienteId`, `fecha`, `opticaId` (always required), nor `osdiPuntuacion`, `dipTotalMm`, `dnpOdMm`, `dnpOiMm`, `updatedAt`, `updatedBy`, `proximaCita`, `lcFechaAdaptacion` (already nullable).
- **Acceptance**: Entity compiles. Room schema version unchanged (nullability change is backward-compatible at SQLite level).
- **Test**: Compile check only (tests in A4 verify runtime behavior).

---

### [x] A3 — Update all EvaluacionClinica consumers

- **Files**:
  - `domain/SyncHistorialDto.kt` — `EvaluacionRemota.toEntity()`: remove `.orEmpty()` from nullable fields (already handled caller-side), pass nullable values directly.
  - `domain/SyncHistorialDto.kt` — `EvaluacionClinica.toRemoto()`: handle nullable `List<String>?` with `?.joinToString(",") ?: ""`, nullable `Boolean?` → pass through.
  - `viewmodel/EvaluacionMapping.kt` — `EvaluacionUiState.toEvaluacionClinica()` and `EvaluacionClinica.toEvaluacionUiState()`: add `?: ""` / `?.` / `.orEmpty()` fallbacks.
  - `util/RecetaRefraccionTable.kt` — `prepareData()` and helper functions: add `?.` safe-calls on eval fields.
  - `util/RecetaPdfBuilder.kt` — `addRefraccion()`, `addDiagnostico()`, `addPrismas()`, `addSeguimiento()`: add `?: ""` fallbacks.
  - `viewmodel/EvaluacionViewModel.kt` — any direct field access: add safe-calls.
  - `viewmodel/DispensacionViewModel.kt` — any direct field access: add safe-calls.
  - `viewmodel/PacienteViewModel.kt` — any direct field access: add safe-calls.
  - `viewmodel/AgendaViewModel.kt` — any direct field access: add safe-calls.
  - `data/PacienteRepository.kt` — any field comparison with `switchMap` or similar.
- **Acceptance**: All consumers compile without change to their external API contracts. Nullable fields render as `""` in UI and PDFs.
- **Test**: 
  - Verify `EvaluacionRemota("id","pid","2024-01-01","oid").toEntity()` produces an entity with all fields as non-null (same as before — no regression).
  - Verify `EvaluacionClinica(id="x",pacienteId="p",fecha=LocalDate.now()).toRemoto()` serializes nullable fields as null.
  - Compilation of all 50+ `EvaluacionClinica(...)` constructor calls in tests must succeed with the new nullable fields.

---

### [x] A4 — Unit test: nullable columns don't crash on NULL from DB

- **Files**: `src/test/java/com/example/optoapp/data/EvaluacionEntityNullableTest.kt` (new)
- **What**: Create Room in-memory DB. Insert an `EvaluacionClinica` where all optional fields are null (provided via default constructor values). Read back via `evaluacionDao.getEvaluacionById()`. Verify no exception — all nullable fields read as null.
- **Acceptance**:
  ```kotlin
  val ev = EvaluacionClinica(id = "e1", pacienteId = "p1", fecha = LocalDate.now())
  dao.insertEvaluacion(ev)
  val loaded = dao.getEvaluacionById("e1")
  assertNotNull(loaded)
  assertNull(loaded?.motivoConsulta)
  assertNull(loaded?.balanceOd)
  assertNull(loaded?.necesidadVisual)
  ```
- **Test**: `./gradlew :optoapp:testDebugUnitTest --tests "*EvaluacionEntityNullableTest*"` passes.

---

## Block B — Download Path Idempotency (REQ-SYNC-002)

### [x] B1 — Audit DAO @Insert methods in download paths

- **Files**: All `*Dao.kt` files referenced by download use cases (`DownloadSyncCoordinator`, `SyncPacientesUseCase.download`, `SyncHistorialUseCase.downloadEvaluaciones`, `SyncInventarioUseCase.download*`, `SyncProveedoresUseCase.download*`, `SyncOrdenesCompraUseCase.download*`, `SyncInventarioFisicoUseCase.download*`)
- **What**: Identify every `@Insert` (without `onConflict = REPLACE`) that runs during download. List must match the 8 methods in the design.
- **Acceptance**: Documented list of DAO + method to change.
- **Test**: None (audit only).

---

### [x] B2 — Add OnConflictStrategy.REPLACE to download-path DAOs

- **Files**:
  - `data/ordencompra/OrdenCompraDao.kt` — line 24: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  - `data/ordencompra/OrdenCompraItemDao.kt` — line 20: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  - `data/gastooperativo/GastoOperativoDao.kt` — line 22: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  - `data/regalodispensacion/RegaloDispensacionDao.kt` — line 10: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)` (note: `upsert` method already has REPLACE)
  - `data/inventariofisico/InventarioFisicoDao.kt` — line 28: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)` for `insertSession`
  - `data/inventariofisico/InventarioFisicoDao.kt` — line 31: `@Insert` → `@Insert(onConflict = OnConflictStrategy.REPLACE)` for `insertDetalles`
  - `data/proveedor/ProveedorDao.kt` — line 21: `@Insert(onConflict = OnConflictStrategy.ABORT)` → `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  - `data/proveedor/CategoriaMonturaDao.kt` — line 18: `@Insert(onConflict = OnConflictStrategy.ABORT)` → `@Insert(onConflict = OnConflictStrategy.REPLACE)`
- **Acceptance**: Compiles. Adding `import androidx.room.OnConflictStrategy` where missing.
- **Test**: Compile check (B3 tests runtime).

---

### [x] B3 — Unit test: duplicate PK insert doesn't crash

- **Files**: `src/test/java/com/example/optoapp/data/DaoIdempotencyTest.kt` (new)
- **What**: Room in-memory DB. For one representative DAO (e.g., `GastoOperativoDao`): insert entity with PK=X, then insert same PK=X again. Verify no exception, second insert replaces.
- **Acceptance**: `assertDoesNotThrow` on second insert. Row count remains 1, last value wins.
- **Test**: `./gradlew :optoapp:testDebugUnitTest --tests "*DaoIdempotencyTest*"` passes.

---

## Block C — Transactional markSynced on Upload (REQ-SYNC-003)

### [x] C1 — Fix UploadSyncCoordinator.executeSimpleUpsert

- **File**: `domain/UploadSyncCoordinator.kt`
- **What**: In `executeSimpleUpsert` (lines 81-83), wrap per-entity `markSynced` inside `database.withTransaction`. The batch-level `markSynced` (line 84) stays outside.
  - Add `database: OptoDatabase` as constructor parameter.
  - Change:
    ```kotlin
    rows.forEach { r ->
        syncStateTracker.markSynced(opticaId, entityType, idSelector(r))
    }
    ```
    to:
    ```kotlin
    database.withTransaction {
        rows.forEach { r ->
            syncStateTracker.markSynced(opticaId, entityType, idSelector(r))
        }
    }
    ```
  - Same pattern for `uploadDispensaciones` (lines 192-194) and `uploadServicios` (lines 287-289).
- **Acceptance**: Per-entity markSynced commits atomically. Batch-level markSynced is NOT inside the per-entity transaction.
- **Test**: C9 verifies.

---

### [x] C2 — Fix SyncPacientesUseCase upload

- **File**: `domain/SyncPacientesUseCase.kt`
- **What**: In `upload()` (lines 180-183), wrap per-entity `markSynced` in `database.withTransaction`. Add `database: OptoDatabase` as constructor parameter.
  ```kotlin
  // BEFORE:
  syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
  finalRows.forEach { p ->
      syncStateTracker.markSynced(opticaId, "paciente", p.id)
  }

  // AFTER:
  database.withTransaction {
      finalRows.forEach { p ->
          syncStateTracker.markSynced(opticaId, "paciente", p.id)
      }
  }
  syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
  ```
- **Acceptance**: Per-paciente markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C3 — Fix SyncHistorialUseCase upload

- **File**: `domain/SyncHistorialUseCase.kt`
- **What**: In `uploadEvaluaciones()` (lines 160-163), wrap per-entity `markSynced` in `database.withTransaction`. Same pattern as C2. Add `database: OptoDatabase`.
- **Acceptance**: Per-evaluacion markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C4 — Fix SyncInventarioUseCase upload

- **File**: `domain/SyncInventarioUseCase.kt`
- **What**: In `uploadMonturas()` (lines 106-107) and `uploadMovimientos()` (lines 149-150), wrap per-entity `markSynced` in `database.withTransaction`. Add `database: OptoDatabase`.
- **Acceptance**: Per-montura and per-movimiento markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C5 — Fix SyncProveedoresUseCase upload

- **File**: `domain/SyncProveedoresUseCase.kt`
- **What**: In `uploadProveedores()` (line 91) and `uploadCategorias()` (line 104), wrap per-entity `markSynced` in `database.withTransaction`. Add `database: OptoDatabase`.
- **Acceptance**: Per-proveedor and per-categoria markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C6 — Fix SyncOrdenesCompraUseCase upload

- **File**: `domain/SyncOrdenesCompraUseCase.kt`
- **What**: In `uploadOrdenesCompra()` (line 94) and `uploadItems()` (line 108), wrap per-entity `markSynced` in `database.withTransaction`. Add `database: OptoDatabase`.
- **Acceptance**: Per-orden and per-item markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C7 — Fix SyncInventarioFisicoUseCase upload

- **File**: `domain/SyncInventarioFisicoUseCase.kt`
- **What**: In `uploadSessions()` (line 88) and `uploadDetalles()` (line 109), wrap per-entity `markSynced` in `database.withTransaction`. Add `database: OptoDatabase`.
- **Acceptance**: Per-session and per-detalle markSynced inside transaction; batch markSynced outside.
- **Test**: C9 verifies.

---

### [x] C8 — Fix SyncFinanzasMerge markSynced

- **File**: `domain/SyncFinanzasMerge.kt`
- **What**: In `mergeLocalDispensacionConflict()` (line 56), move `syncStateTracker.markSynced(opticaId, "dispensacion", duplicate.id)` inside the preceding `repository.withTransaction {}` block (lines 49-55). The `markError` call (lines 57-64) stays outside.
- **Acceptance**: markSynced for the duplicate dispensacion is atomic with the merge transaction.
- **Test**: C9 verifies.

---

### [x] C9 — Unit test: markSynced transactional integrity

- **Files**: `src/test/java/com/example/optoapp/domain/UploadSyncCoordinatorTest.kt` (extend existing), or new `src/test/java/com/example/optoapp/domain/SyncTransactionalTest.kt`
- **What**: For one representative upload coordinator (e.g., `UploadSyncCoordinator`): mock `SyncStateTracker` to throw on `markSynced`. Verify that the transaction rollback prevents batch-level markSynced from running. Then verify success path: both entity write and markSynced commit.
- **Acceptance**: 
  - On `markSynced` failure: exception propagates, batch-level markSynced never called.
  - On success: per-entity markSynced called, batch-level markSynced called after transaction.
- **Test**: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncTransactionalTest*"` passes.

---

## Block D — Graceful All-Chunk Failure (REQ-SYNC-004)

### [x] D1 — Remove RuntimeException in fetchRemoteUpdatedAt

- **File**: `domain/sync/ConflictHelper.kt`
- **What**: In `fetchRemoteUpdatedAt` (lines 211-216), replace:
  ```kotlin
  if (ids.isNotEmpty() && rows.isEmpty()) {
      throw RuntimeException("All chunk queries failed for $tableName")
  }
  ```
  with:
  ```kotlin
  if (ids.isNotEmpty() && rows.isEmpty()) {
      AppLogger.w(TAG, "All chunk queries failed for $tableName — returning empty map")
  }
  ```
- **Acceptance**: Method returns `emptyMap()` instead of throwing when all chunks fail. `selectRemoteRows` already catches per-chunk exceptions gracefully.
- **Test**: D2 verifies.

---

### [x] D2 — Unit test: all-chunk failure returns empty map

- **Files**: `src/test/java/com/example/optoapp/domain/sync/ConflictHelperAllChunkTest.kt` (new)
- **What**: Create a test instance of `ConflictHelper` (or a subclass) that overrides `selectRemoteRowsChunk` to always throw. Call `fetchRemoteUpdatedAt` with non-empty IDs. Verify:
  1. No `RuntimeException` is thrown
  2. Return value is `emptyMap()`
  3. Error is logged (via `AppLogger`)
- **Acceptance**: 
  ```kotlin
  val result = helper.fetchRemoteUpdatedAt("test_table", "optica_1", listOf("id1", "id2"))
  assertTrue(result.isEmpty())
  ```
- **Test**: `./gradlew :optoapp:testDebugUnitTest --tests "*ConflictHelperAllChunkTest*"` passes.

---

## Block E — Integration & Verification

### E1 — Full unit test suite

- **Command**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- **What**: Run all unit tests (including new ones from A4, B3, C9, D2). All pass.
- **Acceptance**: BUILD SUCCESSFUL — 0 test failures, 0 compilation errors.

---

### E2 — JaCoCo coverage report

- **Command**: `./gradlew :optoapp:jacocoTestReport`
- **What**: Generate coverage report. Verify instruction coverage meets the 5% minimum threshold.
- **Acceptance**: Report generates successfully. Coverage threshold passes.

---

### E3 — Manual verification checklist

Verify these scenarios (no automated test):
1. **Nullable entity**: Deploy debug APK to device. Download evaluaciones where Supabase has rows with null optional columns. Verify download completes without crash. Open evaluacion detail screen — null fields display as empty string.
2. **Re-download idempotency**: Trigger sync twice without local changes. Verify no crash on second download.
3. **All-chunk failure**: With device in airplane mode, trigger sync. Verify sync module doesn't crash — entities appear as "safe to upload" (no remote data = no conflict).
4. **Upload transactional integrity**: Upload an entity after modifying it. Verify `sync_entity_state` shows entity as synced. No re-upload on next cycle.

---

## Summary

| Block | Requirement | Tasks | Files Changed | New Tests |
|-------|------------|-------|---------------|-----------|
| A | REQ-SYNC-001 | A1–A4 | ~12 source files | `EvaluacionEntityNullableTest.kt` |
| B | REQ-SYNC-002 | B1–B3 | 7 DAO files | `DaoIdempotencyTest.kt` |
| C | REQ-SYNC-003 | C1–C9 | 9 sync files | `SyncTransactionalTest.kt` |
| D | REQ-SYNC-004 | D1–D2 | `ConflictHelper.kt` | `ConflictHelperAllChunkTest.kt` |
| E | Verification | E1–E3 | — | — |

**Total**: ~29 source files modified, 4 new test files, ~12 UI/consumer files updated for nullable alignment.
