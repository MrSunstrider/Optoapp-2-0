# Design: Fix Conflict Detection Bugs

## Technical Approach

Two bugs in `ConflictHelper.kt` silently disable conflict detection for >160 entities. Bug 1: `selectRemoteRows()` sends all IDs in one `isIn()` query — URL exceeds Supabase's 8000-char limit (HTTP 414). Bug 2: `parseInstant()` fails on microsecond timestamps with `+00:00` offsets on Android desugaring. Fix Bug 1 by chunking IDs and querying in parallel; fixing Bug 1 exposes Bug 2, so both ship together. A third issue (silent failure when ALL queries fail) is addressed by throwing from `fetchRemoteUpdatedAt` on total failure.

## Sync Flow (Before → After)

```
BEFORE (broken):                              AFTER (fixed):
                                              
selectRemoteRows(200 IDs)                     selectRemoteRows(200 IDs)
  │                                             │
  └─ isIn("id", [200 ids])  ← URL > 8000       ├─ async → chunk[0..79]    ✓
     │                                           ├─ async → chunk[80..159]  ✓
     └─ HTTP 414 → emptyList()                  └─ async → chunk[160..199]  ✓
        │                                           │
        └─ ALL entities "safe" (silent!)             └─ merged → 200 timestamps
                                                        │
parseInstant("...469712+00:00")                       parseInstant(...)
  │                                                     │
  └─ Instant.parse() throws → fallback string cmp        └─ normalizeTimestamp() →
                                                              "....469Z" → Instant.parse() ✓
```

## Architecture Decisions

| Decision | Options | Choice & Rationale |
|----------|---------|-------------------|
| **Batch size** | 50 / 80 / 100 | **80**. UUID=36 chars → `80×36=2880` chars + encoding ~3500. Well under 8000. Leaves headroom for `optica_id` filter and base URL. 100 risks edge cases with very long UUIDs or additional query params. |
| **Concurrency model** | Sequential chunks / `async`+`awaitAll` / `channel` | **`coroutineScope { ids.chunked(80).map { async { ... } }.awaitAll() }`**. Already in coroutine context (suspend function). Parallel reduces latency from 3× to ~1×. Structured concurrency ensures cancellation propagation. |
| **Per-chunk error handling** | Fail-fast / per-chunk try-catch | **Per-chunk try-catch with warning log**. Partial results are better than nothing — losing 1 chunk of 3 still lets 160 entities be checked. Each chunk wraps its own `selectRemoteRowsChunk` in try-catch and returns `emptyList()` on failure. |
| **Total failure detection** | Sentinel value / exception / keep current | **Throw exception when ALL chunks fail**. `fetchRemoteUpdatedAt` currently catches everything → `emptyMap()` → all entities "safe" (dangerous). After chunking, if `ids.isNotEmpty()` but all chunks returned empty, throw. Callers already handle exceptions from `filterConflicts`. |
| **Timestamp normalization** | Regex / manual string ops / library | **Manual string ops** (find fractional dot + truncate, `replace("+00:00","Z")` then `replace("+0000","Z")`). Regex adds dependency overhead for a predictable format. `java.time` cannot be configured to accept these patterns on Android desugaring. |
| **Normalization scope** | Inline in `parseInstant` / extracted function | **Extracted `normalizeTimestamp(ts: String): String`** as a `companion` private pure function. Testable in isolation without mocking. Call from `parseInstant` before attempting `Instant.parse()`. |

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../domain/sync/ConflictHelper.kt` | Modify | Add `normalizeTimestamp()`. Replace `parseInstant` to use it. Chunk `selectRemoteRows` IDs. Add total-failure throw in `fetchRemoteUpdatedAt`. |
| `optoapp/.../domain/sync/ConflictHelperTest.kt` | Modify | Add tests: chunking produces correct number of queries, normalization edge cases (micros+offset), all-chunks-fail throws. |
| `optoapp/.../domain/sync/ConflictHelperSnapshotTest.kt` | No changes | `TestConflictHelper` overrides `fetchRemoteUpdatedAt` entirely — unaffected by chunking. |
| `optoapp/.../domain/sync/ConflictHelperMovimientoPersistenceTest.kt` | No changes | Uses `fetchRemoteMovimientos` seam, not timestamps. |

## Normalization Algorithm

```
Input:  "2026-07-25T02:32:19.469712+00:00"
Step 1: Find '.' → extract base before dot + first 3 digits after → "2026-07-25T02:32:19.469"
Step 2: Replace "+00:00" → "Z" → "2026-07-25T02:32:19.469Z"
Step 3: Replace "+0000" → "Z" (no-op here, already handled)
Result: "2026-07-25T02:32:19.469Z" → Instant.parse() succeeds

Idempotent: "2026-07-25T02:32:19.469Z" → step 1: "2026-07-25T02:32:19.469Z" (3 digits, no truncation needed) → step 2/3: no match → unchanged.
```

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit — `ConflictHelperTest` | `normalizeTimestamp` edge cases: micros+Z, micros+`+00:00`, micros+`+0000`, no-fractional, already-normalized, unparseable fallback | Companion object function, call directly |
| Unit — `ConflictHelperTest` | Chunking: 50 IDs → 1 chunk, 200 IDs → 3 chunks, 80 IDs → 1 chunk | Override internal `selectRemoteRowsChunk` seam (extracted single-query method); `capturedChunks` counter in fake |
| Unit — `ConflictHelperTest` | Total failure: `fetchRemoteUpdatedAt` with all-chunks-fail → throws | Fake `selectRemoteRows` returns empty, verify exception |
| Integration — existing | `filterConflicts` full flow, snapshot persistence, stale-conflict cleanup | Existing `FakeConflictHelper` unchanged |

## Open Questions

None — all decisions resolved. Batch size (80) verified against UUID length, concurrency pattern is project-standard `coroutineScope`+`async`, normalization is pure string manipulation with no dependencies.
