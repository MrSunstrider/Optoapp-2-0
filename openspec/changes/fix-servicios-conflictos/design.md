# Design: Fix Persistent servicios_extra Sync Conflicts

## Technical Approach

Per approved proposal (#669): fix the conflict cycle at three layers — trigger (server), download guard, and keep-mine resolution (client) — instead of a force-upload bypass. Codebase reading revealed the **trigger fix already shipped** in migration `20260615000000_fix_updated_at_trigger_no_overwrite.sql`: `updated_at` is already client-owned. This makes the client-side bump strategy correct: bump local `updated_at = NOW`, upload (now `local >= remote`, so `filterConflicts` lets it through), trigger preserves it server-side, download guard prevents same-cycle overwrite. Targets `servicio_extra`, `dispensacion`, `pago` (shared `syncFinanzasUseCase` path).

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|-----------------------|-----------|
| Force upload | NO bypass of `filterConflicts`; bump local `updatedAt` then normal upload | `UploadSyncCoordinator.forceUpload*` direct upsert | User-approved; bypass duplicates merge logic and risks clobbering newer remote without timestamp discipline |
| Trigger migration | Reuse existing `20260615000000`; do NOT add a new migration | New `20260620200000` redundant migration | Existing fn already preserves client `updated_at`. A second migration is dead weight and risks drift. Verify-only |
| Bump mechanism | Reuse existing `OptoRepository.updateServicio/updateDispensacion` (auto-stamp `updatedAt=now`) | New `bumpEntityUpdatedAt(id,type,ts)` with explicit ts param | Repo wrappers already re-stamp `updatedAt=Instant.now()` internally (lines 73, 99). Less new surface |
| Download guard | Inject `ConflictDao`; skip IDs with active conflict records | Status flag on entity; separate skip table | Mirrors existing `DeletionSyncHelper.deletedIds()` skip pattern exactly |
| Pago bump | Add `OptoRepository.updatePago` wrapper (does not exist yet) | Call `PagoDao.updatePago` from VM | Keep VM off DAO; repo is the single write seam that schedules post-save sync |

## Data Flow

    User "Usar el mío" → SyncViewModel.resolveKeepMine(entity)
      → repository.update{Servicio|Dispensacion|Pago}(fetched.copy())  // Room updatedAt = NOW
      → syncForEntityType(skipUpload=false)            // = syncFinanzasUseCase
          ├ upload: filterConflicts → local NOW >= remote → SAFE → upsert
          │         trigger preserves client updated_at = NOW
          └ download: conflictedIds = {entity.id} → SKIP (Room kept) ← Bug B guard
      → conflictDao.resolveConflict(id)                // only after success
      → _conflicts/_conflictCount updated
    Next sync: download conflictedIds empty → entity flows normally

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../data/ConflictRecord.kt` (`ConflictDao`) | Modify | Add `getConflictEntityIds(opticaId, entityType): List<String>` |
| `optoapp/.../domain/DownloadSyncCoordinator.kt` | Modify | Add `conflictDao` ctor param; skip conflicted IDs in `downloadServicios/Dispensaciones/Pagos` |
| `optoapp/.../data/OptoRepository.kt` | Modify | Add `updatePago(pago)` wrapper (mirror `updateServicio`); optional `bumpEntity` dispatcher |
| `optoapp/.../viewmodel/SyncViewModel.kt` | Modify | Fix `resolveKeepMine` (bump before sync); add `resolveKeepMineAll()` |
| `optoapp/.../ui/screens/ConflictosScreen.kt` | Modify | Add "Usar el mío para todos" TextButton in `actions` |
| `optoapp/.../supabase/migrations/20260615000000_...sql` | None | Trigger already correct — verify only, no new migration |
| `test/.../domain/DownloadSyncCoordinatorConflictGuardTest.kt` | Create | Conflicted IDs skipped; non-conflicted downloaded; skipIds+conflictIds independent |
| `test/.../viewmodel/SyncViewModelConflictResolutionTest.kt` | Create/Modify | keep-mine bumps then syncs then resolves; bulk bumps all + clears |

## Interfaces / Contracts

```kotlin
// ConflictDao (package com.example.optoapp.data — NOT data.sync)
@Query("SELECT entityId FROM conflict_records WHERE opticaId = :opticaId AND entityType = :entityType")
suspend fun getConflictEntityIds(opticaId: String, entityType: String): List<String>

// DownloadSyncCoordinator guard (per download fn)
val conflictedIds = conflictDao.getConflictEntityIds(opticaId, "servicio_extra").toSet()
remotos.forEach { r -> if (r.id in skipIds) return@forEach; if (r.id in conflictedIds) return@forEach; /* ... */ }

// SyncViewModel.resolveKeepMine — bump then existing flow
val existing = repository.getServicioById(entity.entityId)   // Resource<ServicioExtra>
if (existing is Resource.Success) repository.updateServicio(existing.data.copy())  // re-stamps updatedAt=NOW
syncForEntityType(opticaId, entity.entityType, skipUpload = false)
conflictDao.resolveConflict(entity.entityId, opticaId)  // only after success
```

`ConflictDao` is already Hilt-provided (SyncViewModel injects it), so adding it to `DownloadSyncCoordinator`'s `@Inject constructor` wires automatically.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit | Download guard skips conflicted IDs; downloads non-conflicted; skipIds and conflictIds independent | mockk `ConflictDao`/`DeletionSyncHelper`/`supabase`, assert repo upsert calls |
| Unit | `resolveKeepMine` bumps via `updateServicio` BEFORE sync, resolves only after; `resolveKeepMineAll` bumps all + clearConflicts | mockk repo + DAO, verify call order |
| Behavior | Conflict does not reappear next cycle | Assert resolved entity absent from second `getConflicts` |

Strict TDD active: behavior tests fail before fix, pass after. Replace any reflection-only structural tests with mockk behavior tests.

## Migration / Rollout

No new DB migration. Trigger fix already deployed (`20260615000000`). Code-only, additive, per-file — rollback = revert commit (restore 4-arg `DownloadSyncCoordinator` ctor, original `resolveKeepMine`, drop DAO query + bulk button). No data impact.

## Open Questions

- [ ] Confirm `20260615000000` is applied to the live Supabase project (verify via advisor/schema dump before relying on client bump). The task-prompt's `IS NOT DISTINCT FROM OLD` variant is functionally equivalent and NOT required.
- [ ] `resolveKeepMineAll` ordering: bump-all then `clearConflicts` then `performFullSync` mirrors `acceptAllCloud`; confirm full-sync (not module-only) is acceptable cost for bulk.
