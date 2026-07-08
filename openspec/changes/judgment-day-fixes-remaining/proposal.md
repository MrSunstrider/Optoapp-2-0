# Proposal: Judgment Day Fixes — Remaining 6 Issues

## Intent

PR #47 audit found 16 issues. 8 were fixed in the first follow-up (`judgment-day-fixes`), 2 became moot (arqueo removal). These 6 remain: 1 CRITICAL (sync coordinator duplication), 5 WARNING (missing test coverage, missing retry, partial progress reporting, stale data UX).

## Scope

### In Scope
1. **F1**: Extract shared chunked-upsert+retry+sync-state pattern in `UploadSyncCoordinator` — 4 Group B methods (items, pagos, gastos, ventas) into private helper. Group A (dispensaciones, servicios) keep reconciliation but share try/catch boilerplate.
2. **F2**: 8 unit tests for ObtenerDeudoresUseCase Room fallback path (success, empty, missing paciente, future fecha, multiple pagos, all paid, monto cero).
3. **F3**: Replace `assertTrue(true)` in DeletionSyncHelperTest with mockk-based behavior tests (push, delete, clear, IO error, unknown entity, empty).
4. **F4**: Wrap DownloadSyncCoordinator SELECT calls with `networkRetryHelper.retryNetwork` — read-only, idempotent.
5. **F5**: Per-chunk tracking in upload loop so partial batch progress is reported instead of 0 on failure.
6. **F6**: Add `stale: Boolean = false` to `Resource.Success`. Wire through ObtenerDeudoresUseCase → ViewModel → UI "datos no actualizados".

### Out of Scope
- AnalisisMensual.esOffline refactor — already exists, kept as-is
- Schema changes, DB migrations, RLS policy changes
- Any new UI screens beyond the stale warning card

## Capabilities

### New Capabilities
None — all fixes are internal refactors, test additions, or small backward-compatible extensions.

### Modified Capabilities
None — no spec-level behavior changes.

## Approach

| Issue | Strategy | Files |
|-------|----------|-------|
| F1 | Extract `uploadChunkedBatch()` helper; Group A calls it after reconciliation | `UploadSyncCoordinator.kt` (~20 net reduction) |
| F2 | Test Room DAO mocks with controlled data covering all 8 scenarios | `ObtenerDeudoresUseCaseTest.kt` (+200) |
| F3 | Mockk-based tests with `coEvery`/`coVerify` on repository + Supabase client | `DeletionSyncHelperTest.kt` (+150) |
| F4 | Wrap `supabase.postgrest[T].select { }` calls with `retryNetwork` | `DownloadSyncCoordinator.kt` (+30) |
| F5 | Track `chunkCount` and `uploadedCount` per chunk; return partial via Pair or exception | `UploadSyncCoordinator.kt`, `SyncFinanzasUseCase.kt` (+20) |
| F6 | `Resource.Success` gains `stale` param; use case sets it; ViewModel checks → UI warning | `Resource.kt`, `ObtenerDeudoresUseCase.kt`, VM, Screen (+25) |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/UploadSyncCoordinator.kt` | Refactor | Extract helper, add per-chunk tracking |
| `domain/DownloadSyncCoordinator.kt` | Modified | Wrap SELECT with retry |
| `domain/ObtenerDeudoresUseCase.kt` | Modified | Set `stale=true` on Room fallback |
| `domain/DeletionSyncHelper.kt` | None | Tests only |
| `data/Resource.kt` | Modified | Add `stale` param to `Success` |
| `domain/SyncFinanzasUseCase.kt` | Modified | Wire per-chunk counts if needed |
| `viewmodel/AnalisisNegocioViewModel.kt` | Modified | Check `stale` flag |
| `ui/screens/AnalisisNegocioScreen.kt` | Modified | Show stale warning card |
| `domain/ObtenerDeudoresUseCaseTest.kt` | New | 8 fallback tests |
| `domain/DeletionSyncHelperTest.kt` | Rewrite | assertTrue(true) → mockk tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| F1 refactor breaks sync upload | Med | Each Group B method keeps identical behavior — helper extraction is mechanical. Existing tests catch regressions. |
| F6 stale flag conflicts with existing esOffline | Low | Different mechanism (Resource generic vs domain-specific). Both coexist; no interference. |

## Rollback Plan

Each fix is scoped to 1–2 files. Revert individual commits per fix from `judgment-day-fixes-remaining` branch. F1 is the riskiest — revert first if sync uploads break.

## Dependencies

None — all changes within Android codebase, no schema or Supabase changes.

## Success Criteria

- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes
- [ ] `./gradlew :optoapp:assembleDebug` builds
- [ ] F1: UploadSyncCoordinator.kt compiles, old tests pass, ~20 lines net removed
- [ ] F2: 8 new test methods in ObtenerDeudoresUseCaseTest cover all fallback scenarios
- [ ] F3: DeletionSyncHelperTest has 0 assertTrue(true) calls, all real mockk verifications
- [ ] F4: All DownloadSyncCoordinator SELECT calls wrapped in retryNetwork
- [ ] F5: Chunk failure reports partial count instead of 0
- [ ] F6: Offline deudores fallback shows stale warning in AnalisisNegocioScreen
