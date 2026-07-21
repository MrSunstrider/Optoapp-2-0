# Tasks: Fix Paciente Delete & Sync Integrity

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~280 (tests ~200, source ~15, misc ~65) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All 6 fixes + tests | PR 1 (single) | 280 lines, well under 400-line budget. All fixes independent but in same module. |

## Phase 1: Sync Layer — Resurrection, Cancellation, Double-Failure (F1, F4, F5)

Tests target `SyncPacientesUseCaseDownloadGuardTest.kt`. Each fix: RED (write failing test) → GREEN (implement).

- [x] 1.1a **F1 RED**: Write test for Phase 1 retry succeeds (happy path — tombstone cleared) + retry fails (tombstone preserved) + no tombstones (all inserted) — tests pass immediately since code exists (GREEN verification)
- [x] 1.2a **F4 RED**: Write test that CancellationException in Phase 1 inner loop propagates (not swallowed) — expect test to fail (RED) because catch is missing
- [x] 1.2b **F4 GREEN**: Add `catch (e: CancellationException) { throw e }` in SyncPacientesUseCase.kt inner try (line 189) + outer try (line 199) before `catch (e: IOException)`
- [x] 1.3a **F5 RED**: Write test for double-failure upload: `fetchAllRemotePacientes` returns empty AND `fetchRemoteUpdatedAt` fails — upload returns error, entities NOT silently synced
- [x] 1.3b **F5 GREEN**: In `SyncPacientesUseCase.upload()` post-`filterConflicts`, add guard: if `effectiveRemoteMap == null` AND `checkableCount > 0` AND all checkable entities passed, abort with error

## Phase 2: Data Layer — Duplicate Merge (F2)

Tests target `PacienteRepositoryTest.kt`.

- [x] 2.1a **F2 RED**: Write test: 3 pacientes with same HO merged via `resolveDuplicatePacientesByHistoria` — expect full field accumulation (email, telefono, direccion from 3 distinct records). `val canonical` causes last merge to overwrite — test should FAIL (RED)
- [x] 2.1b **F2 GREEN**: In `PacienteRepository.kt:174`, change `val canonical` → `var canonical`; reassign after each `mergePacienteData(canonical, duplicate)`
- [x] 2.1c **F2 REGRESSION**: Existing 2-duplicate test still passes (confirm no regression)

## Phase 3: ViewModel Layer — updatedAt Stamping & Authorization (F3, F6)

Tests target `PacienteRepositoryTest.kt` and `PacienteViewModelTest.kt`.

- [x] 3.1a **F3 RED**: Write test: new paciente via `savePaciente` stores non-null `updatedAt`; edit refreshes timestamp to newer value — if ViewModel bypasses stamping, test fails (RED)
- [x] 3.1b **F3 GREEN**: Ensure `PacienteViewModel.savePaciente` delegates to `OptoRepository.insertPaciente` (which stamps `updatedAt`); confirm both create and edit paths hit the stamping
- [x] 3.2a **F6 RED**: Write test: admin role → `savePaciente` succeeds; vendedor role → `IllegalArgumentException("Unauthorized")` thrown. Expect vendedor test to fail (RED, guard missing)
- [x] 3.2b **F6 GREEN**: Add `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` at top of `PacienteViewModel.savePaciente`

## Phase 4: Full Verification

- [x] 4.1 Run full test suite: `./gradlew :optoapp:testDebugUnitTest --stacktrace` — 1836 tests, 1 pre-existing failure (OptoDatabaseMigrationTest)
- [x] 4.2 Confirm JaCoCo coverage threshold: `./gradlew :optoapp:jacocoTestReport`

## Implementation Order

F1 first (tests only, no code change — easy win, establishes test patterns for the test file). Then F2→F3→F4→F5→F6 following the user's ordered RED→GREEN sequence. All fixes are independent — order within the sync layer (F1→F4→F5) and ViewModel layer (F3→F6) groups related test files to minimize context switching.

## Known Risks

- F1 tests are GREEN from the start (code exists) — this is expected, not a failure
- F5 test requires mocking two chained failures — ensure mockk mock clears between scenarios
