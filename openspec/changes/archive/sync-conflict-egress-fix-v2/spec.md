# Android Sync Conflict — Specification

**Change**: `sync-conflict-egress-fix-v2`
**Domain**: `android-sync-conflict`
**Type**: New full spec (no prior spec exists for this domain)
**TDD mode**: STRICT — every requirement has a RED scenario that must fail before implementation

---

## Purpose

Govern how the Android app detects, filters, and clears sync conflicts when uploading entities to Supabase. Three root causes (RC-1, RC-2, RC-3) cause unbounded egress and a permanently-growing stale conflict backlog. This spec describes the correct behavior after the fix.

---

## Requirements

### Requirement: ID-Scoped Remote Timestamp Fetch (RC-1)

`ConflictHelper.fetchRemoteUpdatedAt` MUST fetch remote rows filtered to exactly the set of entity IDs being evaluated. It MUST NOT download the full table.

When the `ids` parameter is empty, the method MUST return an empty map immediately without making any network call.

#### Scenario: Fetch filters by ID list

- GIVEN a non-empty list of entity IDs `[id1, id2]` and a table with 1 000 rows
- WHEN `fetchRemoteUpdatedAt(tableName, ids, opticaId)` is called
- THEN the Supabase query MUST include an `.in("id", ids)` filter
- AND only rows matching those IDs are returned (no extra rows)

#### Scenario: Empty ID list returns immediately

- GIVEN an empty list of entity IDs `[]`
- WHEN `fetchRemoteUpdatedAt(tableName, emptyList(), opticaId)` is called
- THEN the method returns an empty map
- AND no network call is made

#### Scenario: No extra rows returned

- GIVEN IDs `[id1, id2]` and a remote table containing rows for `id1`, `id2`, and `id3`
- WHEN `fetchRemoteUpdatedAt` is called with `[id1, id2]`
- THEN the returned map contains exactly `{id1: t1, id2: t2}`
- AND `id3` is NOT present in the result

---

### Requirement: Per-Module Scheduler Isolation (RC-2)

`PostSaveSyncScheduler.scheduleHistorialSync` MUST NOT invoke `syncPacientesUseCase`.

`PostSaveSyncScheduler.scheduleFinanzasSync` MUST NOT invoke `syncPacientesUseCase`.

Each scheduler MUST trigger only its own module's sync.

#### Scenario: scheduleHistorialSync does not cascade to pacientes

- GIVEN a valid `opticaId`
- WHEN `scheduleHistorialSync(opticaId)` is called
- THEN `syncHistorialUseCase` (or equivalent historial sync) is invoked
- AND `syncPacientesUseCase` is NOT invoked

#### Scenario: scheduleFinanzasSync does not cascade to pacientes

- GIVEN a valid `opticaId`
- WHEN `scheduleFinanzasSync(opticaId)` is called
- THEN the finanzas sync is invoked
- AND `syncPacientesUseCase` is NOT invoked

---

### Requirement: Auto-Clear of Stale Conflict Records (RC-3)

`ConflictHelper.filterConflicts` MUST call `conflictDao.resolveConflict(entityId, opticaId)` for every entity that is added to the `safe` list (local `updatedAt` >= remote `updatedAt`, or remote timestamp is null).

This auto-clear MUST be idempotent: calling `resolveConflict` for an entity that has no existing conflict record MUST be a no-op (no crash, no error).

#### Scenario: resolveConflict called for safe entity with existing record

- GIVEN entity `e1` with local `updatedAt = T2` and remote `updatedAt = T1` (T2 > T1)
- AND a `ConflictRecord` exists in the local DB for `e1`
- WHEN `filterConflicts` processes `e1`
- THEN `e1` is added to the `safe` list
- AND `conflictDao.resolveConflict(e1.id, opticaId)` is called exactly once

#### Scenario: resolveConflict NOT called for conflicted entity

- GIVEN entity `e2` with local `updatedAt = T1` and remote `updatedAt = T2` (T1 < T2, genuine conflict)
- WHEN `filterConflicts` processes `e2`
- THEN `e2` is added to the `conflicts` list
- AND `conflictDao.resolveConflict` is NOT called for `e2`

#### Scenario: Idempotent on entity with no existing record

- GIVEN entity `e3` with local `updatedAt = T2` and remote `updatedAt = T1` (safe)
- AND NO `ConflictRecord` exists for `e3`
- WHEN `filterConflicts` processes `e3`
- THEN `conflictDao.resolveConflict(e3.id, opticaId)` is called
- AND the call completes without error or exception

---

## Test Class Mapping (Strict TDD)

| Requirement | Test Class | Test Method(s) |
|------------|------------|----------------|
| RC-1: ID filter | `ConflictHelperTest` | `fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty` |
| RC-1: Empty IDs | `ConflictHelperTest` | `fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty` |
| RC-1: No extra rows | `ConflictHelperTest` | `fetchRemoteUpdatedAt_returnsOnlyRequestedIds` |
| RC-2: historial no pacientes | `PostSaveSyncSchedulerTest` | `scheduleHistorialSync_doesNotInvokeSyncPacientes` |
| RC-2: finanzas no pacientes | `PostSaveSyncSchedulerTest` | `scheduleFinanzasSync_doesNotInvokeSyncPacientes` |
| RC-3: safe entity clears record | `ConflictHelperTest` | `filterConflicts_callsResolveConflict_forSafeEntityWithRecord` |
| RC-3: conflict not cleared | `ConflictHelperTest` | `filterConflicts_doesNotCallResolveConflict_forConflictedEntity` |
| RC-3: idempotent no record | `ConflictHelperTest` | `filterConflicts_resolveConflict_isIdempotentWhenNoRecord` |

All test methods MUST be written and failing (RED) before any implementation code is added.

---

## Success Criteria

- Per silent sync no longer triggers full-table downloads (RC-1 verified by ID-filter test).
- Historial and finanzas saves do not cascade a pacientes sync (RC-2 verified by scheduler tests).
- Entities with local >= remote timestamp have their conflict record cleared on next `filterConflicts` call (RC-3 verified by auto-clear tests).
- All 8 test methods pass GREEN after implementation.
- Daily Supabase egress returns toward the ~32 MB baseline.
