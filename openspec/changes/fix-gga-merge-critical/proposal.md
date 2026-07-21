# Proposal: Fix GGA Merge Critical Issues

## Intent

7 critical bugs survived a dual-judge GGA review of merged branches (`refactor-financiero` + `fix/judgment-day-sync-tracking`) on `main`. 4 are already fixed in the working tree (unstaged). Left unfixed they cause: **data corruption** (wrong `opticaId` in dispensacion reassign, timestamp parsing crash, sync order mismatch), **stuck sync** (no timeout in mutex lock), and **poor UX** (linear retry without backoff).

## Scope

### In Scope
1. **Multi-tenant isolation** — add `opticaId` filter to `reassignItemsDispensacion` (DispensacionItemDao) + `reassignRegalosDispensacion` (RegaloDispensacionDao); propagate param through OptoRepository + SyncFinanzasMerge
2. **UploadSyncCoordinator data integrity** — `opticaId` fallback removal (`require()`), deferred merge after remote upsert, `Instant.parse` for servicio timestamp dedup, `markSynced("batch")` after individual marks
3. **SyncOrchestrator lock timeout** — `withTimeout` on `syncGate.mutex.withLock`
4. **SyncDiagnosticsViewModel backoff** — exponential instead of linear (300/600/900ms → 1s/2s/4s + jitter)

### Out of Scope
- `FinanzasRemoteDefaults.OPTICA_ID_FALLBACK` in SyncFinanzasDto.kt (download direction, legitimate fallback)
- `SyncRepository` zero-error handling (single judge, theoretical)
- Naming/style improvements (single-letter vars, long methods)

## Capabilities

### New Capabilities
None — all fixes are bug corrections within existing capabilities.

### Modified Capabilities
None — no spec-level behavior changes. Fixes correct implementation to match existing requirements.

## Approach

TDD strict (RED→GREEN). 5 files with partial fixes applied use **Option A**: `git stash` → write failing tests → `git stash pop` → tests pass. 3 unfixed files write tests first (RED) then fix (GREEN).

| Group | Files | Strategy |
|-------|-------|----------|
| Already applied (5) | UploadSyncCoordinator, DispensacionItemDao, DispensacionRepository, OptoRepository (items), SyncFinanzasMerge | Stash → test → pop |
| Remaining (3) | RegaloDispensacionDao, OptoRepository (regalos), SyncFinanzasMerge (regalos) | Test-first (no stash) |
| New (2) | SyncOrchestrator, SyncDiagnosticsViewModel | Test-first with no existing coverage |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../data/local/dao/DispensacionItemDao.kt` | Modified | `opticaId` filter in reassign query |
| `optoapp/.../data/local/dao/RegaloDispensacionDao.kt` | Modified | `opticaId` filter in reassign query |
| `optoapp/.../data/repository/DispensacionRepository.kt` | Modified | Propagate `opticaId` to items DAO |
| `optoapp/.../data/repository/OptoRepository.kt` | Modified | Propagate `opticaId` — items AND regalos |
| `optoapp/.../sync/SyncFinanzasMerge.kt` | Modified | Pass `opticaId` to both reassign calls |
| `optoapp/.../sync/UploadSyncCoordinator.kt` | Modified | 4 data-integrity fixes |
| `optoapp/.../sync/SyncOrchestrator.kt` | Modified | `withTimeout` on mutex lock |
| `optoapp/.../ui/sync/SyncDiagnosticsViewModel.kt` | Modified | Exponential backoff |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `require()` on opticaId crashes on null | Low (already null in dev) | Dev fallback handling kept in download direction only |
| Tests flaky due to timing (timeout/backoff) | Med | Use `TestDispatcher` + configurable delay fns |

## Rollback Plan

Revert commit(s) for each file independently. All fixes are isolated — no cascading schema or migration changes. SyncOrchestrator timeout is the only behavioral change at runtime (safe: default timeout can be raised if too tight).

## Dependencies

None — no schema, migration, or external API changes.

## Success Criteria

- [ ] All 7 fixes have passing RED→GREEN tests (no untested changes)
- [ ] Multi-tenant reassign queries include `opticaId = :opticaId`
- [ ] UploadSyncCoordinator uses `require()` for opticaId, merges after upsert, parses timestamps safely, marks batch after individual
- [ ] SyncOrchestrator lock times out instead of hanging
- [ ] SyncDiagnosticsViewModel uses exponential delays (×2 + jitter)
