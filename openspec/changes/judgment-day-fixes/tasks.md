# Tasks: Judgment Day Follow-up Fixes

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~30 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Data Integrity

- [ ] T1 **Fix uploadServicios dedup accounting** — `UploadSyncCoordinator.kt`
  - Return `rows.size` instead of `servicios.size`
  - Change sync-state marking from `servicios.forEach` to `rows.forEach`
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/UploadSyncCoordinator.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

- [ ] T2 **Add uploadedVentas to FinanzasSyncResult** — `SyncFinanzasDto.kt`, `SyncFinanzasUseCase.kt`
  - Add `uploadedVentas: Int = 0` to `FinanzasSyncResult` data class
  - Pass `ventasUp` counter as `uploadedVentas` in result construction
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasDto.kt`, `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasUseCase.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

- [ ] T3 **Remove client-side createdAt fallback** — `SyncFinanzasDto.kt`
  - In `Venta.toRemoto()`, change `createdAt = createdAt ?: Instant.now().toString()` to `createdAt = createdAt` (remove `Instant.now()` fallback)
  - Supabase `DEFAULT now()` handles null
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasDto.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Phase 2: Security

- [ ] T4 **Add 401/403 rethrow in safeUpload** — `SyncFinanzasUseCase.kt`
  - Add `catch (e: RestException)` block before generic `Exception` catch
  - Check `e.statusCode == 401 || e.statusCode == 403` and throw instead of returning 0
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasUseCase.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

- [ ] T5 **Add opticaId filter to VentaDao DELETE queries** — `VentaDao.kt`
  - Add `AND opticaId = :opticaId` to both `deleteById` and `deleteByOrigenId` queries
  - Add `opticaId: String` parameter to both methods
  - **Files**: `optoapp/src/main/java/com/example/optoapp/data/venta/VentaDao.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Phase 3: Resilience

- [ ] T6 **Add RestException catch for 429 retry** — `NetworkRetryHelper.kt`
  - Add `catch (e: RestException)` block in `retryNetwork` alongside existing `IOException` catch
  - Check `isTransientNetworkError(e) || e.statusCode == 429` for retry decision
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/NetworkRetryHelper.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

- [ ] T7 **Add jitter to retry backoff** — `NetworkRetryHelper.kt`
  - Append `+ Random.nextLong(0, 200)` to backoff calculation in both IO and REST catch blocks
  - **Files**: `optoapp/src/main/java/com/example/optoapp/domain/NetworkRetryHelper.kt`
  - **Verify**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Phase 4: Dev UX

- [ ] T8 **Fix emulador.bat hardcoded path** — `emulador.bat`
  - Replace hardcoded user path with `%LOCALAPPDATA%\Android\Sdk`
  - **Files**: `emulador.bat`
  - **Verify**: Manual — inspect script runs without file-not-found

## Phase 5: Verification

- [ ] T9 **Full verification** — project root
  - Run unit tests: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
  - Run debug build: `./gradlew :optoapp:assembleDebug`
  - Confirm JaCoCo report generates: `./gradlew :optoapp:jacocoTestReport`
  - Git diff confirms each fix addresses its JD finding (5 CRITICAL + 3 WARNING)
