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
