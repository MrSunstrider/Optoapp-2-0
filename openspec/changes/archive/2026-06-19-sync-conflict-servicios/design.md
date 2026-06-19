# Design: Stop Sync Churn for Download-Path Entities

## Technical Approach

Add four `upsertXxxFromRemote()` bypass methods on `OptoRepository` that write a remote
record to its DAO **as-is** — no `copy(updatedAt = Instant.now())`, no
`postSaveSyncScheduler` call. Switch the four download-path callers to these methods.
User-action save methods stay untouched. This replicates the existing `upsertArqueoFromRemote`
pattern (OptoRepository.kt:153) for the 4 remaining churning entities. All target DAO
insert methods are `@Upsert`, so re-download updates the existing row by primary key —
no duplicates, no extra delete/insert logic needed.

Invariant: **download paths NEVER traverse the `Instant.now()` stamping path.**

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|----------------------|-----------|
| Bypass mechanism | New dedicated `upsertXxxFromRemote()` methods | (a) flag param `stamp:Boolean` on existing methods; (b) strip stamping from existing methods | Dedicated names match existing `upsertArqueoFromRemote`/`upsertPaciente` convention, are self-documenting, and keep user-action paths byte-identical (zero regression risk on merge/UI saves). A boolean flag invites mis-call at new call sites; stripping breaks user-action stamping. |
| Scheduler call | Omit entirely in bypass methods | Keep scheduler "just in case" | Download is the **terminal step** of a sync cycle. Scheduling another upload for a record we just received re-creates the churn loop. No follow-up upload should ever be scheduled from a download write. |
| Write target | Delegate to existing `dispensacionRepo.insertX()` / `pacienteRepo.insertEvaluacion()` | Inject DAOs directly into bypass methods | Those repo methods already call the `@Upsert` DAO with no side effects. Reusing them keeps the layering (`OptoRepository → sub-repo → DAO`) intact. |
| `mergeLocalDispensacionConflict` | Leave calling `updateDispensacion()` (stamping) | Switch it to bypass | That path is a real LOCAL edit (field reconciliation). It MUST stamp + schedule so the merged result propagates upstream. Out of scope per proposal. |

## Data Flow

```
BEFORE (churn loop):
  Supabase ──download──> insertServicio() ──copy(updatedAt=now)──> DAO
                                │
                                └──> scheduleFinanzasSync() ──upload──> Supabase ──> (loop)

AFTER (terminal):
  Supabase ──download──> upsertServicioFromRemote() ──as-is──> @Upsert DAO
                                (no stamp, no scheduler) ──> STOP
```

User-action path is unchanged: `UI → insertServicio()/updateServicio() → stamp + schedule → upload`.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/OptoRepository.kt` | Modify | Add 4 bypass methods near existing `upsertArqueoFromRemote` (line 153) |
| `domain/DownloadSyncCoordinator.kt` | Modify | 3 call-site swaps: line 64, line 88, line 112 |
| `domain/SyncHistorialUseCase.kt` | Modify | 1 call-site swap: line 187 |
| `test/.../data/OptoRepositoryFromRemoteTest.kt` | Create | MockK unit tests: bypass methods do not re-stamp and do not schedule |
| `androidTest/.../data/DownloadTimestampIntegrityTest.kt` | Create | Room in-memory: stored `updatedAt == remote.updatedAt` for all 4 entities |

## Interfaces / Contracts

New methods on `OptoRepository` (data classes carry `updatedAt: String?`, value passed through verbatim):

```kotlin
suspend fun upsertServicioFromRemote(servicio: ServicioExtra) =
    dispensacionRepo.insertServicio(servicio)        // no copy(), no scheduler

suspend fun upsertPagoFromRemote(pago: Pago) =
    dispensacionRepo.insertPago(pago)

suspend fun upsertDispensacionFromRemote(dispensacion: DispensacionOptica) =
    dispensacionRepo.insertDispensacion(dispensacion)

suspend fun upsertEvaluacionFromRemote(evaluacion: EvaluacionClinica) =
    pacienteRepo.insertEvaluacion(evaluacion)
```

Call-site swaps (download path only):

| Caller | Line | Before | After |
|--------|------|--------|-------|
| `DownloadSyncCoordinator.downloadDispensaciones` | 64 | `repository.insertDispensacion(local)` | `repository.upsertDispensacionFromRemote(local)` |
| `DownloadSyncCoordinator.downloadServicios` | 88 | `repository.insertServicio(local)` | `repository.upsertServicioFromRemote(local)` |
| `DownloadSyncCoordinator.downloadPagos` | 112 | `repository.insertPago(local)` | `repository.upsertPagoFromRemote(local)` |
| `SyncHistorialUseCase.downloadEvaluaciones` | 187 | `repository.insertEvaluacion(local)` | `repository.upsertEvaluacionFromRemote(local)` |

`downloadDispensacionItems` (line 39) and `downloadArqueos` (line 138) need NO change:
items never stamp, arqueo already bypasses.

## What NOT to Change

- `insertServicio/updateServicio`, `insertPago`, `insertDispensacion/updateDispensacion`,
  `insertEvaluacion/updateEvaluacion` — keep stamping + scheduler for user-action paths.
- `DispensacionMergeHandler.mergeLocalDispensacionConflict()` (SyncFinanzasMerge.kt:54) —
  keep calling stamping `updateDispensacion()` (real local edit).
- Server / Supabase migration — already deployed, no DB change.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Bypass methods don't re-stamp and don't schedule | `OptoRepositoryFromRemoteTest` with MockK: mock sub-repos + `PostSaveSyncScheduler`; assert sub-repo received the entity with original `updatedAt`, and `verify(exactly = 0)` on every `scheduleXxxSync`. Also assert user-action methods still stamp + schedule (`verify(exactly = 1)`). |
| Integration | Stored row keeps remote `updatedAt` | `DownloadTimestampIntegrityTest` (androidTest, Room in-memory): seed a remote-shaped entity with fixed `updatedAt`, call each `upsertXxxFromRemote`, read back via DAO, assert `stored.updatedAt == remote.updatedAt`. |
| Regression | DTO never fabricates timestamp | Existing `SyncDtoTimestampTest` already covers `toRemoto()`. EXTEND it only if a repository-level invariant assertion is wanted; otherwise leave it — it guards the upload-DTO side, this change guards the download-write side. They are complementary, not overlapping. |

## Migration / Rollout

No migration required. Pure code change; revert is the 3-file revert from the proposal's
rollback plan. Server trigger fix is independent and stays.

## Open Questions

- None blocking. `OptoRepository` is `open` and bypass methods are non-`open`; if any test
  needs to stub them, mark them `open` (low cost) — defer to apply phase.
