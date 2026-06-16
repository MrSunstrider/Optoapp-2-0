# Design: Sync Conflict Root-Cause Fix

## Technical Approach

Six independent root causes regenerate "resolved" sync conflicts. Each fix lands in its
existing layer (ViewModel orchestration, Dao persistence, Scheduler lifecycle) and mirrors a
proven sibling pattern already in the codebase. No new abstractions except a private
entity-type → UseCase dispatcher extracted from the existing `resolveAcceptTheirs` `when`.
TDD-first: every RC gets a failing test before its fix. Maps to proposal PRs A/B/C.

## Architecture Decisions

| Decision | Choice | Alternative rejected | Rationale |
|----------|--------|----------------------|-----------|
| RC-3 keep-mine upload | After clearing conflict, dispatch upload then download for the entity type, mirroring `resolveAcceptTheirs` (SyncViewModel.kt:118-141) | Full bidirectional sync of all modules | Targeted upload is cheap and makes local authoritative; download refreshes Room timestamps so no re-conflict |
| RC-3 dispatcher | Private `syncForEntityType(opticaId, entityType, skipUpload)` helper reused by keep-mine and accept-theirs | Duplicate `when` blocks | Single source of truth for entityType→UseCase mapping; keep-mine calls `skipUpload=false`, accept-theirs `skipUpload=true` |
| RC-4 state cleanup | Add `deleteConflictedForOptica(opticaId)` to SyncEntityStateDao, call in `acceptAllCloud` next to `clearConflicts` | Cast a wide DELETE in tracker | Dao is the persistence boundary; mirrors existing `deleteErrorsForOptica` query exactly |
| RC-4 column casing | Query uses camelCase `opticaId`/`status` (NOT snake_case) | Proposal's `optica_id`/`'conflicted'` | Room entity `SyncEntityState` uses camelCase columns; snake_case would fail at compile/runtime |
| RC-1 timestamp source | Stamp `updatedAt = Instant.now().toString()` at Room write sites; make `toRemoto()` require non-null (throw on null) | Keep `?: Instant.now()` fallback in `toRemoto()` | Fallback re-stamps a new time every upload → permanent drift vs remote. Single stamp at save = stable across cycles |
| RC-2 silent sync refresh | Option A: `downloadAfterUpload = true` in `performSilentSync` | Option B lightweight timestamp-only query | Simplicity; bandwidth not flagged as constraint. Reuses existing download path that already writes server timestamps to Room |
| RC-5 cancelPending race | `suspend fun cancelPending()` doing `job.cancelAndJoin()` off main dispatcher | Leave fire-and-forget `launch` | Awaiting cancellation closes the race with `performFullDownload`; join prevents a stale debounced job re-running post-download |
| RC-6 pagos guard | Add `filterConflicts` to `uploadPagos`, mirroring `uploadDispensaciones` | Skip check | Without it, pagos silently overwrite newer remote rows = data loss for financial records |

## Data Flow

### RC-3 resolveKeepMine — before / after

    BEFORE:  resolveKeepMine ─→ conflictDao.resolveConflict ─→ (nothing)
             local change never uploaded ─→ next sync re-detects conflict ✗

    AFTER:   resolveKeepMine ─→ resolveConflict ─→ refreshSession
                    └→ syncForEntityType(opticaId, type, skipUpload=false)  [UPLOAD local]
                    └→ download path refreshes Room updatedAt  ─→ no re-conflict ✓

### RC-1 + RC-2 silent sync timestamp cycle — before / after

    BEFORE:  save (updatedAt=null) ─→ toRemoto() stamps Instant.now() each upload
             silent sync uploads, downloadAfterUpload=false ─→ Room keeps null
             next cycle: new Instant.now() ≠ remote ─→ phantom conflict ✗

    AFTER:   save ─→ updatedAt=Instant.now() stamped ONCE at Room write
             toRemoto() requires non-null (no re-stamp)
             silent sync uploads, downloadAfterUpload=true ─→ server ts written to Room
             next cycle: Room ts == remote ts ─→ no conflict ✓

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `viewmodel/SyncViewModel.kt` | Modify | RC-3: keep-mine uploads via new `syncForEntityType`; RC-4: `acceptAllCloud` calls `deleteConflictedForOptica`; RC-2: `performSilentSync` uses `downloadAfterUpload=true` |
| `data/SyncEntityStateDao.kt` | Modify | RC-4: add `deleteConflictedForOptica(opticaId)` |
| `domain/SyncPacientesUseCase.kt` | Modify | RC-1: remove `?: Instant.now()` in `toRemoto()`; require non-null |
| `domain/SyncHistorialDto.kt` | Modify | RC-1: same fallback removal |
| `domain/SyncFinanzasDto.kt` | Modify | RC-1: remove fallback at 3 sites |
| `domain/SyncInventarioUseCase.kt` | Modify | RC-1: same |
| Room write call sites (Repositories/ViewModels) | Modify | RC-1: stamp `updatedAt` before insert/update |
| `sync/PostSaveSyncScheduler.kt` | Modify | RC-5: `cancelPending()` → `suspend`, `cancelAndJoin` off main |
| `domain/UploadSyncCoordinator.kt` | Modify | RC-6: `uploadPagos` calls `filterConflicts` before upsert |

## Interfaces / Contracts

```kotlin
// RC-4 — SyncEntityStateDao (camelCase columns, mirrors deleteErrorsForOptica)
@Query("DELETE FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'conflicted'")
suspend fun deleteConflictedForOptica(opticaId: String)

// RC-5 — PostSaveSyncScheduler
suspend fun cancelPending()  // withContext(Dispatchers.IO) { jobs.forEach { it.cancelAndJoin() } }

// RC-3 — SyncViewModel (private; extracted from resolveAcceptTheirs when-block)
private suspend fun syncForEntityType(opticaId: String, entityType: String, skipUpload: Boolean)
```

## Testing Strategy

| RC | Test class | Mock / real | Scenario & assertions | Run |
|----|-----------|-------------|-----------------------|-----|
| RC-3 | `viewmodel/SyncViewModelConflictResolutionTest` | MockK UseCases + ConflictDao; real ViewModel | `resolveKeepMine` → verify `resolveConflict` then `syncPacientesUseCase(skipUpload=false, downloadAfterUpload=true)` invoked | `--tests "*SyncViewModelConflictResolutionTest"` |
| RC-4 | same class | MockK Dao | `acceptAllCloud` → verify both `clearConflicts` AND `deleteConflictedForOptica` called | same |
| RC-1 | `domain/SyncDtoTimestampTest` (extend `SyncFinanzasDtoTest`) | pure unit | `toRemoto()` with null updatedAt throws; with set value preserves it across 3 calls | `--tests "*SyncDtoTimestampTest"` |
| RC-2 | `viewmodel/SyncViewModelSilentSyncTest` | MockK UseCases | `performSilentSync` invokes UseCases with `downloadAfterUpload=true` | `--tests "*SyncViewModelSilentSyncTest"` |
| RC-5 | `sync/PostSaveSyncSchedulerTest` (extend) | test-subclass override + TestScope | schedule job, `cancelPending()`, assert job cancelled & joined before return | `--tests "*PostSaveSyncSchedulerTest"` |
| RC-6 | `domain/UploadSyncCoordinatorPagosTest` | MockK ConflictHelper + repo | `uploadPagos` calls `filterConflicts("pagos", ...)`; conflicted pago excluded from upsert | `--tests "*UploadSyncCoordinatorPagosTest"` |
| Roundtrip | reuse RC-3 class | MockK | upload→download preserves timestamp, no re-conflict | — |

Full module: `./gradlew :optoapp:testDebugUnitTest`

## Migration / Rollout

No Room schema migration. RC-1 stamps in code only (no NOT NULL column add — P6 deferred).
Chained stacked-to-main PRs, each independently revertible C→B→A.

### PR boundaries (confirmed from proposal)

| PR | RCs | Entry state | Exit state | Verify |
|----|-----|-------------|-----------|--------|
| PR-A | RC-3 + RC-4 | conflicts re-appear after keep-mine; acceptAllCloud leaves conflicted rows | resolutions stick; zero conflicted rows | `--tests "*SyncViewModelConflictResolutionTest"` |
| PR-B | RC-1 + RC-2 | null updatedAt drifts each cycle | stable timestamp ≥3 cycles | `--tests "*SyncDtoTimestampTest" "*SyncViewModelSilentSyncTest"` |
| PR-C | RC-5 + RC-6 | cancelPending race; pagos overwrite remote | race closed; pagos guarded | `--tests "*PostSaveSyncSchedulerTest" "*UploadSyncCoordinatorPagosTest"` |

## Open Questions

- [ ] RC-1: enumerate exact Room write call sites per module (apply phase must grep insert/update DAO callers); risk of missing one keeps drift alive for that entity.
- [ ] RC-5: confirm no `cancelPending()` caller runs on the main dispatcher (only `performFullDownload` found; audit during apply).

## Risks & Mitigations

- **Timestamp precision (Instant vs String)**: compare via `ConflictHelper.isLocalNewerOrEqual` which parses to UTC `Instant`; assert exact preservation in roundtrip test. Low/Med.
- **suspend cancelPending call sites**: only `performFullDownload` calls it today (already suspend); audit all callers during apply; keep cancellation off main dispatcher. Med.
- **toRemoto() null fallback removal**: any caller relying on lazy stamping breaks — that is the intent, but every save site must stamp first or upload throws. Gated by RC-1 write-site audit. Med.
