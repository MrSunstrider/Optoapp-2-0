# Verification Report: fix-paciente-delete-sync

## Change
**fix-paciente-delete-sync** — Six fixes for data corruption, sync integrity, and authorization gaps in the Pacientes module.

## Mode
Full verification: proposal → 2 delta specs → design → tasks (14/14 complete) → source → tests

## Completeness

| Dimension | Status | Evidence |
|-----------|--------|----------|
| Tasks completed | 14/14 ✅ | All tasks marked [x] in tasks.md |
| Spec coverage | 6 requirements, 14 scenarios | See compliance matrix |
| Design adherence | ✅ | Files changed match design.md exactly |
| Test execution | ✅ | 1836 total, 1 pre-existing failure, 6 skipped |

## Test Results

```
1836 tests completed, 1 failed, 6 skipped
```

**Only failure**: `OptoDatabaseMigrationTest > migrate 30 to current preserves all data` — pre-existing `SQLiteException` (unrelated to this change, documented in tasks.md §4.1).

### Test Class Results

| Test Class | Tests | Failures | Status |
|------------|-------|----------|--------|
| `SyncPacientesUseCaseDownloadGuardTest` | 10 | 0 | ✅ PASS |
| `PacienteRepositoryTest` | 14 | 0 | ✅ PASS |
| `PacienteViewModelTest` | 10 | 0 | ✅ PASS |

## Spec Compliance Matrix — sync-state-tracking

| # | Requirement | Scenarios | Coverage | Status |
|---|-------------|-----------|----------|--------|
| R1 | Download Phase 1 retries pending remote deletes (F1) | 2 | `download phase1 retry fails preserves tombstone` + `download phase1 retry succeeds clears entity state` | ✅ **COVERED** |
| R2 | Download Phase 2 guards re-insertion via skipIds (F1) | 2 | `download phase1 no tombstones does not skip` + code inspection (line 240-251: skipIds built, `return@forEach` in Phase 2 loop) | ✅ **COVERED** |
| R3 | savePaciente stamps updatedAt before upsert (F3) | 2 | Code: `OptoRepository.insertPaciente` stamps at line 57, `updatePaciente` at line 62. **No direct test** at OptoRepository level — `insertPaciente_preservesUpdatedAtAtRepoLevel` tests PacienteRepository (lower layer) asserting null (PacienteRepository is a pass-through, does NOT stamp). | ⚠️ **CODE OK, NO DIRECT TEST** |
| R4 | Download Phase 1 propagates CancellationException (F4) | 2 | `download phase1 cancellationException propagates from inner loop` + code lines 204-205, 216-217 | ✅ **COVERED** |
| R5 | savePaciente requires admin/gerente role (F6) | 2 | `savePaciente with admin role succeeds` + `savePaciente with vendedor role throws unauthorized` | ✅ **COVERED** |

## Spec Compliance Matrix — sync-conflict

| # | Requirement | Scenarios | Coverage | Status |
|---|-------------|-----------|----------|--------|
| F2 | 3+ duplicate merge accumulates all fields | 2 | `resolveDuplicatePacientesByHistoria_threeDuplicates_accumulatesAllFields` — verifies A→email, B→telefono, C→direccion preserved. Oldest ID survives. Duplicates deleted. | ✅ **COVERED** |
| F2 | 2 duplicates — no regression | 1 | No dedicated regression test found. The 3+ test inherently exercises 2 paths but no named 2-duplicate test. | ⚠️ **NO DEDICATED REGRESSION TEST** |
| F5 | Upload aborts on double fetch failure | 2 | `upload double batch and per-entity fetch failure marks error` — verifies `markError()` called. Code at lines 142-155 handles the guard. | ✅ **COVERED** |
| F5 | Normal batch fails, per-entity succeeds — proceeds | 1 | No dedicated test. Code at lines 83-93 sets `effectiveRemoteMap = null` for per-entity fallback. Path untested. | ⚠️ **CODE OK, NO DIRECT TEST** |

## Design Compliance

| Design Change | File | Line | Present | Evidence |
|---------------|------|------|---------|----------|
| F2: `val` → `var` canonical + reassign | `PacienteRepository.kt` | 174, 179 | ✅ | `var canonical = ...` + `canonical = mergedCanonical` |
| F4: CancellationException catch inner+outer | `SyncPacientesUseCase.kt` | 204-205, 216-217 | ✅ | `catch (e: CancellationException) { throw e }` before IOException in both blocks |
| F5: Double-failure guard post-filterConflicts | `SyncPacientesUseCase.kt` | 142-155 | ✅ | `if (effectiveRemoteMap == null && deduplicated.isNotEmpty() && ...)` |
| F6: AuthorizationGuard.requireRole | `PacienteViewModel.kt` | 159 | ✅ | `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` |
| F1/F4/F5 tests | `SyncPacientesUseCaseDownloadGuardTest.kt` | — | ✅ | 10 tests, all passing |
| F2/F3 tests | `PacienteRepositoryTest.kt` | — | ✅ | 14 tests, all passing |
| F6 tests | `PacienteViewModelTest.kt` | — | ✅ | 10 tests, all passing |

**No unexpected files modified** for this change scope (the 6 design-target files are the only changed files relevant to this fix).

## Success Criteria Verification

| Criterion | Status | Detail |
|-----------|--------|--------|
| F1: Pending-delete paciente NOT re-inserted after remote failure | ✅ | skipIds guard at line 240-251; test passes |
| F2: 3+ duplicates merge without data loss | ✅ | Test verifies A→email, B→telefono, C→direccion all preserved |
| F3: @Upsert stores non-null updatedAt | ⚠️ | Code stamps at OptoRepository level. Test at PacienteRepository level confirms lower layer is pass-through (asserts null). No OptoRepository-level test for stamping. |
| F4: CancellationException propagates | ✅ | Test verifies rethrow; code has both inner and outer catches |
| F5: Double failure abort upload | ✅ | Test verifies markError called; code guard present |
| F6: savePaciente blocks unauthorized roles | ✅ | Admin succeeds, vendedor throws IllegalArgumentException |
| All tests pass | ✅ | 1836 tests, 1 pre-existing failure (migration), 6 skipped |

## Issues

### WARNING
1. **F3 — No direct OptoRepository-level test for updatedAt stamping**: `OptoRepository.insertPaciente` stamps `updatedAt` correctly (line 57), and `updatePaciente` stamps (line 62). The test `insertPaciente_preservesUpdatedAtAtRepoLevel` tests `PacienteRepository` (lower pass-through layer) and asserts null. There is no test proving `OptoRepository` stamps `updatedAt` non-null, nor a test proving edit refreshes the timestamp. **Code correct, but lack of direct test coverage for the spec scenario.**

2. **F2 — No dedicated 2-duplicate regression test**: Tasks.md task 2.1c says "Existing 2-duplicate test still passes" but no test by that name exists. The 3+ test exercises the same code path, but there is no named regression test.

3. **F5 — Per-entity fallback success path untested**: The scenario "normal batch fails, per-entity succeeds" has no dedicated test. Code path exists but no covering test.

### SUGGESTION
- Add a dedicated test for F3 at the `OptoRepository` level (or integration test via `ViewModel` → `OptoRepository` path) to directly prove `updatedAt` is stamped non-null on save and refreshed on edit.

## Final Verdict

**PASS WITH WARNINGS**

All 6 fixes are correctly implemented. All tests pass (1 pre-existing failure only). 14/14 tasks complete. Design matches implementation. Two spec scenarios lack direct test coverage (F3 stamping, F5 per-entity fallback) and one regression concern is undocumented (F2 2-duplicate test), but the code is correct in all cases.
