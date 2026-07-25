# Tasks: Sync Resilience Improvements

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~340 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: single-pr
400-line budget risk: Low

All three blocks are independent. Order tasks by dependencies within each block.

---

## Block A — safeUpload error propagation (independent)

**Test command**: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

- [x] **A.1 (test-only)** — Add test scenarios to `SyncFinanzasUseCaseKtTest`: full network failure propagates IOException, UploadPartialException returns partial count, 401/403/409 RestException thrown immediately, non-auth RestException propagated. **RED phase.**
- [x] **A.2 (impl)** — Modify `SyncFinanzasUseCase.safeUpload`: inline retry for IOException (3 attempts, 1s/2s/4s backoff), then throw after exhaustion; propagate RestException (all codes) and generic Exception instead of swallowing. `UploadPartialException` returns partial count (unchanged). Depends on A.1.

## Block B — sync_telemetry_log table

- [x] **B.1 (impl, no test)** — Create `supabase/migrations/20260724200000_create_sync_telemetry_log.sql`: DDL (UUID PK, optica_id FK, status, stage, error_message, created_at), index on (optica_id, created_at DESC), RLS policies (member SELECT, member INSERT).
- [x] **B.2 (test-only)** — Create `SyncTelemetryLogDaoTest` (Room in-memory, Robolectric): insert row, observeByOpticaId returns rows ordered by createdAt DESC. **RED phase.**
- [x] **B.3 (impl)** — Create `SyncTelemetryLogEntity.kt` + `SyncTelemetryLogDao.kt` (insert, observeByOpticaId Flow). Update `OptoDatabase` (add entity, add DAO, v42→v43, MIGRATION_42_43). Add DAO provider in `DatabaseModule.kt`. Depends on B.2.
- [x] **B.4 (test-only)** — Write tests for `SyncViewModel.recordRemoteSyncTelemetry` dual-write: verify UPSERT to sync_telemetry_optica AND INSERT to sync_telemetry_log AND local Room insert. **RED phase.** Depends on B.3.
- [x] **B.5 (impl)** — Modify `SyncViewModel.recordRemoteSyncTelemetry`: after existing UPSERT, INSERT into `sync_telemetry_log` (Supabase) + insert `SyncTelemetryLogEntity` locally via DAO. Depends on B.4.
- [x] **B.6 (test-only)** — Write tests for `SyncDiagnosticsViewModel.syncHistory` Flow: verify Flow emits entries matching DAO content. **RED phase.** Depends on B.3.
- [x] **B.7 (impl)** — Modify `SyncDiagnosticsViewModel.kt`: expose `syncHistory` StateFlow backed by `SyncTelemetryLogDao.observeByOpticaId`. Modify `ConfigSyncDiagnosticsCard.kt`: add sync history section (status icon, timestamp, error message). Depends on B.6.

## Block C — Orchestrator BEC recording (independent)

- [x] **C.1 (test-only)** — Add test scenarios to `SyncOrchestratorTest`: `executeModules` calls BEC.record for each Resource.Error module; `executeSilentModules` calls BEC.record AND existing callback. **RED phase.**
- [x] **C.2 (impl)** — Inject `BackgroundErrorCollector` into `SyncOrchestrator` constructor. In `executeModules()`: call `bgErrorCollector.record("sync:MODULE", msg)` for each Resource.Error. In `executeSilentModules()`: same recording in addition to existing `onModuleResult` callback. Depends on C.1.

---

## Summary

| Block | Test tasks | Impl tasks | Total |
|-------|-----------|------------|-------|
| A | 1 | 1 | 2 |
| B | 3 | 4 | 7 |
| C | 1 | 1 | 2 |
| **Total** | **5** | **6** | **11** |

**Execution order**: B.1 can start immediately. A.1→A.2 and C.1→C.2 are independent from B. Within B: B.1→B.2→B.3→B.4→B.5→B.6→B.7 (interleaved RED/GREEN). All blocks can be worked in parallel.
