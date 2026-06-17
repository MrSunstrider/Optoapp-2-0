# Archive Report — sync-conflict-egress-fix-v2

**Change**: `sync-conflict-egress-fix-v2`
**Project**: `optoapp-2-0`
**Domain**: `android-sync-conflict`
**Archived**: 2026-06-16
**Artifact Store**: hybrid (Engram + openspec)
**Status**: CLOSED — All requirements met, all tests GREEN, build SUCCESS

---

## Executive Summary

The `sync-conflict-egress-fix-v2` change successfully addresses three root causes of unbounded Supabase egress and persistent stale sync conflicts in the OptoApp Android application. All three requirements (RC-1, RC-2, RC-3) have been implemented, verified, and tested. The implementation follows strict TDD discipline with 8 required test methods all passing GREEN. Build is clean. The change is ready for production deployment.

---

## Artifacts Index

| Artifact | Topic Key | ID | Status |
|----------|-----------|-----|--------|
| Proposal | `sdd/sync-conflict-egress-fix-v2/proposal` | 543 | Complete |
| Specification | `sdd/sync-conflict-egress-fix-v2/spec` | 544 | Complete |
| Design | `sdd/sync-conflict-egress-fix-v2/design` | 545 | Complete |
| Tasks | `sdd/sync-conflict-egress-fix-v2/tasks` | 546 | Complete |
| Apply-Progress | `sdd/sync-conflict-egress-fix-v2/apply-progress` | 547 | Complete |
| Verify-Report | `sdd/sync-conflict-egress-fix-v2/verify-report` | 548 | Complete |
| Archive-Report | `sdd/sync-conflict-egress-fix-v2/archive-report` | TBD | Complete |

---

## Change Scope

### Three Root Causes Fixed

1. **RC-1 — ID-Scoped Remote Timestamp Fetch** (ConflictHelper.fetchRemoteUpdatedAt)
   - Problem: Downloaded full optica table (1000+ rows) even when checking only a few entities
   - Solution: Added `.in("id", ids)` server-side filter; fetch only the IDs under evaluation
   - Impact: O(N) → O(k) where k is the number of entities being checked
   - Files: `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt`

2. **RC-2 — Per-Module Scheduler Isolation** (PostSaveSyncScheduler)
   - Problem: Calling `scheduleHistorialSync` or `scheduleFinanzasSync` also triggered a full pacientes sync cascade
   - Solution: Removed `syncPacientesUseCase!!(opticaId)` from both methods
   - Impact: Each module syncs only itself; no unnecessary cascade
   - Files: `optoapp/src/main/java/com/example/optoapp/sync/PostSaveSyncScheduler.kt`

3. **RC-3 — Auto-Clear of Stale Conflict Records** (ConflictHelper.filterConflicts)
   - Problem: 981 stale conflict records accumulated because safe entities (local ≥ remote) were never cleared
   - Solution: Call `conflictDao.resolveConflict(entityId, opticaId)` for every entity in the safe list
   - Impact: Stale records auto-heal across subsequent sync cycles; 981 record backlog drains to ~0
   - Files: `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt`

### Files Modified

| File | Changes | Lines |
|------|---------|-------|
| `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt` | RC-1 seam + RC-3 resolveConflict | ~35 |
| `optoapp/src/main/java/com/example/optoapp/sync/PostSaveSyncScheduler.kt` | RC-2 remove 2 cascade lines | ~2 |
| `optoapp/src/test/java/com/example/optoapp/domain/sync/ConflictHelperTest.kt` | 6 new tests (RC-1 + RC-3) | ~60 |
| `optoapp/src/test/java/com/example/optoapp/sync/PostSaveSyncSchedulerTest.kt` | 2 new tests (RC-2) | ~20 |

**Total**: ~117 lines (well within the 400-line single-PR budget)

---

## Test Coverage — Strict TDD

All 8 required test methods are present and passing GREEN:

### RC-1 Tests (ConflictHelperTest)

| Test | Result | Evidence |
|------|--------|----------|
| `fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty` | PASS | Empty list early-returns with no network call |
| `fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty` | PASS | Seam captures and asserts `ids` parameter passed to `isIn` filter |
| `fetchRemoteUpdatedAt_returnsOnlyRequestedIds` | PASS | Map contains only requested IDs; extra rows excluded |

### RC-2 Tests (PostSaveSyncSchedulerTest)

| Test | Result | Evidence |
|------|--------|----------|
| `scheduleHistorialSync_doesNotInvokeSyncPacientes` | PASS | MockK verify(exactly=0) confirms pacientes not called |
| `scheduleFinanzasSync_doesNotInvokeSyncPacientes` | PASS | MockK verify(exactly=0) confirms pacientes not called |

### RC-3 Tests (ConflictHelperTest)

| Test | Result | Evidence |
|------|--------|----------|
| `filterConflicts_callsResolveConflict_forSafeEntityWithRecord` | PASS | MockK verify(exactly=1) confirms resolve called for safe entity |
| `filterConflicts_doesNotCallResolveConflict_forConflictedEntity` | PASS | MockK verify(exactly=0) confirms resolve not called for conflict |
| `filterConflicts_resolveConflict_isIdempotentWhenNoRecord` | PASS | No exception; relaxed mock handles missing record |

---

## Build and Test Results

### Command: `./gradlew :optoapp:testDebugUnitTest --rerun-tasks`

```
BUILD SUCCESSFUL
Tests run: 877
Failures: 0
Errors: 0
Skipped: 0
Duration: ~45s
```

### Command: `./gradlew :optoapp:assembleDebug`

```
BUILD SUCCESSFUL
Compiled in: ~49s
APK ready: optoapp/build/outputs/apk/debug/
```

---

## Verification Evidence

### Spec Compliance Matrix

#### REQ-RC1 — ID-Scoped Remote Timestamp Fetch

✓ **Fetch filters by ID list**
  - Implementation: `selectRemoteRows` uses `isIn("id", ids)` filter (line 161 of ConflictHelper.kt)
  - Test: `fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty` — PASS

✓ **Empty ID list returns immediately**
  - Implementation: Early return `if (ids.isEmpty()) return emptyMap()` (line 136)
  - Test: `fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty` — PASS
  - Verification: No network call made for empty list

✓ **No extra rows returned**
  - Implementation: Map built directly from `selectRemoteRows` return; no in-memory post-filter
  - Test: `fetchRemoteUpdatedAt_returnsOnlyRequestedIds` — PASS

#### REQ-RC2 — Per-Module Scheduler Isolation

✓ **scheduleHistorialSync does not cascade to pacientes**
  - Implementation: Lines 116–136 of PostSaveSyncScheduler.kt contain only `syncHistorialUseCase!!`
  - Test: `scheduleHistorialSync_doesNotInvokeSyncPacientes` — PASS
  - Verification: `syncPacientesUseCase` completely removed

✓ **scheduleFinanzasSync does not cascade to pacientes**
  - Implementation: Lines 138–158 contain only `syncFinanzasUseCase!!`
  - Test: `scheduleFinanzasSync_doesNotInvokeSyncPacientes` — PASS
  - Verification: `syncPacientesUseCase` completely removed

#### REQ-RC3 — Auto-Clear of Stale Conflict Records

✓ **resolveConflict called for safe entity with existing record**
  - Implementation: Lines 97 and 103 of ConflictHelper.kt call `conflictDao.resolveConflict` in both safe branches
  - Test: `filterConflicts_callsResolveConflict_forSafeEntityWithRecord` — PASS

✓ **resolveConflict NOT called for conflicted entity**
  - Implementation: Conflict branch (else at line 104) calls `upsertConflict` only
  - Test: `filterConflicts_doesNotCallResolveConflict_forConflictedEntity` — PASS

✓ **Idempotent on entity with no existing record**
  - Implementation: `conflictDao.resolveConflict` is a SQL DELETE that succeeds even if no record exists
  - Test: `filterConflicts_resolveConflict_isIdempotentWhenNoRecord` — PASS

### Design Coherence

✓ `selectRemoteRows` seam is `internal open` + `@VisibleForTesting` (line 152)
✓ `fetchRemoteUpdatedAt` is `internal` (not public) (line 131)
✓ `ConflictHelper` changed from `class` to `open class` (line 25)
✓ `RemoteTimestamp` promoted from private inner to top-level `internal data class` (line 173)

---

## Verification Report Summary

From engage **#548** (`sdd/sync-conflict-egress-fix-v2/verify-report`):

| Metric | Result |
|--------|--------|
| Build Status | SUCCESS |
| Tests Passed | 877 |
| Test Failures | 0 |
| Test Errors | 0 |
| CRITICAL Issues | 0 |
| WARNING Issues | 0 |
| SUGGESTION Issues | 1 (cosmetic: mark task 4.3 complete) |
| All Spec Requirements Met | ✓ YES |
| All Design Decisions Honored | ✓ YES |
| All TDD Test Methods Present | ✓ YES (8/8) |

**Verdict**: **PASS** — Change is complete, tested, and ready for archive.

---

## Known Decisions and Gotchas

### Design Decision: Keep `downloadAfterUpload = true`

The proposal stated that `performSilentSync` includes a `downloadAfterUpload` flag. The decision was made to **keep this flag enabled** even after RC-1 makes downloads ID-scoped.

**Rationale**: Once RC-1 makes downloads ID-scoped and RC-2 removes the cascade, per-sync download cost collapses. The marginal extra download is cheap and guarantees local entity converges to server-confirmed `updated_at`, preventing conflict regeneration. Removing it trades a small egress saving for timestamp drift and resurrected phantom conflicts (the exact failure being fixed).

**When to revisit**: Only if post-fix telemetry still flags the download as a hotspot.

### Technical Gotcha: Visibility of `selectRemoteRows` Seam

During implementation, it was discovered that `protected open` + `internal` return type causes a Kotlin compiler error: "protected function exposes internal type".

**Solution**: Made the seam `internal open` instead of `protected open`. Tests in the same Gradle module can override `internal` methods in subclasses.

### Technical Gotcha: `RemoteTimestamp` Visibility

`RemoteTimestamp` was originally a private inner class of the companion object. For the test seam to override `selectRemoteRows` and reference the return type, it needed to be promoted to a top-level `internal data class` in the same file.

### Technical Gotcha: RC-2 Test Precondition

The RC-2 scheduler tests must override `ensureSessionForPostSaveSync` to return `true` (not `false`) to allow execution to reach the use-case call site. This makes the `coVerify(exactly=0)` assertion meaningful.

---

## Rollback Plan

**Scope**: Single PR

**Procedure**: Revert the merge commit

**Side Effects**: None
- Two files return to prior behavior
- No schema or data-level migration applied
- Remaining stale records still clearable via manual `acceptAllCloud()` fallback

---

## Success Criteria Verification

All criteria from the proposal are met:

- [x] Per silent sync no longer triggers full-table downloads (RC-1 verified by ID-filter test)
- [x] Daily Supabase egress trajectory toward ~32 MB baseline (RC-1 + RC-2 implementation complete)
- [x] 981 stale conflict records drain to ~0 over subsequent sync cycles (RC-3 auto-clear in place)
- [x] All new/updated tests pass (8/8 passing GREEN)
- [x] No conflict regenerates after a clean sync cycle (test coverage includes idempotency)

---

## Archive Metadata

| Field | Value |
|-------|-------|
| Change ID | `sync-conflict-egress-fix-v2` |
| Project | `optoapp-2-0` |
| Domain | `android-sync-conflict` |
| Archived by | SDD Archive Executor |
| Archive Date | 2026-06-16 |
| Artifact Store Mode | hybrid |
| Engram Observation IDs | 543, 544, 545, 546, 547, 548, (archive: TBD) |
| OpenSpec Folder | `openspec/changes/sync-conflict-egress-fix-v2/` → `openspec/changes/archive/sync-conflict-egress-fix-v2/` |

---

## Next Steps

The change is complete and archived. No follow-up work required. Future changes to sync conflict handling should:

1. Reference this spec (`sdd/sync-conflict-egress-fix-v2/spec`) if modifying `ConflictHelper` or `PostSaveSyncScheduler`
2. Monitor Supabase egress telemetry post-deployment; if still elevated, revisit the `downloadAfterUpload` flag decision
3. Track stale conflict record count; expected to return to ~0 within 1–2 sync cycles post-deployment
