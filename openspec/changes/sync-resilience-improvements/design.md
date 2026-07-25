# Design: Sync Resilience Improvements

## Technical Approach

Three independent reversible fixes for sync error visibility: safeUpload propagates errors instead of swallowing, append-only `sync_telemetry_log` preserves history alongside the UPSERT table, and `SyncOrchestrator` feeds per-module errors into `BackgroundErrorCollector`.

## Architecture Decisions

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| 1 | safeUpload error propagation | Retry transient → propagate exhausted. UploadPartialException returns partial count. IOException retried 3x, then thrown. RestException 401/403/409 thrown immediately. Non-auth RestException and generic Exception propagated. | Proposal: "change catch (e: Exception) to only catch UploadPartialException". `invoke()` already returns `Resource.Error` on IOException/Exception. |
| 2 | Retry integration | **Inline retry** in safeUpload (not `NetworkRetryHelper.retryNetwork`). | `retryNetwork` returns `Unit` (blocks return `Int`) and catches `IOException` which would incorrectly swallow `UploadPartialException` (its subclass). |
| 3 | sync_telemetry_log schema | UUID PK, optica_id FK, `stage` as module identifier (no separate `module` column). | Matches spec: `stage` carries module name ("finanzas", "pacientes"). Consistent with `recordRemoteSyncTelemetry(stage=module)`. Index on `(optica_id, created_at DESC)`. |
| 4 | BEC source tags | `"sync:pacientes"`, `"sync:finanzas"`, etc. | Namespaced tags prevent collision with existing "sync"/"auth" tags. |
| 5 | Per-module recording: every error | Record every `Resource.Error` per module per cycle (not just first). | Spec: "SHALL call record() for each module returning Resource.Error". 50-entry cap, ≤8 modules/cycle. |
| 6 | BEC in executeSilentModules | Record to BEC AND invoke existing callback. | Spec requires both paths. Silent sync errors must reach diagnostics. |

## Data Flow

### A: Upload error propagation
```
safeUpload(entityName) { block() }
  ├─ block() success → return count
  ├─ UploadPartialException → return partial count (no retry)
  ├─ IOException → retry 3x with backoff → exhausted? throw
  └─ RestException/Exception → throw
      → invoke() catch → Resource.Error
```

### B: Telemetry dual-write
```
recordRemoteSyncTelemetry(opticaId, status, stage, error)
  ├─ Supabase UPSERT sync_telemetry_optica   (existing)
  └─ Supabase INSERT sync_telemetry_log      (NEW)
       └─ Room insert SyncTelemetryLogEntity (NEW local mirror)

SyncDiagnosticsViewModel
  ├─ remoteTelemetry: sync_telemetry_optica  (unchanged)
  └─ syncHistory: syncTelemetryLogDao.observeByOpticaId() (NEW Flow)
```

### C: Orchestrator → BEC → diagnostics
```
executeModules(opticaId)
  syncPacientesUseCase() → Resource.Error? → bgErrorCollector.record("sync:pacientes", msg)
  syncHistorialUseCase() → Resource.Error? → bgErrorCollector.record("sync:historial", msg)
  ... (8 modules total)

DiagnosticsCard → backgroundErrors Flow (unchanged) + syncHistory Flow (NEW section)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/SyncFinanzasUseCase.kt` | Modify | Inline retry in safeUpload: UploadPartialException returns count; IOException retried 3x; all else propagated |
| `domain/sync/SyncOrchestrator.kt` | Modify | Inject BEC; record per-module errors in executeModules + executeSilentModules |
| `data/SyncTelemetryRemoteRow.kt` | Modify | Add `SyncTelemetryLogEntry` DTO |
| `data/SyncTelemetryLogEntity.kt` | Create | Room entity: id, opticaId, status, stage, errorMessage, createdAt |
| `data/SyncTelemetryLogDao.kt` | Create | DAO: insert, observeByOpticaId (Flow) |
| `data/OptoDatabase.kt` | Modify | Add entity + DAO + MIGRATION_42_43, bump to v43 |
| `di/DatabaseModule.kt` | Modify | Add DAO provider |
| `viewmodel/SyncViewModel.kt` | Modify | Dual-write to sync_telemetry_log in recordRemoteSyncTelemetry |
| `viewmodel/SyncDiagnosticsViewModel.kt` | Modify | Expose syncHistory StateFlow backed by DAO |
| `ui/components/config/ConfigSyncDiagnosticsCard.kt` | Modify | Sync history section (status icon, timestamp, message) |
| `supabase/migrations/20260724XXXXXX.sql` | Create | DDL + RLS (member select/insert) + index on (optica_id, created_at DESC) |

## Interfaces

**Room entity**:
```kotlin
@Entity(tableName = "sync_telemetry_log")
data class SyncTelemetryLogEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val status: String, val stage: String,
    val errorMessage: String, val createdAt: Long,
)
```

**Supabase INSERT** (no DTO class needed — map literal for append-only):
```kotlin
supabase.postgrest["sync_telemetry_log"].insert(mapOf(
    "optica_id" to opticaId, "status" to status,
    "stage" to stage, "error_message" to safeError,
))
```

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit | safeUpload retry/propagation | Mock NetworkRetryHelper, verify UploadPartialException → count, IOException propagates after retries, 401 throws |
| Unit | Orchestrator BEC recording | Mock use cases returning Resource.Error, verify N `record()` calls |
| Unit | SyncTelemetryLogDao | Room in-memory: insert, query ordered by createdAt DESC |
| Integration | Dual-write telemetry | Verify both UPSERT and INSERT calls to Supabase |
| Integration | Diagnostics VM log fetching | Verify Flow emits on DAO insert |

## Migration / Rollout

- **Supabase**: `20260724XXXXXX_create_sync_telemetry_log.sql` — new empty table, no data migration.
- **Room**: `MIGRATION_42_43` — `CREATE TABLE sync_telemetry_log (...)`.
- **Rollback**: Each deliverable independently reversible.

## Open Questions

- [ ] Should safeDownload also propagate errors? Out of scope per proposal, follow-up candidate.
