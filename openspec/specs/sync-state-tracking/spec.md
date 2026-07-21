# Sync State Tracking Specification

## ADDED Requirements

### Requirement: deleteGastoOperativo SHALL markDeleted

The system MUST call `syncStateTracker.markDeleted(opticaId, "gasto_operativo", gasto.id)` after the local Room deletion succeeds in `deleteGastoOperativo`, matching the existing pattern in `deleteDispensacion`.

#### Scenario: Successful delete marks deletion state

- GIVEN a `GastoOperativoEntity` with valid `opticaId` and `id`
- WHEN `gastoOperativoDao.delete` succeeds
- THEN `syncStateTracker.markDeleted(opticaId, "gasto_operativo", gasto.id)` is called

#### Scenario: Room delete failure skips markDeleted

- GIVEN `gastoOperativoDao.delete` throws an exception
- WHEN `deleteGastoOperativo` runs
- THEN `syncStateTracker.markDeleted` MUST NOT be called

---

### Requirement: deleteVentaById SHALL markDeleted

The system MUST call `syncStateTracker.markDeleted(opticaId, "venta", id)` after the local Room deletion succeeds in `deleteVentaById`, matching the existing pattern in `deleteDispensacionItemById`.

#### Scenario: Successful delete marks deletion state

- GIVEN a venta with valid `id` and `origenId`
- WHEN `ventaDao.deleteById` and `ventaDao.deleteByOrigenId` succeed
- THEN `syncStateTracker.markDeleted(opticaId, "venta", id)` is called

#### Scenario: Room delete failure skips markDeleted

- GIVEN `ventaDao.deleteById` throws
- WHEN `deleteVentaById` runs
- THEN `syncStateTracker.markDeleted` MUST NOT be called

---

### Requirement: uploadVentas SHALL emit markSynced and markError

`uploadVentas` MUST call `syncStateTracker.markSynced` on success and `syncStateTracker.markError` on failure, matching the exact pattern in `uploadGastosOperativos`.

#### Scenario: Successful upload marks batch and per-entity synced

- GIVEN ventas exist for the optica
- WHEN `uploadVentas` uploads all chunks without error
- THEN `syncStateTracker.markSynced(opticaId, "upload_ventas", "batch")` is called
- AND `syncStateTracker.markSynced(opticaId, "venta", v.id)` is called for each uploaded venta

#### Scenario: IOException marks error before rethrow

- GIVEN supabase upsert throws IOException
- WHEN `uploadVentas` catches it
- THEN `syncStateTracker.markError(opticaId, "upload_ventas", "batch", e.message)` is called
- AND the IOException is rethrown

#### Scenario: Generic exception marks error before rethrow

- GIVEN supabase upsert throws a non-IOException, non-CancellationException
- WHEN `uploadVentas` catches it
- THEN `syncStateTracker.markError(opticaId, "upload_ventas", "batch", e.message)` is called
- AND the exception is rethrown

#### Scenario: Empty venta list marks batch as synced

- GIVEN zero ventas for the optica
- WHEN `uploadVentas` runs
- THEN `syncStateTracker.markSynced(opticaId, "upload_ventas", "batch")` is called
- AND the method returns 0

---

### Requirement: DeletionSyncHelper SHALL map gasto_operativo, venta, dispensacion_item, arqueo_caja

`DeletionSyncHelper.pushPendingDeletions` MUST map four entity types to their Supabase table name constants from `UploadSyncCoordinator.Companion`:

| entityType | Table constant | Supabase table |
|------------|----------------|----------------|
| `gasto_operativo` | `TABLE_GASTOS_OPERATIVOS` | `gastos_operativos` |
| `venta` | `TABLE_VENTAS` | `ventas` |
| `dispensacion_item` | `TABLE_DISPENSACION_ITEMS` | `dispensacion_items` |
| `arqueo_caja` | `TABLE_ARQUEO_CAJA` | `arqueo_caja` |

#### Scenario: gasto_operativo maps to gastos_operativos

- GIVEN a tombstone with `entityType = "gasto_operativo"`
- WHEN `pushPendingDeletions` processes it
- THEN DELETE targets `gastos_operativos`

#### Scenario: venta maps to ventas

- GIVEN a tombstone with `entityType = "venta"`
- WHEN `pushPendingDeletions` processes it
- THEN DELETE targets `ventas`

#### Scenario: dispensacion_item maps to dispensacion_items

- GIVEN a tombstone with `entityType = "dispensacion_item"`
- WHEN `pushPendingDeletions` processes it
- THEN DELETE targets `dispensacion_items`

#### Scenario: arqueo_caja maps to arqueo_caja

- GIVEN a tombstone with `entityType = "arqueo_caja"`
- WHEN `pushPendingDeletions` processes it
- THEN DELETE targets `arqueo_caja`

#### Scenario: Unmapped entity type falls through to else

- GIVEN a tombstone with an unrecognized `entityType`
- WHEN `pushPendingDeletions` processes it
- THEN `clearDeletionState` is called and no remote DELETE is attempted

---

### Requirement: Download Phase 1 SHALL retry pending remote deletes

The system MUST retry pending remote `paciente` deletions during `download()` Phase 1, before the download loop. A prior local delete with a failed remote delete leaves a tombstone. Phase 1 retries the remote DELETE; on IOException it logs and preserves the tombstone — Phase 2 `skipIds` then prevents re-insertion.

#### Scenario: Remote delete retry succeeds

- GIVEN a tombstone for paciente `"P1"`
- WHEN Phase 1 retries the remote DELETE and it succeeds
- THEN `clearEntityState(opticaId, "paciente", "P1")` is called

#### Scenario: Remote delete retry fails — tombstone preserved

- GIVEN a tombstone for `"P1"` and remote DELETE throws IOException
- WHEN Phase 1 retry runs
- THEN the error is logged and the tombstone is NOT cleared

### Requirement: Download Phase 2 SHALL skip re-insertion via skipIds

The system MUST combine `conflictDao.getConflictEntityIds()` and pending-deletion tombstones into a `skipIds` set. Remote entities whose ID is in `skipIds` MUST be skipped (`return@forEach`) during download, preventing resurrection after partial delete.

#### Scenario: Pending-delete ID skipped

- GIVEN a tombstone for `"P1"` exists AND `"P1"` is still on Supabase
- WHEN `download()` Phase 2 runs
- THEN `"P1"` is in `skipIds` and is NOT upserted to Room

#### Scenario: No tombstones — all inserted

- GIVEN zero pending deletions for the optica
- WHEN `download()` runs
- THEN all remote pacientes are upserted normally

### Requirement: savePaciente SHALL stamp updatedAt before upsert

`OptoRepository.insertPaciente` MUST set `updatedAt = Instant.now().toString()` before passing the entity to Room. Every create or edit MUST produce a fresh timestamp so conflict detection works correctly.

#### Scenario: New paciente gets fresh updatedAt

- GIVEN a Paciente with `updatedAt = null`
- WHEN `OptoRepository.insertPaciente` is called
- THEN the stored entity has a non-null `updatedAt` equal to the current instant

#### Scenario: Edit refreshes updatedAt

- GIVEN a Paciente with `updatedAt = "2025-01-01T00:00:00Z"`
- WHEN `OptoRepository.insertPaciente` is called
- THEN the stored `updatedAt` is greater than the previous value

### Requirement: Download Phase 1 SHALL propagate CancellationException

The Phase 1 inner try/catch MUST include `catch (e: CancellationException) { throw e }` before `catch (e: IOException)`. Without this, `CancellationException` is caught by the outer `catch (e: Exception)` and silently swallowed, blocking coroutine cancellation.

#### Scenario: Cancellation during Phase 1 propagates

- GIVEN the coroutine scope is cancelled while Phase 1 iterates tombstones
- WHEN `download()` Phase 1 runs
- THEN `CancellationException` is rethrown, not swallowed by the outer catch

#### Scenario: IOException does not cancel

- GIVEN a pending-delete retry throws IOException
- WHEN Phase 1 catches it
- THEN the error is logged and the loop continues to the next tombstone

### Requirement: savePaciente SHALL require admin or gerente role

`PacienteViewModel.savePaciente` MUST call `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` before performing the save, matching the pattern in `deletePacienteGuarded`.

#### Scenario: Admin saves successfully

- GIVEN role = `"admin"`
- WHEN `savePaciente` is called
- THEN the guard passes and the paciente is saved

#### Scenario: Non-admin is denied

- GIVEN role = `"vendedor"`
- WHEN `savePaciente` is called
- THEN `IllegalArgumentException` is thrown with message containing "Unauthorized"
- AND the paciente is NOT saved
