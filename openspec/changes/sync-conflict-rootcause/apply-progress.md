# Apply Progress: sync-conflict-rootcause — PR-A

**Change**: sync-conflict-rootcause
**Batch**: PR-A (RC-3 + RC-4)
**Mode**: Strict TDD
**Date**: 2026-06-15

---

## TDD Cycle Evidence

| Task | RED | GREEN | REFACTOR | Notes |
|------|-----|-------|----------|-------|
| TASK-A-1 | compile error (no syncEntityStateDao param, no deleteConflictedForOptica) | PASS after impl | N/A | |
| TASK-A-2 | compile error (same) | PASS after impl | N/A | |
| TASK-A-3 | compile error (same) | PASS after impl | N/A | |
| TASK-A-4 | compile error (no deleteConflictedForOptica on DAO) | PASS after impl | N/A | |
| TASK-A-5 | compile error (no deleteConflictedForOptica on DAO) | PASS after impl | N/A | |
| TASK-A-6 | — (impl task) | PASS (TASK-A-5 green) | N/A | |
| TASK-A-7 | — (refactor, no new tests) | existing tests still pass | N/A | |
| TASK-A-8 | — (impl task) | TASK-A-1/A-2/A-3 green | N/A | |
| TASK-A-9 | — (impl task) | TASK-A-4 green | N/A | |
| TASK-A-10 | — (gate) | 849 tests, 0 failures | N/A | Full suite clean |

---

## Completed Tasks

- [x] TASK-A-1: `resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict` — PASSED
- [x] TASK-A-2: `resolveKeepMine_writesServerTimestampToRoom` — PASSED
- [x] TASK-A-3: `resolveKeepMine_doesNotRegenerateConflictOnNextSync` — PASSED
- [x] TASK-A-4: `acceptAllCloud_clearsBothConflictRecordsAndSyncEntityState` — PASSED
- [x] TASK-A-5: `deleteConflictedForOptica_deletesOnlyConflictedRows` + `deleteConflictedForOptica_whenNoConflictedRows_doesNothing` — PASSED
- [x] TASK-A-6: Added `deleteConflictedForOptica(opticaId: String)` to `SyncEntityStateDao.kt`
- [x] TASK-A-7: Extracted `syncForEntityType(opticaId, entityType, skipUpload)` private suspend fun in `SyncViewModel.kt`; refactored `resolveAcceptTheirs` to use it
- [x] TASK-A-8: Rewrote `resolveKeepMine()` to call `syncForEntityType(..., skipUpload=false)` BEFORE `conflictDao.resolveConflict()`
- [x] TASK-A-9: Added `syncEntityStateDao.deleteConflictedForOptica(opticaId)` inside `acceptAllCloud()`; added `syncEntityStateDao: SyncEntityStateDao` constructor parameter
- [x] TASK-A-10: Full green gate — 849 tests, 0 failures, 0 errors

---

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `optoapp/src/main/java/com/example/optoapp/data/SyncEntityStateDao.kt` | Modified | Added `deleteConflictedForOptica(opticaId: String)` query |
| `optoapp/src/main/java/com/example/optoapp/viewmodel/SyncViewModel.kt` | Modified | Added `syncEntityStateDao` param; added `syncForEntityType`; rewrote `resolveKeepMine`; updated `resolveAcceptTheirs`; updated `acceptAllCloud` |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/SyncViewModelConflictResolutionTest.kt` | Created | 4 unit tests with MockK for RC-3/RC-4 ViewModel behavior |
| `optoapp/src/test/java/com/example/optoapp/data/SyncEntityStateDaoTest.kt` | Created | 2 Room in-memory integration tests for `deleteConflictedForOptica` |

---

## Deviations from Design

1. **SyncViewModel signature**: The design assumed `resolveKeepMine(conflictId: String)` but the actual code uses `resolveKeepMine(entity: ConflictRecord)`. Implementation follows the actual signature.

2. **Entity type strings**: The design's `syncForEntityType` used plural forms (`"pacientes"`, `"evaluaciones"`) but the actual entity types in `ConflictRecord.entityType` are singular (`"paciente"`, `"evaluacion"`, etc.), matching `resolveAcceptTheirs`. Implementation uses singular forms.

3. **No `SyncEntityState.status = 'conflicted'`**: The `SyncEntityState` entity only uses statuses `pending | synced | error | deleted`. The `deleteConflictedForOptica` query is added as specified (it is a valid SQL DELETE that matches zero rows today but is ready for when `'conflicted'` status is written in future). The DAO test still exercises isolation by status correctly.

4. **`resolveKeepMine` session check**: The original code had no `refreshSessionBeforeSync` call. The design didn't require it either. Implementation omits it (use cases handle auth internally), keeping the behavior consistent with the existing pattern.

5. **`acceptAllCloud` and `performFullDownload`**: The test stubs `ConnectivityManager.activeNetwork` to return null so `performFullDownload` short-circuits at the network check without crashing or calling use cases. This is intentional — the test only needs to verify the two DAO calls happen, not the download flow.

---

## Pending Tasks (PR-B and PR-C)

- [ ] TASK-B-1 through TASK-B-9: Timestamp correctness (RC-1 + RC-2)
- [ ] TASK-C-1 through TASK-C-8: Race fix + pagos guard (RC-5 + RC-6)

---

## Test Results Summary

```
Test run: ./gradlew :optoapp:testDebugUnitTest
Result: BUILD SUCCESSFUL
Tests: 849 total, 0 failures, 0 errors
New tests added: 6 (4 ViewModel + 2 DAO)
```
