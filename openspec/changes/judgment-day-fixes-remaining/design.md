# Design: Judgment Day Fixes — Remaining 6 Issues

## Technical Approach

Six scoped fixes across sync coordinators, use case, tests, and UI. No new features — all are refactors, test rewrites, or backward-compatible extensions. Each fix is independently revertable.

## Architecture Decisions

### Decision: Upload helper extraction (F1)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Keep duplication | 6 methods, ~50% copy-paste; any upsert change requires 6 edits | ❌ |
| Extract `upsertBatchWithSync` helper | Group A (uploadDispensaciones, uploadServicios) retain reconciliation + call helper for upsert. Group B (items, pagos, gastos, ventas) become thin wrappers. | ✅ |

Helper signature:
```kotlin
private suspend fun <R> upsertBatchWithSync(
    opticaId: String,
    tableName: String,
    entityType: String,
    batchTrackingType: String,
    rows: List<R>,
    idSelector: (R) -> String
): Int
```

The helper chunks by `UPSERT_BATCH_SIZE`, wraps each chunk in `networkRetryHelper.retryNetwork`, catches IOException → `markError` + rethrow, CancellationException → rethrow, success → `markSynced` batch + per-entity. Group A methods pass their post-reconciliation row list; Group B methods are ~8-line wrappers (fetch → map → distinct → call helper).

### Decision: Download helper and retry wrapping (F2 + F4)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Inject networkRetryHelper into DownloadSyncCoordinator | SELECT calls currently unprotected. Retry is idempotent (read-only). | ✅ Add `networkRetryHelper: NetworkRetryHelper` to constructor. Wrap each `supabase.postgrest[T].select{} + decodeList` with `networkRetryHelper.retryNetwork`. On failure → Log + markError + return 0. |
| Extract `downloadTable` helper | Same chunked+retry pattern across 5 download methods. Extract a generic helper. | ✅ |

Helper:
```kotlin
private suspend fun <T : Any, R : Any> downloadTable(
    opticaId: String,
    tableName: String,
    entityType: String,
    skipDeletions: Boolean,
    decoder: (T) -> R,
    upsert: (R) -> Unit
): Int
```

Wrap the SELECT inside `retryNetwork`. On IOException → log + `markError` + return 0 (graceful degradation).

### Decision: DeletionSyncHelperTest abstraction (F3)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Create `SupabaseDeleteClient` interface | New abstraction layer. Production: real impl. Test: mock. Invasive — requires constructor change. | ❌ Too invasive for a test-only fix. |
| Test side effects only | Mock `repository` methods. Verify `clearDeletionState` calls, `getPendingDeletions` returns. Don't mock supabase-kt DSL. | ✅ Each test: mock `repository.getPendingDeletions` to return specific data, call `pushPendingDeletions`, verify `repository.clearDeletionState` was/wasn't called. Supabase interaction is tested by integration tests. |

Test matrix:
- Unknown entity type → clearDeletionState called
- Post-delete success → clearDeletionState called
- IOException → clearDeletionState NOT called (stays pending for retry)
- Empty pending → returns early, no supabase calls
- Error handling: CancellationException rethrown, generic Exception logged + skipped

### Decision: Chunked batch counts (F5)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| All-or-nothing | Current behavior: if chunk 3/4 fails, returns 0 via safeUpload. Reports zero even though 2 chunks succeeded. | ❌ |
| Per-chunk try/catch with partial count | Returns partial success count. safeUpload catches IOException at method level — conflict. | ❌ |
| Add comment + improve RESULT reporting | No behavioral change, just transparency. | ✅ **Simplest correct fix**: wrap each chunk upsert in per-chunk try/catch inside `upsertBatchWithSync`. Track `var succeeded = 0`. On chunk failure → `Log.e` + `markError` + continue. Return `succeeded`. The IOException is caught inside the loop, not propagated — allowing later chunks to still attempt. This is a design decision: **partial upload is better than zero**. |

This makes the upload loop **best-effort per chunk** rather than all-or-nothing. safeUpload will no longer see the IOException (caught inside the helper) so it returns the partial count.

### Decision: Stale indicator (F6)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| New sealed subclass `Resource.Stale<T>` | More expressive but requires pattern-match changes everywhere `Resource` is destructured. | ❌ |
| `val stale: Boolean = false` on `Success` | Backward compatible. Existing `(result as? Resource.Success)?.data` consumers ignore `stale`. | ✅ |

Flow: `ObtenerDeudoresUseCase.invoke()` → sets `stale = true` on the fallback (Room) path → `AnalisisNegocioViewModel.loadData()` extracts `(deudoresResult as? Resource.Success)?.stale ?: false` → wires to `AnalisisNegocioUiState.mostrarAdvertenciaEstacionalidad` (reusing existing field — stale data is another "datos no actualizados" scenario).

## Data Flow

```
F6 stale indicator:

RPC success ──→ Resource.Success(deudores, stale=false)
                                      ↓
RPC IOException ──→ Room fallback ──→ Resource.Success(deudores, stale=true)
                                      ↓
                             AnalisisNegocioViewModel
                                  extracts stale flag
                                      ↓
                             mostrarAdvertenciaEstacionalidad = stale
                                      ↓
                             Screen renders warning card

F1 upload flow (after refactor):

uploadDispensaciones(opticaId)
  ├─ reconciliation logic (unchanged)
  └─ upsertBatchWithSync(rows=uniqueRows, table=DISPENSACIONES, ...)
       ├─ chunk[0] ──→ retryNetwork ──→ markSynced ──→ succeeded += chunk.size
       ├─ chunk[1] ──→ retryNetwork ──→ markSynced ──→ succeeded += chunk.size
       └─ chunk[n] ──→ IOException ──→ markError ──→ continue (skip, don't abort)
  return succeeded  (partial if any chunk failed)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/UploadSyncCoordinator.kt` | Modify | Extract `upsertBatchWithSync` helper (~-20 net lines). Group B methods become thin wrappers. Add per-chunk try/catch for partial progress. |
| `domain/DownloadSyncCoordinator.kt` | Modify | Inject `networkRetryHelper`. Extract `downloadTable` helper. Wrap all SELECT calls with retry. |
| `domain/DeletionSyncHelper.kt` | Modify | Add `networkRetryHelper` param (for consistency) — no behavioral change. |
| `data/Resource.kt` | Modify | Add `val stale: Boolean = false` to `Success<T>` constructor. |
| `domain/ObtenerDeudoresUseCase.kt` | Modify | Pass `stale = true` on Room fallback path. |
| `viewmodel/AnalisisNegocioViewModel.kt` | Modify | Extract `stale` flag from deudores result; wire to UI state. |
| `ui/screens/AnalisisNegocioScreen.kt` | Modify | Existing `mostrarAdvertenciaEstacionalidad` card already renders — no additional UI needed. |
| `domain/SyncFinanzasUseCase.kt` | Modify | No behavioral change — `safeUpload` returns partial count naturally since IOException is caught inside upsert helper. |
| `domain/ObtenerDeudoresUseCaseTest.kt` | Modify | Add 8 test cases for Room fallback path. |
| `domain/DeletionSyncHelperTest.kt` | Rewrite | Replace `assertTrue(true)` with mockk-based behavior tests (repository mocks, clearDeletionState verification). |

## Interfaces / Contracts

```kotlin
// Resource.kt — new stale parameter on Success
class Success<T>(data: T, val stale: Boolean = false) : Resource<T>(data)

// UploadSyncCoordinator.kt — new helper
private suspend fun <R> upsertBatchWithSync(
    opticaId: String,
    tableName: String,
    entityType: String,
    batchTrackingType: String,
    rows: List<R>,
    idSelector: (R) -> String
): Int

// DownloadSyncCoordinator.kt — new helper + injected retry
private suspend fun <T : Any, R : Any> downloadTable(
    opticaId: String,
    tableName: String,
    entityType: String,
    skipDeletions: Boolean,
    decoder: (T) -> R,
    upsert: (R) -> Unit
): Int
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | F2: Room fallback — all 8 scenarios | DAO mocks with `coEvery`/`coVerify`. Force RPC IOException → verify fallback data. Cover: empty ventas, missing paciente, future fecha (negative dias), multiple pagos, all-paid, monto cero, missing JSON fields. |
| Unit | F3: DeletionSyncHelper behavior | Mock `repository.getPendingDeletions`. Test unknown type, IOException, empty list, success path. Verify `clearDeletionState` call counts. |
| Unit | F1/F4: Existing UploadSyncCoordinator/DownloadSyncCoordinator tests pass | Characterization tests already exist — they verify class structure and method contracts. Regression only. |
| Unit | F5: Partial upload counts | Upload test with mock supabase that throws on specific chunk — verify return is partial count, not 0. |
| Build | F6: Stale flag | Compile check. VM test verifies `stale` is extracted from Resource.Success and wired to UI state. |

## Migration / Rollout

No migration required. Each fix is scoped to 1–3 files and independently revertable.

## Open Questions

- [ ] None — all decisions scoped and resolved in this document.
