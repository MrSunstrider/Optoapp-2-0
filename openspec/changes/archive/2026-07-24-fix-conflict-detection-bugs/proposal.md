# Proposal: Fix Conflict Detection Bugs

## Intent

Two bugs in `ConflictHelper.kt` silently disable conflict detection. (1) `selectRemoteRows()` sends ALL entity IDs in a single `isIn()` query — with 160+ UUIDs (36 chars each), the URL exceeds Supabase/Kong's 8000-char limit, the query fails, `fetchRemoteUpdatedAt` returns `emptyMap()`, and ALL entities pass as "safe". (2) `parseInstant()` fails on timestamps with microsecond precision (e.g., `2026-07-25T02:32:19.469712+00:00`) on Android core library desugaring, falling back to fragile string comparison. Fixing (1) exposes (2), so both must ship together.

## Scope

### In Scope
- Batch `selectRemoteRows()`: split IDs into chunks of 80, query in parallel via `coroutineScope { async {} }`
- Normalize timestamps: truncate fractional seconds to 3 digits, replace `+00:00`/`+0000` with `Z`
- Update `ConflictHelperTest`, `ConflictHelperSnapshotTest`

### Out of Scope
- Adding `updatedAt` to entity types that lack it (e.g., `inventario_fisico`)
- Supabase schema or RLS changes
- Three-way merge or conflict UI (covered by `sync-conflict` phase)

## Capabilities

### New Capabilities
None

### Modified Capabilities
- `sync-conflict`: `ConflictHelper.filterConflicts()` MUST handle >160 entities without silent URL failure; `isLocalNewerOrEqual()` MUST parse timestamps with microsecond precision and non-Z offsets on Android desugaring

## Approach

1. **Batching**: Replace `isIn("id", ids)` with `ids.chunked(80)`. Launch concurrent queries via `coroutineScope { chunks.map { async { queryChunk(it) } }.awaitAll().flatten() }`.
2. **Timestamp normalization**: Extract `normalizeTimestamp(ts: String): String` — truncate fractional seconds to 3 chars, replace `+00:00`/`+0000` with `Z`. Pass to `Instant.parse()`. Keep `LocalDateTime.parse` + string comparison as fallback.
3. **Tests**: Override `selectRemoteRows` to verify chunking. Add `isLocalNewerOrEqual` tests for microsecond + offset combos.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ConflictHelper.kt` | Modified | Batching in `selectRemoteRows`, normalization in `parseInstant`, new `normalizeTimestamp()` |
| `ConflictHelperTest.kt` | Modified | Tests for URL failure, microsecond/offset timestamps |
| `ConflictHelperSnapshotTest.kt` | Modified | Mock expectations if single-query assumed |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Chunked parallel queries increase Supabase request count | Low | 80/chunk → max 3 concurrent queries for 240 entities |
| Exotic offset format not covered | Low | Fallback to `LocalDateTime.parse` + string comparison preserved |

## Rollback Plan

Revert changes to `ConflictHelper.kt` and test files. This restores pre-existing behavior (failure on >160 entities is the current broken state). No data migration or schema rollback needed.

## Dependencies

None.

## Success Criteria

- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes with new + existing tests
- [ ] `isLocalNewerOrEqual("2026-07-25T02:32:19.469712+00:00", "2026-07-25T02:32:19Z")` returns true
- [ ] `selectRemoteRows` with 200 IDs executes exactly 3 queries (chunks of 80)
- [ ] No Supabase schema or RLS affected (confirmed: client-side only)
