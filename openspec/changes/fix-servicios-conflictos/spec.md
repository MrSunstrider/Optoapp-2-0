# Sync Conflict Resolution Specification

## Purpose

This spec governs how the Android app detects, surfaces, and resolves synchronization conflicts
for `servicio_extra`, `dispensacion`, and `pago` entities. A conflict exists when the local
`updatedAt` timestamp is older than the remote `updated_at` value at the time of sync.

## Requirements

### Requirement: Supabase Trigger Preserves Client Timestamp

The Supabase trigger `set_updated_audit_fields()` MUST NOT advance `updated_at` when the
client upsert includes an explicit `updated_at` value that differs from the existing row.
The trigger MUST only set `updated_at = now()` when the incoming value is identical to
the stored value (i.e., no client-supplied timestamp was sent).

#### Scenario: Upload with explicit timestamp

- GIVEN a row exists in Supabase with `updated_at = T_remote`
- WHEN the app upserts the row with `updated_at = T_local` where `T_local != T_remote`
- THEN Supabase stores `updated_at = T_local`
- AND a subsequent read of the row returns `updated_at = T_local`

#### Scenario: Upload without explicit timestamp change

- GIVEN a row exists with `updated_at = T`
- WHEN the app upserts with the same `updated_at = T` (no change)
- THEN the trigger advances `updated_at` to server clock
- AND the row reflects the new server timestamp

---

### Requirement: Download Must Not Overwrite Conflicted Entities

`DownloadSyncCoordinator` MUST skip downloading any entity whose ID appears in an active
`conflict_record` for the same `opticaId` and entity type. This applies to
`servicio_extra`, `dispensacion`, and `pago`.

#### Scenario: Conflicted entity is skipped during download

- GIVEN `conflict_records` contains an active record for entity ID `X` of type `servicio_extra`
- WHEN `downloadServicios(opticaId)` runs
- THEN entity `X` is NOT written to Room
- AND all non-conflicted entities ARE written to Room normally

#### Scenario: No active conflicts — download proceeds normally

- GIVEN no active `conflict_records` exist for the optica
- WHEN `downloadServicios(opticaId)` runs
- THEN all remote entities are written to Room

#### Scenario: Resolved conflict does not block future downloads

- GIVEN a conflict record for entity `X` existed and was resolved
- WHEN `downloadServicios(opticaId)` runs after resolution
- THEN entity `X` IS written to Room from remote

---

### Requirement: Keep-Mine Resolution Uploads and Clears the Conflict

When the user selects "Usar el mío" for a specific conflict, the app MUST:
1. Bump the entity's `updatedAt` to the current instant before uploading.
2. Upload the entity to Supabase within the same sync cycle.
3. Mark the conflict record as resolved only after a successful upload.
4. Ensure the conflict does NOT reappear on the next sync cycle.

#### Scenario: Happy path — keep-mine succeeds

- GIVEN entity `X` has an active conflict record
- WHEN the user taps "Usar el mío" for entity `X`
- THEN entity `X`'s `updatedAt` is set to `now()`
- AND entity `X` is uploaded to Supabase
- AND `conflict_records` entry for `X` is removed
- AND the next sync cycle completes without recreating a conflict for `X`

#### Scenario: Upload fails — conflict record is retained

- GIVEN entity `X` has an active conflict record
- WHEN the user taps "Usar el mío" and the upload to Supabase fails
- THEN the conflict record for `X` remains active
- AND the entity in Room is NOT marked as resolved

#### Scenario: Timestamp bump ensures local wins

- GIVEN entity `X` has `updatedAt = T_old` and remote has `updated_at = T_remote` where `T_remote > T_old`
- WHEN keep-mine bumps `updatedAt` to `T_now` where `T_now > T_remote`
- THEN `filterConflicts()` passes the entity for upload (local > remote)

---

### Requirement: Bulk Keep-Mine Resolves All Conflicts

`SyncViewModel.resolveKeepMineAll(opticaId)` MUST resolve all active conflict records for
the optica across all entity types (`servicio_extra`, `dispensacion`, `pago`) in one
operation, using the same bump-then-upload-then-resolve flow as single keep-mine.

#### Scenario: All conflicts cleared in bulk

- GIVEN N active conflict records across multiple entity types for the optica
- WHEN the user taps "Usar el mío para todos"
- THEN all N entities have their `updatedAt` bumped to `now()`
- AND all N entities are uploaded to Supabase
- AND all N conflict records are removed
- AND the next sync cycle produces zero conflicts for those entities

#### Scenario: Partial failure in bulk — successfully resolved entities stay resolved

- GIVEN 3 active conflicts: entities A (servicio), B (dispensacion), C (pago)
- WHEN bulk keep-mine runs and upload for B fails
- THEN A and C are resolved and their conflict records removed
- AND B retains its active conflict record

---

### Requirement: Accept-All-Cloud Regression Safety

`acceptAllCloud(opticaId)` MUST continue to overwrite all conflicted entities in Room with
their remote versions and clear all conflict records, unaffected by the download-guard or
keep-mine changes.

#### Scenario: Accept all cloud still works

- GIVEN N active conflict records exist for the optica
- WHEN the user taps "Usar nube para todos"
- THEN all N Room entities are overwritten with remote values
- AND all N conflict records are removed
- AND the next sync cycle produces zero conflicts

---

### Requirement: Normal Sync Is Unaffected by Conflict Guard

The download guard MUST only skip entities with ACTIVE conflict records. Entities with no
conflict record MUST be downloaded and written to Room as before. Upload behavior for
non-conflicted entities MUST be unchanged.

#### Scenario: Non-conflicted entities sync normally

- GIVEN zero active conflict records for entity `Y`
- WHEN a full sync cycle runs
- THEN entity `Y` is downloaded and written to Room
- AND entity `Y` is uploaded if locally modified

---

### Requirement: ConflictDao Exposes Conflict ID Query

`ConflictDao` MUST expose `getConflictEntityIds(opticaId: String, entityType: String): List<String>`
returning the IDs of all entities with active conflict records for the given optica and type.

#### Scenario: Query returns only active conflict IDs

- GIVEN two active conflict records for `servicio_extra` and one resolved record
- WHEN `getConflictEntityIds(opticaId, "servicio_extra")` is called
- THEN the result contains exactly the two active entity IDs
- AND the resolved entity ID is NOT in the result
