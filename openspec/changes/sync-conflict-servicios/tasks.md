# Tasks: Stop Sync Churn for Download-Path Entities

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~225 (20 new in OptoRepository, 4 call-site swaps, ~120 unit test, ~80 integration test) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR with work-unit commits |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All 5 files shipped together | PR 1 | Foundation → wiring → tests in one pass; ~225 lines total |

---

## Phase 1: Foundation — Add Bypass Methods to OptoRepository

- [ ] 1.1 Open `data/OptoRepository.kt`; locate the `upsertArqueoFromRemote` method near line 153 as the convention reference.
- [ ] 1.2 Add `suspend fun upsertServicioFromRemote(servicio: ServicioExtra)` delegating to `dispensacionRepo.insertServicio(servicio)` — no `copy(updatedAt = Instant.now())`, no scheduler call.
- [ ] 1.3 Add `suspend fun upsertDispensacionFromRemote(dispensacion: DispensacionOptica)` delegating to `dispensacionRepo.insertDispensacion(dispensacion)`.
- [ ] 1.4 Add `suspend fun upsertPagoFromRemote(pago: Pago)` delegating to `dispensacionRepo.insertPago(pago)`.
- [ ] 1.5 Add `suspend fun upsertEvaluacionFromRemote(evaluacion: EvaluacionClinica)` delegating to `pacienteRepo.insertEvaluacion(evaluacion)`.

## Phase 2: Wiring — Swap Call Sites on Download Paths

- [ ] 2.1 In `domain/DownloadSyncCoordinator.kt` line ~64 (`downloadDispensaciones`): replace `insertDispensacion(...)` call with `upsertDispensacionFromRemote(...)`.
- [ ] 2.2 In `domain/DownloadSyncCoordinator.kt` line ~88 (`downloadServicios`): replace `insertServicio(...)` call with `upsertServicioFromRemote(...)`.
- [ ] 2.3 In `domain/DownloadSyncCoordinator.kt` line ~112 (`downloadPagos`): replace `insertPago(...)` call with `upsertPagoFromRemote(...)`.
- [ ] 2.4 In `domain/SyncHistorialUseCase.kt` line ~187 (`downloadEvaluaciones`): replace `insertEvaluacion(...)` call with `upsertEvaluacionFromRemote(...)`.
- [ ] 2.5 Verify `downloadDispensacionItems` (L39) and `downloadArqueos` (L138) are NOT changed — confirm they remain on their existing paths.

## Phase 3: Unit Tests (RED → GREEN, MockK)

- [ ] 3.1 Create `test/.../data/OptoRepositoryFromRemoteTest.kt`; set up MockK mocks for `dispensacionRepo`, `pacienteRepo`, and `PostSaveSyncScheduler`.
- [ ] 3.2 Write test: `upsertServicioFromRemote` → asserts `dispensacionRepo.insertServicio` received entity with original `updatedAt`; verifies `scheduleServicioSync` called `exactly(0)` times.
- [ ] 3.3 Write test: `upsertDispensacionFromRemote` → same pattern for `dispensacionRepo.insertDispensacion`; scheduler not called.
- [ ] 3.4 Write test: `upsertPagoFromRemote` → same pattern for `dispensacionRepo.insertPago`; scheduler not called.
- [ ] 3.5 Write test: `upsertEvaluacionFromRemote` → same pattern for `pacienteRepo.insertEvaluacion`; scheduler not called.
- [ ] 3.6 Write regression test: `insertServicio` (user-action) still stamps `updatedAt` (non-null, >= before) AND scheduler called `exactly(1)` times.
- [ ] 3.7 Write regression test: `updateServicio` (user-action) same stamp + schedule assertion.
- [ ] 3.8 Run `./gradlew testDebugUnitTest` — all tests in the file must pass (GREEN).

## Phase 4: Integration Tests (Room In-Memory, androidTest)

- [ ] 4.1 Create `androidTest/.../data/DownloadTimestampIntegrityTest.kt`; configure Room in-memory database with all relevant DAOs.
- [ ] 4.2 Write integration test: call `upsertServicioFromRemote` with a fixed `updatedAt = T_remote`; read back via DAO; assert `stored.updatedAt == T_remote`.
- [ ] 4.3 Write integration test: same flow for `upsertDispensacionFromRemote`.
- [ ] 4.4 Write integration test: same flow for `upsertPagoFromRemote`.
- [ ] 4.5 Write integration test: same flow for `upsertEvaluacionFromRemote`.
- [ ] 4.6 Write integration test: re-download same record with identical `updatedAt`; assert no duplicate row (upsert by PK).
- [ ] 4.7 Run `./gradlew connectedAndroidTest` (emulator/device required) — all tests must pass.

## Phase 5: Cleanup

- [ ] 5.1 Confirm `DispensacionMergeHandler.mergeLocalDispensacionConflict` (SyncFinanzasMerge.kt:54) still calls `updateDispensacion()` — no accidental change.
- [ ] 5.2 Run `./gradlew lintDebug` — no new lint errors.
- [ ] 5.3 Run full unit test suite `./gradlew testDebugUnitTest` — no regressions.
