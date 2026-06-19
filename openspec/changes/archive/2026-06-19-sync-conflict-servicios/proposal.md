# Proposal: Stop Sync Churn for Download-Path Entities

## Intent

The offline-first Android app re-uploads server data it just downloaded, creating a perpetual sync churn loop. Root cause: `OptoRepository` mutating methods unconditionally call `entity.copy(updatedAt = Instant.now())` AND `postSaveSyncScheduler.scheduleXxxSync()` on EVERY write — including during server-data download. A downloaded record gets a fresh local timestamp newer than the server copy, so the next cycle re-uploads it, the server preserves that timestamp (post migration `20260615`), the download re-stamps, and the loop repeats. This is not a true conflict; it is churn that wastes egress, battery, and DB writes. The server half is already fixed; the Android half is still live.

## Scope

### In Scope
- Add `upsertXxxFromRemote()` bypass methods to `OptoRepository` for the 4 affected entities: `ServicioExtra`, `DispensacionOptica`, `Pago`, `EvaluacionClinica`.
- Bypass methods skip `updatedAt` stamping AND skip the `postSaveSyncScheduler` call.
- Update download-path callers only: `DownloadSyncCoordinator` (Servicio, Dispensacion, Pago) and `SyncHistorialUseCase` (Evaluacion).
- Unit + integration tests asserting the download-preserves-timestamp invariant.

### Out of Scope
- Any server / Supabase migration changes (already deployed).
- The `ConflictHelper` / conflict-resolution algorithm.
- `Paciente`, `ArqueoCaja`, `DispensacionItem` (not affected — already bypass or never stamp).
- `DispensacionMergeHandler.mergeLocalDispensacionConflict()` — a real local edit; MUST keep calling the stamping `updateDispensacion()`.
- User-action save paths — they must keep stamping `Instant.now()` (correct behavior).

## Capabilities

### New Capabilities
- `sync-download-timestamp-integrity`: Download path persists remote records without re-stamping `updatedAt` or scheduling an upload, preventing sync churn.

### Modified Capabilities
- None

## Approach

Apply the existing codebase pattern (`upsertArqueoFromRemote`, `upsertPaciente`) to the 4 remaining entities. Each new `upsertXxxFromRemote(entity)` writes the entity to its DAO as-is and does NOT call the scheduler. Download callers switch to these methods; user-action methods are untouched. Invariant: download paths NEVER traverse the `Instant.now()` stamping path.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../data/OptoRepository.kt` | Modified | Add 4 `upsertXxxFromRemote()` methods (no stamp, no scheduler) |
| `optoapp/.../domain/DownloadSyncCoordinator.kt` | Modified | Servicio/Dispensacion/Pago download callers use bypass methods |
| `optoapp/.../domain/SyncHistorialUseCase.kt` | Modified | Evaluacion download caller uses bypass method |
| `optoapp/.../test/.../data/` | New | Repository timestamp tests + download integration tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Changing `updateDispensacion()` breaks the legit local-edit merge path | Med | Add ONLY new methods; leave `updateDispensacion()` and its `DispensacionMergeHandler` caller untouched |
| Future download paths forget the bypass and reintroduce churn | Med | Dedicated method names + tests asserting the invariant; document pattern in code |
| Bypassing scheduler skips a needed sync | Low | Download is the terminal step of a sync cycle; no further upload should be scheduled by design |

## Rollback Plan

Revert the commits touching `OptoRepository.kt`, `DownloadSyncCoordinator.kt`, and `SyncHistorialUseCase.kt`. No schema or data migration is involved, so revert is purely code-level and immediate. The previously deployed server trigger fix is independent and stays in place.

## Dependencies

- Server migration `20260615000000_fix_updated_at_trigger_no_overwrite.sql` (already deployed — prerequisite met).

## Success Criteria

- [ ] Sync churn stops: a downloaded record does not trigger a follow-up upload.
- [ ] Download preserves `updatedAt`: `stored.updatedAt == remote.updatedAt` for all 4 entities.
- [ ] User-action saves still stamp `Instant.now()` (unchanged behavior).
- [ ] `DispensacionMergeHandler` local-edit path remains unchanged and stamping.
- [ ] Existing tests pass; new tests cover the download-preserves-timestamp invariant.
