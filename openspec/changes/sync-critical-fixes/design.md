# Design: Sync Critical Fixes

## Technical Approach

Four independent bugfixes across EvaluacionEntity nullability, DAO idempotency, upload transactional state, and ConflictHelper graceful degradation. All changes are client-side Room/Kotlin only — no Supabase migrations, no schema version bumps.

## Architecture Decisions

| # | Decision | Options | Rationale |
|---|----------|---------|-----------|
| D1 | Make EvaluacionClinica fields `String?` instead of adding a type converter | A) Type converter that maps NULL→""; B) Nullable types with Elvis at consumers | A hides the problem; B makes the null contract explicit. Room already handles nullable columns natively. |
| D2 | `OnConflictStrategy.REPLACE` over `ABORT` + try/catch | A) REPLACE on DAO; B) Keep ABORT with runCatching in repository | B is fragile — if the update branch also fails, exception propagates. A is declarative, simpler, and consistent with existing DAOs like MonturaMovimientoDao. |
| D3 | Per-entity `withTransaction { markSynced }` on upload; batch-level outside | A) Wrap ALL markSynced; B) Only per-entity | B matches download path pattern (line 77-79 of DownloadSyncCoordinator) where per-entity upsert+markSynced are atomic, and batch tracking is separate. |
| D4 | Return `emptyMap()` instead of throwing in `fetchRemoteUpdatedAt` | A) Return emptyMap; B) Return Result; C) Custom exception | A is minimal — `selectRemoteRows` already handles per-chunk failures gracefully. Callers (`filterConflicts`) treat empty map as "no remote data," equivalent to safe-to-proceed. |

## Data Flow

### Upload path (REQ-SYNC-003 fix)

```
Supabase upsert succeeds
        │
        ▼
database.withTransaction {
    syncStateTracker.markSynced(entityId)    ◄── NEW: atomic
}
        │
        ▼
(batch markSynced stays outside)             ◄── unchanged
```

### Conflict detection (REQ-SYNC-004 fix)

```
filterConflicts → fetchRemoteUpdatedAt
    │
    ├── selectRemoteRows (chunked, parallel)
    │       ├── chunk succeeds → merged
    │       └── chunk fails → logged, empty list
    │
    └── rows.isEmpty() → return emptyMap()   ◄── NEW: no throw
            │
            ▼
        safe.add(entity)                     ◄── no remote data = safe
```

## File Changes

**REQ-SYNC-001 (Nullable alignment)**: `EvaluacionEntity.kt` (~85 fields: `String = ""` → `String? = null`, Boolean → `Boolean?`, `List<String>` → `List<String>?`). Consumers: `EvaluacionMapping.kt`, `EvaluacionDiagnosticoHelper.kt`, `SyncHistorialDto.kt`, 5 UI components — add `?: ""` / `?.` safe-calls.

**REQ-SYNC-002 (DAO idempotency)**: 8 DAO methods across 7 files: `OrdenCompraDao`, `OrdenCompraItemDao`, `GastoOperativoDao`, `RegaloDispensacionDao`, `InventarioFisicoDao` (2 methods), `ProveedorDao`, `CategoriaMonturaDao`. `@Insert` → `@Insert(onConflict = REPLACE)`.

**REQ-SYNC-003 (Transactional markSynced)**: 9 files — `UploadSyncCoordinator`, `SyncPacientesUseCase`, `SyncHistorialUseCase`, `SyncInventarioUseCase`, `SyncProveedoresUseCase`, `SyncOrdenesCompraUseCase`, `SyncInventarioFisicoUseCase`, `SyncFinanzasMerge`. Wrap per-entity `markSynced` in `database.withTransaction { ... }`; batch-level stays outside.

**REQ-SYNC-004 (Graceful degradation)**: `ConflictHelper.kt` — remove `throw RuntimeException`, log error, return `emptyMap()`.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | EvaluacionClinica nullable construction + orEmpty fallback | JUnit 5 + kotlinx-coroutines-test. Create entity with null fields, verify `.toString()` doesn't crash. |
| Unit | DAO REPLACE idempotency | Room in-memory DB (as per existing test pattern). Insert same PK twice, verify no exception + last value wins. |
| Unit | ConflictHelper.fetchRemoteUpdatedAt returns empty on all-chunk failure | Mock `selectRemoteRowsChunk` to always throw. Verify no RuntimeException, empty map returned, error logged. |
| Unit | markSynced atomic via withTransaction on upload | Mock `SyncStateTracker`/database. Verify call order: entity markSynced inside transaction, batch markSynced after. |
| Integration | Full sync cycle with nullable evaluacion | In-memory Room + mock Supabase. Download evaluacion with NULL fields, verify Room insert succeeds, UI state has empty-string fallbacks. |

No Robolectric — pure JVM tests using Room's `inMemoryDatabaseBuilder` and `kotlinx-coroutines-test`.

## Migration / Rollout

No migration required. SQLite column nullability is backward-compatible — existing non-null data reads correctly through nullable Kotlin types. Room schema version unchanged.

## Open Questions

None.
