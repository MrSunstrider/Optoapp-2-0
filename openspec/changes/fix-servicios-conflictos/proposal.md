# Proposal: Fix Persistent servicios_extra Sync Conflicts

## Intent

77 `servicios_extra` conflicts persist forever in OptoApp. When a user picks "Usar el mío" the local data is silently discarded and the same conflict reappears on the next sync. Three bugs combine: (A) `resolveKeepMine()` routes through `filterConflicts()`, which always sees `local.updatedAt < remote.updated_at` because the Supabase trigger `trg_servicios_extra_set_updated_audit` advances the server timestamp on every upload, so the entity is blocked from upload and then overwritten by download; (B) `DownloadSyncCoordinator` overwrites Room entities even when they have active conflict records; (C) no "Usar el mío para todos" bulk action exists, forcing one-at-a-time resolution that silently fails. The bugs affect `dispensaciones` and `pagos` identically (same `syncFinanzasUseCase` path). Users lose finance edits without warning — unacceptable for a clinical billing record.

## Scope

### In Scope
- Download guard: skip entities with active conflict records (`servicio_extra`, `dispensacion`, `pago`).
- Force-upload path: "keep mine" actually uploads the local version, bypassing `filterConflicts()`.
- Bulk "Usar el mío para todos" action in `ConflictosScreen`.
- New DAO query `getConflictIdsByType(opticaId, entityType)`.
- TDD behavior tests for download skip and keep-mine upload (Strict TDD active).

### Out of Scope
- Changing the Supabase trigger or server-side timestamp semantics.
- Field-level/three-way merge UI (only whole-record keep-mine / keep-cloud).
- Migrating the conflict model away from timestamp comparison.
- Recovering local snapshots already lost from past silent overwrites.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `sync-conflict-resolution`: "keep mine" MUST force-upload the local record and clear the conflict; download MUST NOT overwrite entities with active conflicts; bulk keep-mine MUST resolve all conflicts of a type. Applies to `servicio_extra`, `dispensacion`, `pago`.

## Approach

1. **Bug B (download guard):** inject `ConflictDao` into `DownloadSyncCoordinator`; in `downloadServicios/Dispensaciones/Pagos` fetch conflicted IDs via `getConflictIdsByType` and skip them (mirrors `DeletionSyncHelper.deletedIds()`).
2. **Bug A (force-upload):** add `forceUploadServicio/Dispensacion/Pago(opticaId, ids)` to `UploadSyncCoordinator` that bypass `filterConflicts()` and upsert directly to Supabase. `resolveKeepMine()` then: fetch entity ID → force-upload → download module to sync server timestamp back to Room → `conflictDao.resolveConflict()` only after success.
3. **Bug C (bulk UX):** add `resolveKeepMineAll(entityType)` / `resolveKeepMineAllServicios()` in `SyncViewModel`; add "Usar el mío para todos" `TextButton` beside "Usar nube para todos".

Sequence: B → A → C (guard prevents data loss before the resolution flow runs).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/DownloadSyncCoordinator.kt` | Modified | Inject `ConflictDao`; skip conflicted IDs on download |
| `domain/UploadSyncCoordinator.kt` | Modified | Add force-upload methods bypassing `filterConflicts()` |
| `data/sync/ConflictRecord.kt` (`ConflictDao`) | Modified | Add `getConflictIdsByType(opticaId, entityType)` |
| `viewmodel/SyncViewModel.kt` | Modified | Fix `resolveKeepMine()`; add `resolveKeepMineAll()` |
| `ui/screens/ConflictosScreen.kt` | Modified | Add "Usar el mío para todos" button |
| `test/domain/DownloadSyncCoordinatorConflictSkipTest.kt` | New | Verify conflicted IDs skipped |
| `test/viewmodel/SyncViewModelKeepMineTest.kt` | New | Verify force-upload + Room gets server timestamp |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Hilt wiring breaks adding `ConflictDao` dep | Low | `ConflictDao` already provided (used by `SyncViewModel`) |
| Race: another device edits during force-upload | Med | Download server timestamp immediately in same cycle |
| Stale local snapshot (past silent overwrites) | Med | Document that old conflicts may already hold remote data; future warning |
| Existing structural tests give false confidence | High | Replace reflection-only tests with real behavior tests (mockk) |

## Rollback Plan

Changes are additive and isolated per file. Revert the commit(s): restore `DownloadSyncCoordinator` 4-arg constructor, remove force-upload methods, restore original `resolveKeepMine()`, remove the bulk button and DAO query. No schema/migration changes, so rollback is code-only with no data impact.

## Dependencies

- `ConflictDao` already wired via Hilt (no new provider needed).
- mockk for new behavior tests.

## Success Criteria

- [ ] "Usar el mío" uploads the local record and the conflict does NOT reappear on next sync.
- [ ] Download does not overwrite an entity that has an active conflict record.
- [ ] "Usar el mío para todos" clears all conflicts of a type in one action.
- [ ] Fix applies to `servicio_extra`, `dispensacion`, and `pago`.
- [ ] New behavior tests fail before the fix and pass after.
