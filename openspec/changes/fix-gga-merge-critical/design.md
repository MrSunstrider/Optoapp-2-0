# Design: Fix GGA Merge Critical Issues

## Technical Approach

Eight isolated bugfixes across 8 files, all within existing classes. No new classes, schema migrations, or DI changes. Each fix is independently revertible. Three groups: already-applied (stashed → tested → restored), DAO chain fixes (test-first), and new behavioral fixes (test-first).

## Architecture Decisions

| # | Decision | Tradeoffs | Rationale |
|---|----------|-----------|-----------|
| 1 | `require()` over fallback | Crash instead of silent wrong tenant | Blank `opticaId` at upload is unrecoverable; fallback hid data corruption |
| 2 | Collect merges → apply after upsert | Extra list allocation vs inline merge call | Prevents orphaned local data when remote upsert fails — data integrity over memory |
| 3 | `Instant.parse()` with try/catch fallback | Catch-all masking bad timestamps vs crash | Existing parse failures are non-critical; falling back to existing record preserves the batch |
| 4 | Batch `markSynced` after individual marks | Reorder-only, no memory tradeoff | Batch mark was incorrectly claiming success before individual entities were tracked |
| 5 | `opticaId` filter in `reassignItemsDispensacion` | DAO signature change propagated through 4 call sites | Without tenant filter, merge moves items from ANY tenant — multi-tenant isolation violation |
| 6 | Same as #5 for `reassignRegalosDispensacion` | Identical pattern to #5 | Currently unfixed — same class of bug for regalos |
| 7 | `withTimeout(5min)` on mutex lock | Hard timeout vs indefinite hang; must handle `TimeoutCancellationException` | Currently zero timeout — a stuck module freezes ALL sync permanently |
| 8 | Exponential backoff + jitter in telemetry retries | Slightly more complex arithmetic vs linear 300/600/900ms | Follows NetworkRetryHelper pattern already in codebase; reduces thundering herd on transient failures |

## Key Sequence: Merge Ordering Fix (#2)

```
uploadDispensaciones()
  │
  ├─ reconcile with remote ─→ build uniqueRows
  │
  ├─ collect duplicate pairs → deferredMerges.add(canonical, duplicate)
  │
  ├─ upsert rows to Supabase ──── (may throw)
  │   │
  │   ├─ SUCCESS: deferredMerges.forEach → mergeLocalDispensacionConflict()
  │   ├─ THEN: individual markSynced per entity
  │   └─ FINALLY: batch markSynced
  │
  └─ FAILURE: deferredMerges discarded (local data preserved)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/UploadSyncCoordinator.kt` | Modify | Fixes 1-4: require() opticaId, deferred merge, Instant.parse(), markSynced order |
| `data/dispensacion/DispensacionItemDao.kt` | Modify | Fix 5: add `optica_id = :opticaId` to reassign query |
| `data/DispensacionRepository.kt` | Modify | Fix 5: propagate `opticaId` param to items DAO |
| `data/OptoRepository.kt` | Modify | Fixes 5-6: propagate `opticaId` to items and regalos reassign |
| `domain/SyncFinanzasMerge.kt` | Modify | Fixes 5-6: pass `opticaId` to both reassign calls (already in scope) |
| `data/regalodispensacion/RegaloDispensacionDao.kt` | Modify | Fix 6: add `optica_id = :opticaId` to reassign query |
| `domain/sync/SyncOrchestrator.kt` | Modify | Fix 7: wrap `mutex.withLock{}` in `withTimeout(300_000)` |
| `viewmodel/SyncDiagnosticsViewModel.kt` | Modify | Fix 8: exponential delay `(1000 * 2^attempt) + jitter` |

## Testing Strategy

| Layer | Fixes | Framework | Pattern |
|-------|-------|-----------|---------|
| DAO (Room) | 5, 6 | Robolectric + `Room.inMemoryDatabaseBuilder` | Insert items/regalos for two tenants → reassign with one opticaId → verify only that tenant moved |
| Coordinator | 1-4 | mockk | Mock all 5 constructor deps; verify `require()` throws, merge order, timestamp winner, markSynced call order |
| Orchestrator | 7 | mockk + `runTest` | Mock `syncGate` with real Mutex; trigger timeout via `testScheduler`; verify `TimeoutCancellationException` handled |
| ViewModel | 8 | mockk + `runTest` | Verify delay sequence is exponential; verify `CancellationException` propagates |

### TDD for Already-Applied Fixes (1-5)

1. `git stash` working tree changes
2. Write RED tests (fail because fixes not applied)
3. `git stash pop` → GREEN tests
4. Run full suite: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

### Test Locations

```
optoapp/src/test/java/com/example/optoapp/
├── data/dispensacion/DispensacionItemDaoTest.kt       (NEW)
├── data/regalodispensacion/RegaloDispensacionDaoTest.kt (MODIFY — add reassign test)
└── domain/
    ├── UploadSyncCoordinatorTest.kt                    (NEW)
    ├── sync/SyncOrchestratorTest.kt                    (NEW)
    └── viewmodel/SyncDiagnosticsViewModelRetryTest.kt  (NEW)
```

## Migration / Rollout

No migration required. All fixes are runtime behavior changes. Rollback: revert individual commits/files. SyncOrchestrator timeout is the only behavioral change at runtime — can be raised via config if 5min proves too tight.

## Open Questions

None — all decisions resolved in proposal.
