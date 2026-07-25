# sync-resilience Specification

## Purpose

Sync error propagation and telemetry history. Finanzas upload errors MUST propagate to the orchestrator. Remote telemetry MUST support append-only logging alongside the existing UPSERT table. Per-module sync errors MUST reach `BackgroundErrorCollector` so diagnostics surfaces them.

## Requirements

### Requirement: safeUpload non-transient error propagation

`SyncFinanzasUseCase.safeUpload()` MUST propagate non-transient errors (IOException from network failures) instead of silently returning 0. Transient errors (socket timeout, connection reset) SHALL be caught and retried within `safeUpload` before propagating. The use case's `invoke()` MUST return `Resource.Error` when a non-transient upload error escapes `safeUpload`.

#### Scenario: Full network failure propagates

- GIVEN all upload REST calls throw IOException with no partial progress
- WHEN `safeUpload()` is called
- THEN the IOException propagates to `invoke()`
- AND `invoke()` returns `Resource.Error`

#### Scenario: Partial entity failure counts as success

- GIVEN dispensaciones upload succeeds but pagos upload throws IOException
- WHEN `safeUpload()` catches the per-entity error via `UploadPartialException`
- THEN successful entity counts are returned
- AND `invoke()` returns `Resource.Success` with partial counts

#### Scenario: Transient error retried then propagated

- GIVEN an upload REST call throws SocketTimeoutException
- WHEN `safeUpload()` catches it
- THEN the operation is retried up to a bounded limit
- AND if retries are exhausted, the IOException propagates

### Requirement: sync_telemetry_log append-only table

A `sync_telemetry_log` table MUST exist in Supabase with columns: id (UUID PK), optica_id (FK references opticas(id) ON DELETE CASCADE), status (text NOT NULL), stage (text NOT NULL DEFAULT ''), error_message (text NOT NULL DEFAULT ''), created_at (timestamptz NOT NULL DEFAULT now()). The table SHALL accept INSERT only. RLS MUST restrict SELECT to authenticated users whose optica_id matches the row.

#### Scenario: Sync cycle inserts log row

- GIVEN a sync cycle completes for optica "O1"
- WHEN `recordRemoteSyncTelemetry` is called
- THEN both `sync_telemetry_optica` (UPSERT) AND `sync_telemetry_log` (INSERT) are written

#### Scenario: History preserved across cycles

- GIVEN optica "O1" had a failed sync at T1 and a successful sync at T2
- WHEN querying `sync_telemetry_log` WHERE optica_id = 'O1' ORDER BY created_at DESC
- THEN both the T1 error row and T2 success row are present

### Requirement: Room entity for sync telemetry log

The Android data layer MUST define a `SyncTelemetryLogEntity` Room entity mirroring `sync_telemetry_log` with columns: id (TEXT PK), opticaId, status, stage, errorMessage, createdAt (Long, epoch millis). A corresponding DAO MUST support INSERT and ordered SELECT by opticaId.

#### Scenario: Local log row persisted

- GIVEN a remote telemetry log entry is written successfully
- WHEN the entry is mapped to `SyncTelemetryLogEntity`
- THEN it is inserted into the local Room database
- AND is queryable by `opticaId` ordered by `createdAt` descending

### Requirement: Per-module error recording in orchestrator

`SyncOrchestrator.executeModules()` SHALL call `BackgroundErrorCollector.record()` for each module returning `Resource.Error`. The error message SHALL include the module name and the specific error string. `executeSilentModules()` SHALL also record errors to `BackgroundErrorCollector` in addition to its existing per-module callback.

#### Scenario: Single module fails

- GIVEN `SyncFinanzasUseCase.invoke()` returns `Resource.Error("timeout")`
- WHEN `executeModules()` processes finanzas
- THEN `BackgroundErrorCollector.record("Sync", "Error en finanzas: timeout")` is called

#### Scenario: Multiple modules fail

- GIVEN finanzas returns `Resource.Error("timeout")` AND inventario returns `Resource.Error("network")`
- WHEN `executeModules()` processes all modules
- THEN two `record()` calls are made with module-specific messages
- AND `SyncDiagnosticsViewModel.errors` Flow emits both entries

#### Scenario: Silent sync module fails

- GIVEN `executeSilentModules()` processes proveedores which returns `Resource.Error`
- WHEN the module completes
- THEN `BackgroundErrorCollector.record()` is called with the module error
- AND the existing `onModuleResult` callback is also invoked
