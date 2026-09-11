# Delta for Sync Conflict

## MODIFIED Requirements

### FR-01: Download Guard for All Entity Types

All 10 unprotected download methods MUST skip entities with active `conflict_records` using the pattern: `conflictDao.getConflictEntityIds(opticaId, entityType).toSet()` → skip if ID in set.

For **`SyncPacientesUseCase.download()` only**, guard-set assembly MUST also include pending delete tombstones via Phase 2 `getPendingDeletions(opticaId)` filtered to `entityType = "paciente"`. If either the conflict-ID query or the pending-deletions query throws any exception other than `CancellationException`, the download MUST fail-closed: log the error, mark the download batch as failed, perform zero upserts, and MUST NOT substitute `emptySet()` to proceed. `CancellationException` MUST propagate unchanged.

All other entity types listed below MUST retain fail-open behavior on guard query failure: log the error and proceed without the guard.

NOTE: `inventario_fisico` and `inventario_fisico_detalle` are EXCLUDED from download guard because the `InventarioFisico` entity has NO `updatedAt` field — `filterConflicts` never creates `conflict_records` for this type, making the guard a no-op. These entity types are append/sequential in nature (one session at a time) and rarely generate real conflicts.

NOTE: `montura_movimiento` download guard depends on FR-08 (filterConflictMovimientos creating conflict_records). Without FR-08, the guard is a no-op. Apply the guard pattern anyway (it will activate once FR-08 ships).

| # | File | Method | entityType | Applies now? |
|---|------|--------|------------|-------------|
| 1 | SyncPacientesUseCase | `download()` | paciente | ✅ Yes — fail-closed on guard query failure |
| 2 | SyncHistorialUseCase | `downloadEvaluaciones()` | evaluacion | ✅ Yes — fail-open on guard query failure |
| 3 | SyncInventarioUseCase | `downloadMonturas()` | montura | ✅ Yes — fail-open on guard query failure |
| 4 | SyncInventarioUseCase | `downloadMovimientos()` | montura_movimiento | ⚠️ Depends on FR-08 — fail-open on guard query failure |
| 5 | SyncProveedoresUseCase | `downloadProveedores()` | proveedor | ✅ Yes — fail-open on guard query failure |
| 6 | SyncProveedoresUseCase | `downloadCategorias()` | categoria_montura | ✅ Yes — fail-open on guard query failure |
| 7 | SyncOrdenesCompraUseCase | `downloadOrdenesCompra()` | orden_compra | ✅ Yes — fail-open on guard query failure |
| 8 | SyncOrdenesCompraUseCase | `downloadItems()` | orden_compra_item | ✅ Yes — fail-open on guard query failure |
| 9 | DownloadSyncCoordinator | `downloadDispensacionItems()` | dispensacion_item | ✅ Yes — fail-open on guard query failure |
| 10 | DownloadSyncCoordinator | `downloadArqueos()` | arqueo_caja | ✅ Yes — fail-open on guard query failure |

(Previously: all entity types, including pacientes, proceeded without guard on any guard query error — fail-open with `emptySet()`.)

#### Scenario: Conflicted entity skipped during download

- GIVEN active conflict_record for entity X of type `paciente`
- WHEN `SyncPacientesUseCase.download()` runs
- THEN entity X is NOT written to Room
- AND all non-conflicted entities ARE written normally

#### Scenario: No conflicts — download proceeds normally

- GIVEN zero active conflict_records for the optica
- WHEN any download method runs
- THEN all remote entities are written to Room

#### Scenario: Resolved conflict unblocks download

- GIVEN conflict_record for entity X was resolved
- WHEN download runs after resolution
- THEN entity X IS written to Room from remote

#### Scenario: Pacientes conflict guard query fails — fail-closed

- GIVEN `conflictDao.getConflictEntityIds(opticaId, "paciente")` throws a non-cancellation exception
- WHEN `SyncPacientesUseCase.download()` runs
- THEN zero paciente upserts are performed
- AND the error is logged
- AND the download batch is marked failed
- AND remote pacientes are NOT written to Room

#### Scenario: Pacientes pending-deletions guard query fails — fail-closed

- GIVEN Phase 2 `getPendingDeletions(opticaId)` throws a non-cancellation exception during pacientes download
- WHEN `SyncPacientesUseCase.download()` runs
- THEN zero paciente upserts are performed
- AND the error is logged
- AND the download batch is marked failed
- AND entities with pending delete tombstones are NOT resurrected from remote

#### Scenario: Pacientes guard query cancellation propagates

- GIVEN a guard query in `SyncPacientesUseCase.download()` throws `CancellationException`
- WHEN download is cancelled
- THEN `CancellationException` propagates to the caller
- AND fail-closed handling MUST NOT swallow the cancellation

#### Scenario: Non-paciente guard query error — fail-open unchanged

- GIVEN `conflictDao.getConflictEntityIds` throws for a non-paciente download method (e.g. evaluacion)
- WHEN that download method runs
- THEN download proceeds without the guard
- AND the error is logged

#### Scenario: Network error during conflict query (non-paciente)

- GIVEN ConflictDao query throws IOException during a non-paciente download
- WHEN that download method runs
- THEN download proceeds without guard (fail-open) and logs error
