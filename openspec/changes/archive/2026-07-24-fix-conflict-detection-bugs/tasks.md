# Tasks: Fix Conflict Detection Bugs

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 150–210 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Tests First

- [x] 1.1 Write test `normalizeTimestamp` for microsecond + `+00:00` → `"....469Z"`
- [x] 1.2 Write test `normalizeTimestamp` for microsecond + `+0000` → truncate + replace with Z
- [x] 1.3 Write test `normalizeTimestamp` for millisecond + `Z` (no-op, idempotent)
- [x] 1.4 Write test `normalizeTimestamp` for no fractional seconds (`"....Z"` unchanged)
- [x] 1.5 Write test `parseInstant` for unparseable after normalization → fallback preserved (covered by existing tests)
- [x] 1.6 Write test `selectRemoteRows` chunking: 50 IDs → 1 query, 80 → 1, 200 → 3 chunks
- [x] 1.7 Write test `selectRemoteRows` single chunk failure → partial results merged, no crash
- [x] 1.8 Write test `fetchRemoteUpdatedAt` all chunks fail → throws exception

## Phase 2: GREEN — Implement Fixes

- [x] 2.1 Add `normalizeTimestamp()` companion function: truncate fractional to 3 digits, replace `+00:00`/`+0000` with `Z`
- [x] 2.2 Update `parseInstant()` to call `normalizeTimestamp()` before `Instant.parse()`, preserve fallback
- [x] 2.3 Chunk `selectRemoteRows()`: batch IDs in groups of 80, parallel via `coroutineScope { async {} }`, merge results
- [x] 2.4 Add total-failure guard in `fetchRemoteUpdatedAt`: throw when `ids.isNotEmpty()` but all chunks returned empty

## Phase 3: REFACTOR & Verify

- [x] 3.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests GREEN
- [x] 3.2 Verify `selectRemoteRows` with 240 IDs produces exactly 3 queries via captured chunk counter
- [x] 3.3 Verify `isLocalNewerOrEqual(..., ...)` with real timestamps from spec scenarios
