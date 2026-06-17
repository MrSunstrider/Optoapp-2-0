# Design: Sync Conflict Egress Fix v2

## Technical Approach

Two-file surgical fix mapped 1:1 to the spec requirements (RC-1, RC-2, RC-3),
following existing patterns in `ConflictHelper` and `PostSaveSyncScheduler`. No
new classes, no schema change, no migration. Strict TDD: the 8 test methods from
the spec's Test Class Mapping are written RED first, then implementation drives
them GREEN.

- RC-1: `ConflictHelper.fetchRemoteUpdatedAt` adds an `isIn("id", ids)` filter so
  the query is ID-scoped (O(k)) instead of full-table (O(N)).
- RC-2: Remove the two stray `syncPacientesUseCase!!(opticaId)` cascade lines in
  `scheduleHistorialSync` and `scheduleFinanzasSync`.
- RC-3: In `filterConflicts`, call `conflictDao.resolveConflict(id, opticaId)`
  for every entity routed to the `safe` list — idempotent self-healing.

## Architecture Decisions

### Decision: RC-1 filter API

**Choice**: `filter { eq("optica_id", opticaId); isIn("id", ids) }` (postgrest-kt
3.6.0). Drop the in-memory `idSet` post-filter; build the result map directly from
the returned rows.

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `isIn("id", ids)` inside existing `filter {}` block | Native 3.x API, server-side filter, keeps `optica_id` guard | CHOSEN |
| Keep `eq` only + in-memory filter | Still downloads full table — the bug | Rejected |
| Use a raw RPC | Over-engineered for a one-line fix | Rejected |

**Rationale**: `isIn` is the 3.6.0 `PostgrestFilterBuilder` method for SQL `IN`.
Server-side filtering is the egress fix; the `idSet` memory pass becomes dead code.
Keep `optica_id` for tenant isolation.

### Decision: RC-1 testability — extract a seam

**Choice**: Keep `fetchRemoteUpdatedAt` but make it `internal` and split the
network call behind an overridable `protected open suspend fun selectRemoteRows(
tableName, opticaId, ids): List<RemoteTimestamp>`. Tests subclass `ConflictHelper`
(mirroring `PostSaveSyncSchedulerTest`'s subclass pattern), override
`selectRemoteRows` to capture the `ids` argument, and assert the filter contract.

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Subclass + override seam | Matches existing scheduler test style; no MockK of Supabase DSL needed | CHOSEN |
| MockK the `SupabaseClient` postgrest DSL | The `filter {}` builder lambda is hard to verify reliably with MockK | Rejected |
| Make method `public`, hit a fake server | Heavyweight for a unit test | Rejected |

**Rationale**: The spec asserts "the query MUST include `.in("id", ids)`". The
Supabase DSL lambda is not introspectable via MockK, so we verify intent at a seam
that receives `ids`. The seam's body contains the real `isIn` call. Note: spec
scenarios show arg order `(tableName, ids, opticaId)`; the real signature is
`(tableName, opticaId, ids)` — tasks/tests MUST use the real order.

### Decision: RC-3 per-entity vs batch clear

**Choice**: Option A — call `conflictDao.resolveConflict(entity.id, opticaId)`
inline in the `safe` branch of the existing loop, unconditionally.

| Option | Tradeoff | Decision |
|--------|----------|----------|
| A: inline `resolveConflict` per safe entity | Simplest; fits current loop; idempotent | CHOSEN |
| B: collect safe IDs + new `clearConflictsForIds` DAO | Fewer DB calls but new DAO method + new test surface | Rejected (out of scope) |

**Rationale**: `resolveConflict` is `DELETE ... WHERE entityId AND opticaId` —
SQLite DELETE on a missing row affects 0 rows and never errors, so it is ALREADY
idempotent. No `getConflicts` pre-check needed (that would add an extra query and
violate the spec's idempotency scenario). Batch (B) only matters at large safe
volumes; with RC-1 making fetches ID-scoped, safe lists stay bounded per sync.

### Decision: RC-2 mechanical removal

**Choice**: Delete line `syncPacientesUseCase!!(opticaId)` from `scheduleHistorialSync`
(currently line 123) and from `scheduleFinanzasSync` (currently line 147). No
structural change. Confirmed both calls exist exactly as the proposal states.

## Data Flow

```
filterConflicts(localEntities)
   │  checkableIds = entities with updatedAt
   ▼
fetchRemoteUpdatedAt(table, opticaId, ids)
   │  selectRemoteRows → postgrest.select { eq(optica_id); isIn("id", ids) }   ← RC-1
   ▼  Map<id, remote_updated_at>   (only k rows, not N)
loop each entity:
   local >= remote  ─→ safe.add(e); conflictDao.resolveConflict(e.id, opticaId) ← RC-3
   local <  remote  ─→ conflictDao.upsertConflict(...); markConflicted(...)
```

Scheduler (RC-2): `scheduleHistorialSync → syncHistorialUseCase` only;
`scheduleFinanzasSync → syncFinanzasUseCase` only. No pacientes cascade.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt` | Modify | RC-1 `isIn` filter + `selectRemoteRows` seam; RC-3 `resolveConflict` in safe branch; make `fetchRemoteUpdatedAt` `internal` |
| `optoapp/src/main/java/com/example/optoapp/sync/PostSaveSyncScheduler.kt` | Modify | RC-2 remove 2 `syncPacientesUseCase!!(opticaId)` cascade lines |
| `optoapp/src/test/java/com/example/optoapp/domain/sync/ConflictHelperTest.kt` | Modify | Add RC-1 (3) + RC-3 (3) tests; class needs a `ConflictHelper` instance + mocked `ConflictDao` |
| `optoapp/src/test/java/com/example/optoapp/sync/PostSaveSyncSchedulerTest.kt` | Modify | Add RC-2 (2) tests using existing subclass/MockK pattern |

## Interfaces / Contracts

```kotlin
// ConflictHelper.kt — testable seam (RC-1)
@VisibleForTesting
protected open suspend fun selectRemoteRows(
    tableName: String, opticaId: String, ids: List<String>
): List<RemoteTimestamp> =
    supabase.postgrest[tableName]
        .select { filter { eq("optica_id", opticaId); isIn("id", ids) } }
        .decodeList()

internal suspend fun fetchRemoteUpdatedAt(           // was private
    tableName: String, opticaId: String, ids: List<String>
): Map<String, String> { /* if ids empty → emptyMap(); else selectRemoteRows(...) */ }
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | RC-1 `isIn` filter + empty-ids short-circuit + no-extra-rows | Subclass `ConflictHelper`, override `selectRemoteRows`, capture `ids`; assert empty-ids makes NO call |
| Unit | RC-3 resolve on safe, not on conflict, idempotent when no record | MockK `ConflictDao`; `verify(exactly=1/0) { resolveConflict(...) }` |
| Unit | RC-2 no pacientes cascade | MockK `SyncPacientesUseCase`; run scheduler block; `verify(exactly=0) { syncPacientesUseCase(any()) }` |

All 8 methods RED before implementation (Strict TDD). MockK is the project's test
double library; `RobolectricTestRunner` already used in `ConflictHelperTest`.

## Migration / Rollout

No migration required. Trigger fix `20260615000000_...` already applied. Single PR
(~80 lines, well under the 400-line budget). Rollback = revert the merge commit;
no schema/data side effects. Stale records self-heal via RC-3 over sync cycles;
`acceptAllCloud()` remains the manual fallback.

## Open Questions

- [ ] Confirm `isIn` is the exact 3.6.0 method name during apply (vs `isIn`/`in_`);
      the seam isolates this so only `selectRemoteRows` changes if the name differs.
- [ ] `ConflictHelperTest` currently tests only the static `isLocalNewerOrEqual`;
      adding instance tests requires constructing `ConflictHelper(supabase, tracker, dao)` — verify `SyncStateTracker` is mockable or pass a relaxed MockK.
