# Sync Conflict Root-Cause Fix — Specification

**Change**: `sync-conflict-rootcause`
**Artifact store**: hybrid
**TDD mode**: STRICT — every requirement has a corresponding test scenario that MUST be written and failing before implementation begins.

---

## Purpose

Eliminate six independent root causes that cause phantom sync conflicts to regenerate after each sync cycle in the Android app. Manual resolutions MUST be durable. The Supabase trigger fix (migration 20260615) closed RC-0; this spec covers RC-1 through RC-6.

---

## PR-A Requirements: Durable Conflict Resolutions (RC-3 + RC-4)

### Requirement: REQ-A1 — resolveKeepMine Triggers Upload

When the user resolves a conflict by keeping their local version, the local entity version MUST be uploaded to Supabase before the conflict record is deleted from Room. The upload MUST use the same upsert path as a normal sync upload.

### Requirement: REQ-A2 — resolveKeepMine Writes Server Timestamp Back

After a successful upload in `resolveKeepMine`, the server-confirmed `updated_at` MUST be written back to the local Room entity. The conflict MUST NOT regenerate on the next silent sync cycle.

### Requirement: REQ-A3 — acceptAllCloud Clears sync_entity_state

`acceptAllCloud` MUST clear both `conflict_records` AND all rows with `sync_status = CONFLICTED` in `sync_entity_state` for the current optica. Stale conflicted state rows MUST NOT remain after this operation.

### Requirement: REQ-A4 — SyncEntityStateDao Exposes deleteConflictedForOptica

`SyncEntityStateDao` MUST expose a `deleteConflictedForOptica(opticaId)` query that deletes all rows where `optica_id = :opticaId` AND `sync_status = 'CONFLICTED'`. `acceptAllCloud` MUST call this method inside the same transaction as `clearConflicts()`.

---

## PR-B Requirements: Timestamp Correctness (RC-1 + RC-2)

### Requirement: REQ-B1 — updatedAt Stamped at Room Save, Not at toRemoto()

Every entity's `updatedAt` field MUST be set at the moment the entity is saved to Room (insert or update), not inside `toRemoto()`. `toRemoto()` MUST NOT fall back to `Instant.now()` when `updatedAt` is null. A null `updatedAt` after a Room save is a bug and MUST be caught in unit tests.

### Requirement: REQ-B2 — null updatedAt Does Not Drift Across Sync Cycles

If an entity is loaded from Room with a null `updatedAt`, the system MUST NOT silently substitute `Instant.now()` during serialization. The resulting remote timestamp MUST be deterministic and idempotent across multiple silent sync cycles (same value for same entity state).

### Requirement: REQ-B3 — Silent Sync Persists Server-Confirmed Timestamp

After a successful upload during a silent sync (`performSilentSync`), the server-confirmed `updated_at` returned by Supabase MUST be written back to the Room entity. Room MUST NOT retain the pre-upload timestamp after the sync completes.

### Requirement: REQ-B4 — Roundtrip Timestamp Stability

A full upload→download cycle MUST leave the entity's `updatedAt` in Room equal to the server-confirmed value. Running the cycle a second time with no data changes MUST produce no conflict and no timestamp change.

---

## PR-C Requirements: Hardening — Race Fix + Pagos Guard (RC-5 + RC-6)

### Requirement: REQ-C1 — cancelPending Completes Before performFullDownload

`cancelPending()` MUST be a `suspend` function that awaits job cancellation before returning. Callers that proceed to `performFullDownload` MUST only do so after `cancelPending()` has completed. A fire-and-forget call to `cancelPending()` is prohibited.

### Requirement: REQ-C2 — uploadPagos Calls filterConflicts

`uploadPagos` MUST call `filterConflicts` on the pago list before pushing to Supabase. Pagos that have an active conflict record in Room MUST be excluded from the upload. `uploadPagos` MUST NOT silently overwrite remote pago data for entities with unresolved conflicts.

---

## Acceptance Scenarios

### PR-A Scenarios

#### Scenario: REQ-A1 — keepMine triggers upload before conflict deletion

- GIVEN a `ConflictRecord` for a `Paciente` entity exists in Room
- WHEN `resolveKeepMine(conflictId)` is called
- THEN the local `Paciente` is upserted to Supabase via the standard upload path
- AND the upsert completes BEFORE `conflict_records` row is deleted

#### Scenario: REQ-A2 — keepMine writes server timestamp back to Room

- GIVEN `resolveKeepMine` has uploaded the local entity and received a server response
- WHEN the upload succeeds
- THEN the server-confirmed `updated_at` is written to the Room entity
- AND a subsequent silent sync does NOT create a new `ConflictRecord` for the same entity

#### Scenario: REQ-A3 — acceptAllCloud leaves sync_entity_state empty

- GIVEN one or more `sync_entity_state` rows exist with `sync_status = CONFLICTED` for optica `X`
- WHEN `acceptAllCloud()` is called for optica `X`
- THEN all `conflict_records` are deleted for optica `X`
- AND all `sync_entity_state` rows with `sync_status = CONFLICTED` for optica `X` are deleted

#### Scenario: REQ-A4 — deleteConflictedForOptica removes only conflicted rows

- GIVEN `sync_entity_state` has rows: 2 with `CONFLICTED`, 1 with `SYNCED`, for optica `X`
- WHEN `deleteConflictedForOptica(opticaId = X)` is called
- THEN the 2 `CONFLICTED` rows are deleted
- AND the 1 `SYNCED` row remains untouched

---

### PR-B Scenarios

#### Scenario: REQ-B1 — toRemoto does not call Instant.now() for null updatedAt

- GIVEN a `Paciente` entity in Room with `updatedAt = null`
- WHEN `toRemoto()` is called on that entity
- THEN no call to `Instant.now()` is made inside `toRemoto()`
- AND the test verifies this by asserting the system clock was not accessed (MockK `verify(exactly = 0) { Instant.now() }`)

#### Scenario: REQ-B2 — null updatedAt is stable across 3 silent sync cycles

- GIVEN a `Paciente` entity with `updatedAt = null` saved to Room
- WHEN `performSilentSync()` is called 3 times consecutively with no data changes
- THEN no new `ConflictRecord` is created after the first cycle
- AND the `updatedAt` value observed on the remote call is identical in all 3 cycles

#### Scenario: REQ-B3 — silent sync writes server timestamp to Room after upload

- GIVEN a `Paciente` entity with `updatedAt = T1` is uploaded during `performSilentSync`
- WHEN Supabase returns `updated_at = T2` (T2 > T1)
- THEN the Room entity's `updatedAt` is updated to `T2`
- AND a subsequent `performSilentSync` does NOT create a conflict for this entity

#### Scenario: REQ-B4 — full upload→download roundtrip is timestamp-stable

- GIVEN a `Paciente` entity is uploaded and the server returns `updated_at = T2`
- WHEN a full download is then performed
- THEN the Room entity's `updatedAt` equals `T2`
- AND a second upload→download cycle produces no conflict and no timestamp change

---

### PR-C Scenarios

#### Scenario: REQ-C1 — cancelPending completes before performFullDownload executes

- GIVEN a pending sync job is in progress
- WHEN the sync flow requests `cancelPending()` followed by `performFullDownload()`
- THEN `performFullDownload` does not start until `cancelPending` has completed (job cancellation confirmed)
- AND the test asserts sequential execution order via coroutine ordering assertions

#### Scenario: REQ-C1 — cancelPending is a suspend function (compile-time contract)

- GIVEN the `PostSaveSyncScheduler` source file
- WHEN `cancelPending()` signature is inspected
- THEN it is declared as `suspend fun cancelPending()`
- AND all call sites use `cancelPending()` from a coroutine scope

#### Scenario: REQ-C2 — uploadPagos excludes conflicted pagos

- GIVEN 3 `Pago` entities exist in Room, 1 has an active `ConflictRecord`
- WHEN `uploadPagos()` is called
- THEN `filterConflicts` is called on the pago list before the Supabase push
- AND only the 2 non-conflicted pagos are sent to Supabase
- AND no Supabase upsert is called for the conflicted pago

#### Scenario: REQ-C2 — uploadPagos does not overwrite remote when conflict exists

- GIVEN a `Pago` entity has a remote version newer than local AND has a `ConflictRecord`
- WHEN `uploadPagos()` is called
- THEN the Supabase upsert for that pago is NOT called
- AND the `ConflictRecord` remains intact in Room

---

## Test Contract

### PR-A Tests

| Test class | Method | Type | Key assertions |
|---|---|---|---|
| `SyncViewModelTest` | `resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict` | Unit (MockK) | `verify { supabaseRepo.upsertPaciente(any()) }` called before `conflictDao.delete(conflictId)` |
| `SyncViewModelTest` | `resolveKeepMine_writesServerTimestampToRoom` | Unit (MockK) | Room entity `updatedAt` equals server response timestamp after call |
| `SyncViewModelTest` | `resolveKeepMine_doesNotRegenerateConflictOnNextSync` | Unit (MockK) | No `ConflictRecord` inserted after subsequent `performSilentSync()` call |
| `SyncViewModelTest` | `acceptAllCloud_clearsBothConflictRecordsAndSyncEntityState` | Unit (MockK) | `verify { syncEntityStateDao.deleteConflictedForOptica(opticaId) }` called; zero conflicted rows remain |
| `SyncEntityStateDaoTest` | `deleteConflictedForOptica_deletesOnlyConflictedRows` | Integration (Room in-memory) | 2 CONFLICTED rows deleted; 1 SYNCED row survives |

### PR-B Tests

| Test class | Method | Type | Key assertions |
|---|---|---|---|
| `PacienteRemotoMapperTest` | `toRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull` | Unit (MockK) | `verify(exactly = 0) { Instant.now() }` inside `toRemoto()` call |
| `SyncViewModelTest` | `silentSync_nullUpdatedAt_stableAcross3Cycles` | Unit (MockK) | Remote timestamp argument identical on all 3 `upsert` calls; no conflict created |
| `SyncViewModelTest` | `silentSync_writesServerTimestampToRoom_afterUpload` | Unit (MockK) | Room entity `updatedAt` equals Supabase response `T2` after upload |
| `SyncIntegrationTest` | `uploadDownloadRoundtrip_timestampStable` | Integration (Room in-memory + MockK Supabase) | Entity `updatedAt` equals server value; second roundtrip produces no conflict |

### PR-C Tests

| Test class | Method | Type | Key assertions |
|---|---|---|---|
| `PostSaveSyncSchedulerTest` | `cancelPending_isSuspend_completesBeforeFullDownload` | Unit (MockK coroutines) | `performFullDownload` not invoked until `cancelPending` deferred completes; `TestCoroutineScheduler` used |
| `UploadSyncCoordinatorTest` | `uploadPagos_callsFilterConflicts_beforePush` | Unit (MockK) | `verify { conflictFilter.filterConflicts(any()) }` called before `supabaseRepo.upsertPagos(any())` |
| `UploadSyncCoordinatorTest` | `uploadPagos_excludesConflictedPago_fromSupabasePush` | Unit (MockK) | `supabaseRepo.upsertPagos` called with list NOT containing conflicted pago ID |
| `UploadSyncCoordinatorTest` | `uploadPagos_doesNotCallUpsert_forConflictedPago` | Unit (MockK) | `verify(exactly = 0) { supabaseRepo.upsertPago(conflictedPagoId) }` |

**Strict TDD rule**: every test above MUST be written and confirmed failing before the corresponding implementation code is written.

---

## Out of Scope

- P6: Adding `updated_at` column to `dispensacion_items` and `montura_movimientos` — schema change, deferred to separate change.
- Any changes to `optoweb` (Next.js web app).
- Any new Supabase migrations — trigger fix (migration 20260615) is already applied and is a prerequisite.

---

## Definition of Done

- All REQ-A*, REQ-B*, REQ-C* tests pass (unit + integration).
- `./gradlew :optoapp:testDebugUnitTest` exits with zero failures.
- No new `ConflictRecord` appears after a full upload→silent sync→conflict check cycle executed in the test suite.
- `acceptAllCloud` leaves both `conflict_records` and `sync_entity_state` tables empty (zero rows with CONFLICTED status) for the tested optica.
- Each PR-A, PR-B, PR-C is independently verifiable and rollback-safe.
