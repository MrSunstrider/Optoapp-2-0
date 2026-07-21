# Tasks: Fix GGA Merge Critical Issues

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~400–600 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (8 small isolated fixes) |
| Delivery strategy | ask-on-risk |
| Chain strategy | size-exception |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

Each fix is 2–30 lines; tests are the bulk (~50–100 lines each). 13 files total (8 prod, 5 test). All fixes are independent, atomic, and revertible — no schema changes. A single PR with `size:exception` is appropriate given the explicit user demand and pre-structured sequence.

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All 8 fixes + tests | PR 1 | Single PR with size-exception; every fix independently revertible |

## Phase 0: Setup (1 task)

- [x] 0.1 Run `git stash` to save working tree (5 already-applied fixes)

## Phase 1: Fix 5+6 — reassignItems + reassignRegalos opticaId (TDD RED→GREEN)

- [x] 1.1 **RED** — Write `DispensacionItemDaoTest`: insert items for two opticaIds → `reassignItemsDispensacion` with opticaId filter → assert only matching tenant rows moved
- [x] 1.2 **RED** — Write `RegaloDispensacionDaoTest` reassign test: same two-tenant pattern for regalos DAO
- [x] 1.3 **GREEN** — Apply Fix 5: add `optica_id = :opticaId` to `DispensacionItemDao.reassignItemsDispensacion` query; propagate param through `DispensacionRepository`, `OptoRepository`, `SyncFinanzasMerge`
- [x] 1.4 **GREEN** — Apply Fix 6: add `optica_id = :opticaId` to `RegaloDispensacionDao.reassignRegalosDispensacion` query; propagate through `OptoRepository`, `SyncFinanzasMerge`
- [x] 1.5 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests pass

## Phase 2: Fix 1-4 — UploadSyncCoordinator (stash→pop TDD)

- [x] 2.1 **RED** — Write `UploadSyncCoordinatorTest`: `require()` throws on blank opticaId (no silent fallback)
- [x] 2.2 **RED** — Write `UploadSyncCoordinatorTest`: deferred merge does NOT run when remote upsert throws; verify `mergeLocalDispensacionConflict` NOT called on `IOException`
- [x] 2.3 **RED** — Write `UploadSyncCoordinatorTest`: servicio dedup picks later `Instant` (string `"2025-01-01T10:00:00.500Z"` beats `"2025-01-01T10:00:00Z"`); unparseable timestamp falls back to existing record
- [x] 2.4 **RED** — Write `UploadSyncCoordinatorTest`: batch `markSynced` is NOT called when individual mark fails (entity 2 of 3 fails)
- [x] 2.5 **GREEN** — Apply Fixes 1-4 from stash to working tree → all 4 coordinator tests pass
- [x] 2.6 Run full test suite — all tests pass

## Phase 3: Fix 7 — SyncOrchestrator timeout (TDD RED→GREEN)

- [x] 3.1 **RED** — Write `SyncOrchestratorTest`: mock `syncGate` with real Mutex; `runTest` + configurable timeout; verify timeout is handled and lock releases
- [x] 3.2 **GREEN** — Apply Fix 7: wrap `syncGate.mutex.withLock{}` in `withTimeout(300_000)` with `TimeoutCancellationException` catch
- [x] 3.3 Run full test suite — all tests pass

## Phase 4: Fix 8 — SyncDiagnosticsViewModel backoff (TDD RED→GREEN)

- [x] 4.1 **RED** — Write `SyncDiagnosticsViewModelRetryTest`: verify delays follow ≈1s, ≈2s, ≈4s (×2 + jitter); verify stops after 5 attempts; verify `CancellationException` propagates
- [x] 4.2 **GREEN** — Apply Fix 8: replace linear delay with `(1000L * (1 shl attempt)) + jitter` capped at 5 retries
- [x] 4.3 Run full test suite — all tests pass

## Phase 5: Verify

- [x] 5.1 Full test suite: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [x] 5.2 Debug build: `./gradlew :optoapp:assembleDebug`
