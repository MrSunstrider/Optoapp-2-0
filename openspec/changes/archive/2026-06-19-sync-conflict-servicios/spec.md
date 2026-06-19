# sync-download-timestamp-integrity Specification

## Purpose

Ensure the Android offline-first sync layer never re-stamps or re-schedules records received
from the server. Two distinct write paths MUST be kept explicit and separate in `OptoRepository`:
a **remote path** (download) that preserves the server timestamp verbatim, and a **local path**
(user action) that stamps `Instant.now()` and enqueues an upload job.

## Requirements

### Requirement: Remote Write Path Preserves Server Timestamp

When a record arrives via a server download, `OptoRepository` MUST store the entity with
`updatedAt` exactly equal to the value received from the remote source. The repository MUST NOT
substitute a local timestamp (`Instant.now()`) on this path.

Applies to: `ServicioExtra`, `DispensacionOptica`, `Pago`, `EvaluacionClinica`.

#### Scenario: upsertServicioFromRemote preserves updatedAt

- GIVEN a `ServicioExtra` entity with `updatedAt = T_remote` received from Supabase
- WHEN `OptoRepository.upsertServicioFromRemote(entity)` is called
- THEN the record stored in the local DB has `updatedAt == T_remote`
- AND `T_remote` is NOT replaced by a later `Instant.now()` value

#### Scenario: upsertDispensacionFromRemote preserves updatedAt

- GIVEN a `DispensacionOptica` entity with `updatedAt = T_remote` received from Supabase
- WHEN `OptoRepository.upsertDispensacionFromRemote(entity)` is called
- THEN the record stored in the local DB has `updatedAt == T_remote`

#### Scenario: upsertPagoFromRemote preserves updatedAt

- GIVEN a `Pago` entity with `updatedAt = T_remote` received from Supabase
- WHEN `OptoRepository.upsertPagoFromRemote(entity)` is called
- THEN the record stored in the local DB has `updatedAt == T_remote`

#### Scenario: upsertEvaluacionFromRemote preserves updatedAt

- GIVEN an `EvaluacionClinica` entity with `updatedAt = T_remote` received from Supabase
- WHEN `OptoRepository.upsertEvaluacionFromRemote(entity)` is called
- THEN the record stored in the local DB has `updatedAt == T_remote`

#### Scenario: Full download cycle timestamp fidelity

- GIVEN a download cycle that fetches N records for any of the 4 entity types
- WHEN all records are persisted via their respective `upsertXxxFromRemote()` methods
- THEN for every stored record, `stored.updatedAt == remote.updatedAt`
- AND no stored record has an `updatedAt` value greater than its corresponding remote value

---

### Requirement: Remote Write Path Does Not Schedule Upload

When a record is persisted via a download path, the sync scheduler MUST NOT be called.
The download operation IS the terminal step of a sync cycle; no follow-up upload job
shall be enqueued.

Applies to: `ServicioExtra`, `DispensacionOptica`, `Pago`, `EvaluacionClinica`.

#### Scenario: upsertServicioFromRemote does not trigger scheduler

- GIVEN a `ServicioExtra` entity received from the server
- WHEN `OptoRepository.upsertServicioFromRemote(entity)` is called
- THEN `postSaveSyncScheduler.scheduleServicioSync()` (or equivalent) is NOT called

#### Scenario: upsertDispensacionFromRemote does not trigger scheduler

- GIVEN a `DispensacionOptica` entity received from the server
- WHEN `OptoRepository.upsertDispensacionFromRemote(entity)` is called
- THEN the sync scheduler is NOT called for any entity type

#### Scenario: upsertPagoFromRemote does not trigger scheduler

- GIVEN a `Pago` entity received from the server
- WHEN `OptoRepository.upsertPagoFromRemote(entity)` is called
- THEN the sync scheduler is NOT called

#### Scenario: upsertEvaluacionFromRemote does not trigger scheduler

- GIVEN an `EvaluacionClinica` entity received from the server
- WHEN `OptoRepository.upsertEvaluacionFromRemote(entity)` is called
- THEN the sync scheduler is NOT called

---

### Requirement: Local Write Path Stamps Timestamp and Schedules Upload

When a record is created or updated by a user action (form submit, local edit), `OptoRepository`
MUST stamp `updatedAt` with `Instant.now()` and MUST schedule the appropriate sync job.
This behavior is unchanged by this change.

#### Scenario: insertServicio stamps updatedAt and schedules sync

- GIVEN a `ServicioExtra` entity with `updatedAt = null` (new record from user form)
- WHEN `OptoRepository.insertServicio(entity)` is called
- THEN the stored record has `updatedAt` set to a non-null value close to `Instant.now()`
- AND the sync scheduler is called for ServicioExtra

#### Scenario: updateServicio refreshes updatedAt and schedules sync

- GIVEN a `ServicioExtra` entity with an existing `updatedAt` value
- WHEN `OptoRepository.updateServicio(entity)` is called
- THEN the stored record has a refreshed `updatedAt` value (`>= original updatedAt`)
- AND the sync scheduler is called for ServicioExtra

---

### Requirement: DispensacionMergeHandler Local-Edit Path Remains Unchanged

`DispensacionMergeHandler.mergeLocalDispensacionConflict()` calls `updateDispensacion()`,
which is a local user-action path. This MUST continue to stamp `updatedAt` and schedule sync.
The merge handler MUST NOT be migrated to the remote bypass path.

#### Scenario: Merge handler uses stamping update path

- GIVEN a local dispensacion conflict resolved by `DispensacionMergeHandler`
- WHEN `mergeLocalDispensacionConflict()` executes
- THEN `updateDispensacion()` is called (not `upsertDispensacionFromRemote()`)
- AND the stored record has a refreshed `updatedAt`
- AND the sync scheduler is called

---

### Requirement: Path Separation Is Explicit in OptoRepository

`OptoRepository` MUST expose dedicated `upsertXxxFromRemote(entity)` methods for each of
the 4 affected entities. Download-path callers (`DownloadSyncCoordinator` for Servicio,
Dispensacion, Pago; `SyncHistorialUseCase` for Evaluacion) MUST use only these bypass
methods. User-action callers MUST NOT use the bypass methods.

#### Scenario: Download caller uses bypass method

- GIVEN `DownloadSyncCoordinator` receiving a batch of `ServicioExtra` records from Supabase
- WHEN it persists each record
- THEN it calls `upsertServicioFromRemote()`, not `insertServicio()` or `updateServicio()`

#### Scenario: User-action caller does not use bypass method

- GIVEN a user saving a new `ServicioExtra` via the UI form
- WHEN the repository method is called
- THEN `insertServicio()` or `updateServicio()` is used, not `upsertServicioFromRemote()`
