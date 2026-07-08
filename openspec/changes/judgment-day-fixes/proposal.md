# Proposal: Judgment Day Follow-up Fixes

## Intent

Post-merge corrections to PR #47 (fix/judgment-day-sync-tracking). The PR was audited by 4 judges (R1-R4) who found 5 CRITICAL and 11 WARNING issues. 2 CRITICAL (Arqueo de caja) were already moot after removal. This change applies the remaining 3 CRITICAL + 5 WARNING fixes to prevent data loss, harden security, and improve observability.

## Scope

### In Scope
1. **uploadServicios data loss** (CRITICAL) -- `servicios.size` to `rows.size`; mark only deduplicated rows as synced
2. **uploadedVentas missing from FinanzasSyncResult** (CRITICAL) -- add `uploadedVentas: Int = 0` field; pass `ventasUp`
3. **Venta.toRemoto() client clock for createdAt** (WARNING) -- remove `Instant.now()` fallback for `createdAt`, use server-side default
4. **safeUpload swallows 401/403 RLS errors** (WARNING) -- add `RestException` catch for status code check, rethrow auth errors
5. **deleteById/deleteByOrigenId without opticaId filter** (WARNING) -- add `AND opticaId = :opticaId` to both VentaDao DELETE queries
6. **PostgrestException 429 not retried** (WARNING) -- add `RestException` catch in `NetworkRetryHelper.retryNetwork`
7. **No jitter in backoff** (WARNING) -- add `Random.nextLong(0, 200)` to retry delays
8. **emulador.bat hardcoded path** (WARNING) -- use `%LOCALAPPDATA%` instead of hardcoded user path

### Out of Scope
- Duplicacion masiva en sync coordinators (large refactor)
- DeletionSyncHelperTest tests vacios (test rewrite)
- Download sin logica de retry (architectural)
- Chunked batch reporting (observability)
- Offline fallback stale indicator (UX)

## Capabilities

### New Capabilities
None -- all fixes are bug corrections within existing capabilities.

### Modified Capabilities
None -- no spec-level behavior changes. Fixes correct implementation to match existing requirements.

## Approach

All fixes are already applied to `main`. Changes grouped by concern:
- **Data integrity**: UploadSyncCoordinator.kt (dedup tracking), SyncFinanzasDto.kt + SyncFinanzasUseCase.kt (uploadedVentas, createdAt)
- **Security**: VentaDao.kt (opticaId filter), SyncFinanzasUseCase.kt (401/403 rethrow)
- **Resilience**: NetworkRetryHelper.kt (RestException 429, jitter)
- **Dev UX**: emulador.bat (env var path)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `UploadSyncCoordinator.kt` | Modified | `uploadServicios` now returns `rows.size`, marks only dedup rows synced |
| `SyncFinanzasDto.kt` | Modified | Added `uploadedVentas` to `FinanzasSyncResult`; `Venta.toRemoto()` omits `createdAt` for server default |
| `SyncFinanzasUseCase.kt` | Modified | Pass `ventasUp` to result; safeUpload rethrows 401/403 via `RestException` status check |
| `VentaDao.kt` | Modified | Both DELETE queries include `AND opticaId = :opticaId` |
| `NetworkRetryHelper.kt` | Modified | `RestException` catch for 429 retry; `Random.nextLong(0, 200)` jitter |
| `emulador.bat` | Modified | `%LOCALAPPDATA%` replaces hardcoded path |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Missing `uploadedVentas` breaks existing consumers of `FinanzasSyncResult` | Low | Field has default `= 0` -- backward-compatible |
| 401/403 rethrow in safeUpload crashes sync for auth errors | Low | Correct behavior -- RLS errors must propagate, not silently become 0s |

## Rollback Plan

Each fix is scoped to a single file. Revert individual files from git history if needed. No schema changes, no migrations, no data format changes.

## Dependencies

None -- all fixes are self-contained within the Android codebase.

## Success Criteria

- [ ] Build passes (`./gradlew :optoapp:assembleDebug`)
- [ ] All unit tests pass (`./gradlew :optoapp:testDebugUnitTest`)
- [ ] JaCoCo coverage report generates without threshold breach
- [ ] Code review confirms each fix addresses its JD finding
