# Tasks: Fix Persistent servicios_extra Sync Conflicts

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 220–300 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (all groups sequential, focused scope) |
| Delivery strategy | auto-chain / stacked-to-main |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation + download guard (Groups A infra + test/impl) | PR 1 | ConflictDao query, DownloadSyncCoordinator injection & guard |
| 2 | Keep-mine fix + bulk action + UI (Groups B & C) | PR 2 | Depends on PR 1 (ConflictDao already injectable) |

---

## Phase 1: Foundation — ConflictDao & Repository Gap

- [ ] 1.1 Add `@Query("SELECT entityId FROM conflict_records WHERE opticaId = :opticaId AND entityType = :entityType") suspend fun getConflictEntityIds(opticaId: String, entityType: String): List<String>` to `ConflictDao` in `src/main/java/com/example/optoapp/data/ConflictRecord.kt`
- [ ] 1.2 Add `suspend fun updatePago(pago: Pago)` to `OptoRepository` in `src/main/java/com/example/optoapp/data/OptoRepository.kt`, delegating to `pagoDao.updatePago(pago)` and mirroring the `updateServicio` pattern (no manual timestamp — PagoDao already has `updatePago`)

---

## Phase 2: Tests (RED) — Download Guard

- [ ] 2.1 **[RED]** Create `src/test/java/com/example/optoapp/domain/DownloadSyncCoordinatorConflictGuardTest.kt`; write failing tests:
  - `downloadServicios skips entity whose ID is in getConflictEntityIds`
  - `downloadServicios writes non-conflicted entities to Room`
  - `downloadDispensaciones skips conflicted IDs`
  - `downloadPagos skips conflicted IDs`
  - `no active conflicts — all entities written normally`

---

## Phase 3: Implementation (GREEN) — Download Guard

- [ ] 3.1 **[GREEN]** Inject `ConflictDao` into `DownloadSyncCoordinator` `@Inject constructor` in `src/main/java/com/example/optoapp/domain/DownloadSyncCoordinator.kt` (add as 5th param; Hilt already provides it)
- [ ] 3.2 **[GREEN]** In `downloadServicios(opticaId)`: fetch `conflictIds = conflictDao.getConflictEntityIds(opticaId, "servicio_extra")`; filter remote list to exclude those IDs before writing to Room
- [ ] 3.3 **[GREEN]** In `downloadDispensaciones(opticaId)`: same guard with entity type `"dispensacion"`
- [ ] 3.4 **[GREEN]** In `downloadPagos(opticaId)`: same guard with entity type `"pago"`
- [ ] 3.5 **[REFACTOR]** Extract `suspend fun getSkipIds(opticaId: String, entityType: String) = conflictDao.getConflictEntityIds(opticaId, entityType)` inline helper if duplication warrants it; verify all 5 RED tests pass

---

## Phase 4: Tests (RED) — Keep-Mine Fix

- [ ] 4.1 **[RED]** In `src/test/java/com/example/optoapp/viewmodel/SyncViewModelConflictResolutionTest.kt`, add/update tests:
  - `resolveKeepMine for servicio calls updateServicio BEFORE syncForEntityType`
  - `resolveKeepMine for dispensacion calls updateDispensacion BEFORE syncForEntityType`
  - `resolveKeepMine for pago calls updatePago BEFORE syncForEntityType`
  - `resolveKeepMine resolves conflict record only after successful upload`
  - `resolveKeepMine retains conflict record when upload fails`
  - `resolveKeepMineAll bumps all entities, calls clearConflicts, then performFullSync`
  - `resolveKeepMineAll retains conflict for entity whose upload fails`

---

## Phase 5: Implementation (GREEN) — Keep-Mine Fix & Bulk Action

- [ ] 5.1 **[GREEN]** Fix `SyncViewModel.resolveKeepMine()` in `src/main/java/com/example/optoapp/viewmodel/SyncViewModel.kt` (lines ~134-147): fetch entity from Room by `entityId + entityType`, call `repository.updateServicio(entity)` / `updateDispensacion(entity)` / `updatePago(entity)` to bump `updatedAt`, THEN call `syncForEntityType(...)`, THEN call `conflictDao.resolveConflict()` only on upload success
- [ ] 5.2 **[GREEN]** Handle nullable `getPagoById()`: unwrap `Pago?` with null-check (log + return early if null); unlike servicio/dispensacion it is not wrapped in `Resource<T>`
- [ ] 5.3 **[GREEN]** Add `suspend fun resolveKeepMineAll(opticaId: String)` to `SyncViewModel`: fetch all active conflicts via `conflictDao.getActiveConflicts(opticaId)`, loop bumping each entity (`updateServicio`/`updateDispensacion`/`updatePago`), collect successes/failures, call `conflictDao.clearConflicts(opticaId)` for successful ones + update `syncEntityStateDao`, then call `performFullSync()`
- [ ] 5.4 **[REFACTOR]** Extract shared bump-by-type helper inside `SyncViewModel` to avoid duplicating the when-entityType branch across `resolveKeepMine` and `resolveKeepMineAll`; verify all RED tests pass

---

## Phase 6: UI — "Usar el mío para todos" Button

- [ ] 6.1 Add `"Usar el mío para todos"` `TextButton` in `ConflictosScreen.kt` (`src/main/java/com/example/optoapp/ui/screens/ConflictosScreen.kt`) inside `OptoTopAppBar` actions row, beside the existing `"Usar nube para todos"` button; wire to `viewModel.resolveKeepMineAll(opticaId)`

---

## Phase 7: Verification

- [ ] 7.1 **[VERIFY TRIGGER]** Confirm live trigger fix: query Supabase `SELECT prosrc FROM pg_proc WHERE proname = 'set_updated_audit_fields'` — result MUST contain `IF NEW.updated_at IS NULL` (not unconditional `NEW.updated_at = NOW()`)
- [ ] 7.2 **[VERIFY TESTS]** Run `./gradlew testDebugUnitTest` from `Optoapp/` — all tests pass with zero failures
- [ ] 7.3 **[VERIFY BUILD]** Run `./gradlew assembleDebug` — clean build, no compile errors from constructor arity change or new method

---

## Spec Requirements Cross-Reference

| Task(s) | Spec Requirement |
|---------|-----------------|
| 1.1, 2.1–3.5 | ConflictDao Exposes Conflict ID Query; Download Must Not Overwrite Conflicted Entities |
| 1.2, 4.1, 5.1–5.2 | Keep-Mine Resolution Uploads and Clears the Conflict |
| 4.1, 5.3 | Bulk Keep-Mine Resolves All Conflicts |
| 6.1 | Bulk Keep-Mine (UI surface) |
| 7.1 | Supabase Trigger Preserves Client Timestamp |
| 7.2–7.3 | Normal Sync Is Unaffected by Conflict Guard (regression) |

## Parallelism

- **Sequential only**: Phases 1 → 2 → 3 → 4 → 5 → 6 → 7. Phase 1 (DAO query + `updatePago`) must land first — all downstream tests and implementations depend on it.
- Phases 2 and 4 (test writing) could overlap if two developers are available, but they share the `SyncViewModel` test file so coordination is needed.
