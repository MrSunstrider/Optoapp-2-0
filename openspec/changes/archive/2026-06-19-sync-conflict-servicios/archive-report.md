# Archive Report: sync-conflict-servicios

**Change**: sync-conflict-servicios (sync-download-timestamp-integrity)
**Status**: Archived
**Date**: 2026-06-19
**Artifact Store**: hybrid (engram + openspec)

---

## Change Summary

Stopped sync churn loop in offline-first Android app by adding remote-path bypass methods to `OptoRepository` that preserve server-provided `updatedAt` timestamps and skip the post-save sync scheduler. Prevented false re-uploads of just-downloaded records for 4 entities: ServicioExtra, DispensacionOptica, Pago, EvaluacionClinica.

---

## Implementation Artifacts

All phases completed successfully. All 5 phases have passed verification with 0 CRITICAL issues.

| Phase | Artifact | Topic Key | Observation ID |
|-------|----------|-----------|-----------------|
| Exploration | (inline) | sdd/sync-conflict-servicios/explore | 604 |
| Proposal | proposal.md | sdd/sync-conflict-servicios/proposal | 605 |
| Specification | spec.md | sdd/sync-conflict-servicios/spec | 606 |
| Design | design.md | sdd/sync-conflict-servicios/design | 607 |
| Tasks | tasks.md | sdd/sync-conflict-servicios/tasks | 608 |
| Apply Progress | apply-progress.md (engram) | sdd/sync-conflict-servicios/apply-progress | 609 |
| Verification | verify-report.md (engram) | sdd/sync-conflict-servicios/verify-report | 610 |

---

## Code Changes

### Files Modified

| File | Changes | Lines |
|------|---------|-------|
| `optoapp/src/main/java/com/example/optoapp/data/OptoRepository.kt` | Added 4 bypass methods (upsertServicioFromRemote, upsertDispensacionFromRemote, upsertPagoFromRemote, upsertEvaluacionFromRemote) | ~20 |
| `optoapp/src/main/java/com/example/optoapp/domain/DownloadSyncCoordinator.kt` | Swapped 3 call sites to use bypass methods (ServicioExtra, DispensacionOptica, Pago) | ~3 |
| `optoapp/src/main/java/com/example/optoapp/domain/SyncHistorialUseCase.kt` | Swapped 1 call site to use bypass method (EvaluacionClinica) | ~1 |

### Files Created

| File | Purpose | Lines |
|------|---------|-------|
| `optoapp/src/test/java/com/example/optoapp/data/OptoRepositoryFromRemoteTest.kt` | MockK unit tests (10 tests): bypass timestamp preservation + scheduler isolation + regression on local-action paths | ~120 |
| `optoapp/src/test/java/com/example/optoapp/data/DownloadTimestampIntegrityTest.kt` | Room in-memory integration tests (5 tests): timestamp fidelity for all 4 entities + idempotency | ~80 |

**Total changed lines**: ~224 (within Low budget risk)

---

## Task Completion Status

All 5 implementation phases fully completed and verified:

- [x] Phase 1: Foundation — 4 bypass methods added to OptoRepository.kt (lines 163-173)
- [x] Phase 2: Wiring — 4 download-path call sites swapped in DownloadSyncCoordinator.kt and SyncHistorialUseCase.kt
- [x] Phase 3: Unit tests — OptoRepositoryFromRemoteTest.kt (10/10 GREEN) using MockK
- [x] Phase 4: Integration tests — DownloadTimestampIntegrityTest.kt (5/5 GREEN) using Room in-memory
- [x] Phase 5: Cleanup — Full suite BUILD SUCCESSFUL (1073 tests); DispensacionMergeHandler.mergeLocalDispensacionConflict() confirmed unchanged

**Note**: OpenSpec tasks.md contained stale unchecked checkboxes. Archive-time reconciliation applied: apply-progress and verify-report provide complete evidence that all tasks are implemented and passing. Stale checkboxes are now reconciled with completion proof.

---

## Test Evidence

| Suite | Tests | Status | Result |
|-------|-------|--------|--------|
| OptoRepositoryFromRemoteTest (unit) | 10 | PASS | 0 failures, 0 errors |
| DownloadTimestampIntegrityTest (integration) | 5 | PASS | 0 failures, 0 errors |
| Full test suite | 1073 | PASS | BUILD SUCCESSFUL in 1m 39s |

**Test Runner**: `./gradlew :optoapp:testDebugUnitTest --rerun-tasks --stacktrace`

---

## Specification Compliance

All requirements from the specification have been satisfied and verified:

| Requirement | Scenario | Evidence | Status |
|-------------|----------|----------|--------|
| Remote Write Path Preserves Server Timestamp | upsertServicioFromRemote preserves updatedAt | OptoRepositoryFromRemoteTest + DownloadTimestampIntegrityTest | PASS |
| Remote Write Path Preserves Server Timestamp | upsertDispensacionFromRemote preserves updatedAt | OptoRepositoryFromRemoteTest + DownloadTimestampIntegrityTest | PASS |
| Remote Write Path Preserves Server Timestamp | upsertPagoFromRemote preserves updatedAt | OptoRepositoryFromRemoteTest + DownloadTimestampIntegrityTest | PASS |
| Remote Write Path Preserves Server Timestamp | upsertEvaluacionFromRemote preserves updatedAt | OptoRepositoryFromRemoteTest + DownloadTimestampIntegrityTest | PASS |
| Remote Write Path Preserves Server Timestamp | Full download cycle timestamp fidelity | DownloadTimestampIntegrityTest (idempotency test) | PASS |
| Remote Write Path Does Not Schedule Upload | All 4 bypass methods do not trigger scheduler | OptoRepositoryFromRemoteTest (exactly=0 scheduler calls) | PASS |
| Local Write Path Stamps Timestamp and Schedules Upload | insertServicio + updateServicio regression | OptoRepositoryFromRemoteTest | PASS |
| Path Separation Is Explicit | DispensacionMergeHandler uses stamping update path | SyncFinanzasMerge.kt:54 (static verification) | PASS |
| Path Separation Is Explicit | Download caller uses bypass method | DownloadSyncCoordinator.kt (static verification) | PASS |

---

## Verification Report Summary

**Verdict**: PASS
**Date**: 2026-06-19
**Issues**: 0 CRITICAL, 0 WARNING, 0 SUGGESTION

All requirements verified. Sync churn prevention achieved. Timestamp fidelity guaranteed for all 4 entities.

---

## Archive Contents

The `openspec/changes/archive/2026-06-19-sync-conflict-servicios/` directory contains:

- proposal.md — Business intent and scope
- spec.md — Detailed requirements and scenarios
- design.md — Technical approach and interfaces
- tasks.md — Work breakdown with all phases
- verify-report.md — Verification results (if present in archive)
- archive-report.md — This document

---

## Rollback Information

No schema or data migration — revert is code-level only. To rollback:

```bash
# Revert the 3 modified files to pre-change state
git revert <commits-for-OptoRepository.kt>
git revert <commits-for-DownloadSyncCoordinator.kt>
git revert <commits-for-SyncHistorialUseCase.kt>

# Test files (if needed to remove)
git rm test/.../data/OptoRepositoryFromRemoteTest.kt
git rm test/.../data/DownloadTimestampIntegrityTest.kt
```

The previously deployed server trigger fix (migration 20260615) is independent and remains in place.

---

## SDD Cycle Complete

This change has been fully:
1. **Planned** (proposal → spec → design → tasks)
2. **Implemented** (5 phases of code + 15 tests)
3. **Verified** (strict TDD: 0 CRITICAL issues, all tests GREEN)
4. **Archived** (this report + moved to openspec archive folder)

Ready for the next change.
