# Tasks: Sync Conflict Root-Cause Fix

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 280–380 (spread across 3 PRs) |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR-A → PR-B → PR-C (stacked-to-main) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| PR-A | Durable conflict resolutions (RC-3 + RC-4) | PR 1 | Base: main; independent of B/C |
| PR-B | Timestamp correctness (RC-1 + RC-2) | PR 2 | Base: main; independent of A/C |
| PR-C | Hardening — race fix + pagos guard (RC-5 + RC-6) | PR 3 | Base: main; independent of A/B |

---

## PR-A — Durable Conflict Resolutions (RC-3 + RC-4)

- [x] TASK-A-1: Write failing test `resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict` in `SyncViewModelConflictResolutionTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelConflictResolutionTest.resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict"` — RED then GREEN

- [x] TASK-A-2: Write failing test `resolveKeepMine_writesServerTimestampToRoom` in `SyncViewModelConflictResolutionTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelConflictResolutionTest.resolveKeepMine_writesServerTimestampToRoom"` — RED then GREEN

- [x] TASK-A-3: Write failing test `resolveKeepMine_doesNotRegenerateConflictOnNextSync` in `SyncViewModelConflictResolutionTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelConflictResolutionTest.resolveKeepMine_doesNotRegenerateConflictOnNextSync"` — RED then GREEN

- [x] TASK-A-4: Write failing test `acceptAllCloud_clearsBothConflictRecordsAndSyncEntityState` in `SyncViewModelConflictResolutionTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelConflictResolutionTest.acceptAllCloud_clearsBothConflictRecordsAndSyncEntityState"` — RED then GREEN

- [x] TASK-A-5: Write failing integration test `deleteConflictedForOptica_deletesOnlyConflictedRows` in `SyncEntityStateDaoTest.kt` (Room in-memory)
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncEntityStateDaoTest.deleteConflictedForOptica_deletesOnlyConflictedRows"` — RED then GREEN

- [x] TASK-A-6: Add `deleteConflictedForOptica(opticaId: String)` to `SyncEntityStateDao.kt` — `@Query("DELETE FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'conflicted'")`
  Type: impl
  Depends: TASK-A-5
  Verify: TASK-A-5 turns GREEN

- [x] TASK-A-7: Extract private `syncForEntityType(opticaId, entityType, skipUpload)` dispatcher in `SyncViewModel.kt` — refactor from existing `resolveAcceptTheirs` when-block
  Type: refactor
  Depends: TASK-A-1, TASK-A-2, TASK-A-3
  Verify: existing tests still pass; no behavior change yet

- [x] TASK-A-8: Implement RC-3 — rewrite `resolveKeepMine()` in `SyncViewModel.kt` to call `syncForEntityType(entityType, opticaId, skipUpload=false)` before `resolveConflict()`
  Type: impl
  Depends: TASK-A-7
  Verify: TASK-A-1, TASK-A-2, TASK-A-3 turn GREEN

- [x] TASK-A-9: Implement RC-4 — add `syncEntityStateDao.deleteConflictedForOptica(opticaId)` call inside `acceptAllCloud()` in `SyncViewModel.kt`, same transaction as `clearConflicts()`
  Type: impl
  Depends: TASK-A-6
  Verify: TASK-A-4 and TASK-A-5 turn GREEN

- [x] TASK-A-10: Full PR-A green gate
  Type: impl
  Depends: TASK-A-8, TASK-A-9
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelConflictResolutionTest" "*SyncEntityStateDaoTest"` — all GREEN ✓ (849 tests, 0 failures)

---

## PR-B — Timestamp Correctness (RC-1 + RC-2)

- [ ] TASK-B-1: Write failing test `toRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull` in `SyncDtoTimestampTest.kt` — covers mappers in `SyncPacientesUseCase`, `SyncHistorialDto`, `SyncFinanzasDto`, `SyncInventarioUseCase`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncDtoTimestampTest.toRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull"` — RED

- [ ] TASK-B-2: Write failing test `toRemoto_preservesUpdatedAt_acrossMultipleCalls` in `SyncDtoTimestampTest.kt` — same value returned 3x for same entity state
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncDtoTimestampTest.toRemoto_preservesUpdatedAt_acrossMultipleCalls"` — RED

- [ ] TASK-B-3: Write failing test `silentSync_nullUpdatedAt_stableAcross3Cycles` in `SyncViewModelSilentSyncTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelSilentSyncTest.silentSync_nullUpdatedAt_stableAcross3Cycles"` — RED

- [ ] TASK-B-4: Write failing test `silentSync_writesServerTimestampToRoom_afterUpload` in `SyncViewModelSilentSyncTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncViewModelSilentSyncTest.silentSync_writesServerTimestampToRoom_afterUpload"` — RED

- [ ] TASK-B-5: Write failing integration test `uploadDownloadRoundtrip_timestampStable` in `SyncIntegrationTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncIntegrationTest.uploadDownloadRoundtrip_timestampStable"` — RED

- [ ] TASK-B-6: RC-1 — Remove `?: Instant.now()` fallback from `toRemoto()` in `SyncPacientesUseCase.kt:254`, `SyncHistorialDto.kt:275`, `SyncFinanzasDto.kt:198/211/238`, `SyncInventarioUseCase.kt:268`; add require/throw on null
  Type: impl
  Depends: TASK-B-1, TASK-B-2
  Verify: TASK-B-1, TASK-B-2 turn GREEN

- [ ] TASK-B-7: RC-1 — Audit and stamp `updatedAt = Instant.now().toString()` at every Room write call site (insert/update in Repos/ViewModels); grep `insertOrReplace\|update` DAO callers across all modules
  Type: impl
  Depends: TASK-B-6
  Verify: no null `updatedAt` reaches `toRemoto()` in any test path

- [ ] TASK-B-8: RC-2 — Change `performSilentSync` in `SyncViewModel.kt` to pass `downloadAfterUpload = true`
  Type: impl
  Depends: TASK-B-3, TASK-B-4
  Verify: TASK-B-3, TASK-B-4 turn GREEN

- [ ] TASK-B-9: Full PR-B green gate
  Type: impl
  Depends: TASK-B-6, TASK-B-7, TASK-B-8
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncDtoTimestampTest" "*SyncViewModelSilentSyncTest" "*SyncIntegrationTest"` — all GREEN

---

## PR-C — Hardening: Race Fix + Pagos Guard (RC-5 + RC-6)

- [ ] TASK-C-1: Write failing test `cancelPending_isSuspend_completesBeforeFullDownload` in `PostSaveSyncSchedulerTest.kt` — TestCoroutineScheduler, assert sequential order
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*PostSaveSyncSchedulerTest.cancelPending_isSuspend_completesBeforeFullDownload"` — RED

- [ ] TASK-C-2: Write failing test `uploadPagos_callsFilterConflicts_beforePush` in `UploadSyncCoordinatorPagosTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*UploadSyncCoordinatorPagosTest.uploadPagos_callsFilterConflicts_beforePush"` — RED

- [ ] TASK-C-3: Write failing test `uploadPagos_excludesConflictedPago_fromSupabasePush` in `UploadSyncCoordinatorPagosTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*UploadSyncCoordinatorPagosTest.uploadPagos_excludesConflictedPago_fromSupabasePush"` — RED

- [ ] TASK-C-4: Write failing test `uploadPagos_doesNotCallUpsert_forConflictedPago` in `UploadSyncCoordinatorPagosTest.kt`
  Type: test
  Depends: none
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*UploadSyncCoordinatorPagosTest.uploadPagos_doesNotCallUpsert_forConflictedPago"` — RED

- [ ] TASK-C-5: RC-5 — Change `cancelPending()` to `suspend fun cancelPending()` with `withContext(Dispatchers.IO) { jobs.forEach { it.cancelAndJoin() } }` in `PostSaveSyncScheduler.kt:65-70`
  Type: impl
  Depends: TASK-C-1
  Verify: TASK-C-1 turns GREEN

- [ ] TASK-C-6: RC-5 — Audit all `cancelPending()` call sites; ensure each caller is inside a coroutine scope; update fire-and-forget patterns if found
  Type: impl
  Depends: TASK-C-5
  Verify: compile passes; no GlobalScope or detached-launch pattern for `cancelPending`

- [ ] TASK-C-7: RC-6 — Add `filterConflicts("pagos", pagos)` call before `supabaseRepo.upsertPagos(...)` in `uploadPagos()` inside `UploadSyncCoordinator.kt:259-289`, mirroring `uploadDispensaciones`
  Type: impl
  Depends: TASK-C-2, TASK-C-3, TASK-C-4
  Verify: TASK-C-2, TASK-C-3, TASK-C-4 turn GREEN

- [ ] TASK-C-8: Full PR-C green gate
  Type: impl
  Depends: TASK-C-5, TASK-C-6, TASK-C-7
  Verify: `./gradlew :optoapp:testDebugUnitTest --tests "*PostSaveSyncSchedulerTest" "*UploadSyncCoordinatorPagosTest"` — all GREEN
