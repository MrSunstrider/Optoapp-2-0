# Exploration: Sync Resilience Improvements

**Change**: `sync-resilience-improvements`
**Date**: 2026-07-23

## Current State

### Sync Orchestrator (`SyncOrchestrator.kt`)
- Executes 8 modules sequentially: pacientes → historial → finanzas → proveedores → ordenes_compra → inventory_kpis → inventario → inventario_fisico
- Each module returns `Resource<T>` — orchestrator checks `is Resource.Error` and sets `hasErrors = true`
- **Errors are ONLY logged via `Log.w()`** — no per-module error recording to telemetry or diagnostics
- Result: a single boolean `hasErrors` — no visibility into WHICH module failed
- **Two execution paths**:
  - `executeModules()` — full sync, returns `Boolean hasErrors`
  - `executeSilentModules()` — silent sync, delivers per-module results via `onModuleResult` callback (already has per-module granularity!)

### SyncFinanzasUseCase — The Silent Swallower

**Critical bug**: `SyncFinanzasUseCase.invoke()` **NEVER returns `Resource.Error`** — always returns `Resource.Success` because:
1. `safeUpload()` catches ALL exceptions (except CancellationException, 401/403/409 RestException) and returns `0`
2. `safeDownload()` catches ALL exceptions (except CancellationException) and returns `0`
3. The `invoke()` body wraps everything in a try/catch that only catches exceptions escaping `safeUpload`/`safeDownload` — but they never escape
4. Result: even when ALL network calls fail, the orchestrator sees `Resource.Success(FinanzasSyncResult(all_zeros))`

This means:
- The orchestrator's `finanzas` check at line 60-64 of SyncOrchestrator.kt is dead code — `f` is ALWAYS `Resource.Success`
- `SyncFinanzasUseCase` has its own internal per-entity error tracking via `SyncStateTracker.markError()`, but those errors NEVER propagate to the orchestrator or to telemetry
- `UploadPartialException` (for partial uploads) IS caught and converted to a count, but full failures are silently swallowed

### Remote Telemetry (`sync_telemetry_optica` table)

Schema (from migration `20260427054500`):
```sql
create table public.sync_telemetry_optica (
  optica_id text primary key references public.opticas(id) on delete cascade,
  last_sync_at timestamptz,
  last_status text not null default 'idle' check (last_status in ('idle', 'ok', 'error')),
  last_stage text not null default '',
  last_error text not null default '',
  last_actor uuid references auth.users(id) on delete set null,
  updated_at timestamptz not null default timezone('utc', now())
);
```

- **UPSERT by `optica_id`** — each sync cycle overwrites the single row
- No sync history preserved — if a sync fails then succeeds, the error is lost forever
- Only 4 status values: `idle`, `ok`, `error` — not module-specific
- Written from two places in `SyncViewModel`:
  - `performFullSync()` — writes `"finalizado"` stage with `"ok"` or `"error"` status
  - `performSilentSync()` — writes per-module stage with error messages during `executeSilentModules`
  - Silent sync overwrites the full sync's telemetry (both write the same optica_id row)

### Local Telemetry (`SyncTelemetry` — DataStore)
- Stores only two values: `sync_last_full_success_ms` (Long) and `sync_last_full_error` (String)
- `recordFullSyncSuccess()` removes the error key
- `recordFullSyncError()` overwrites the error — only last error retained

### BackgroundErrorCollector
- In-memory singleton, max 50 events, oldest discarded
- Currently called from `SyncViewModel` after full sync completes (records "Full sync completada con errores")
- No per-module error recording from the orchestrator

### SyncStateTracker (Room)
- Per-entity state: `synced`/`error`/`deleted`/`conflicted` per `(optica_id, entity_type, entity_id)`
- Used extensively by `SyncFinanzasUseCase`, `UploadSyncCoordinator`, `DownloadSyncCoordinator` for per-entity error tracking
- **Already captures per-entity granularity** — but it's entity-level, not module-level

### Error propagation by module (verified in code):

| UseCase | Upload error behavior | Download error behavior | Propagates to orchestrator? |
|---|---|---|---|
| SyncPacientesUseCase | `throw` → caught in invoke → `Resource.Error` | swallow per-row via SyncStateTracker | ✅ YES |
| SyncHistorialUseCase | `throw` → caught in invoke → `Resource.Error` | swallow per-row via SyncStateTracker | ✅ YES |
| SyncFinanzasUseCase | `safeUpload` returns 0 (swallowed) | `safeDownload` returns 0 (swallowed) | ❌ NEVER |
| SyncProveedoresUseCase | `throw` → caught in invoke → `Resource.Error` | swallow per-row via SyncStateTracker | ✅ YES |
| SyncOrdenesCompraUseCase | no explicit try/catch on upload | swallow per-row | ✅ (if exception reaches invoke) |
| SyncInventarioUseCase | `throw` → caught in invoke → `Resource.Error` | swallow per-row via SyncStateTracker | ✅ YES |
| SyncInventarioFisicoUseCase | `throw` → caught in invoke → `Resource.Error` | swallow per-row via SyncStateTracker | ✅ YES |
| SyncInventoryKpisUseCase | N/A | N/A | N/A (download only) |

## Affected Areas

### Android (Kotlin)
- `optoapp/.../domain/sync/SyncOrchestrator.kt` — error propagation, retry, per-module error recording
- `optoapp/.../domain/SyncFinanzasUseCase.kt` — `safeUpload`/`safeDownload` error swallowing
- `optoapp/.../domain/UploadSyncCoordinator.kt` — `executeSimpleUpsert` error handling (throws correctly)
- `optoapp/.../domain/DownloadSyncCoordinator.kt` — download error handling (throws correctly, catches in downloadTable)
- `optoapp/.../domain/NetworkRetryHelper.kt` — existing retry with exponential backoff (used for chunk-level retry)
- `optoapp/.../data/SyncTelemetry.kt` — DataStore local telemetry (overwrites)
- `optoapp/.../data/SyncTelemetryRemoteRow.kt` — remote telemetry DTO
- `optoapp/.../data/SyncEntityStateDao.kt` — Room entity state DAO (may need module-level queries)
- `optoapp/.../viewmodel/SyncViewModel.kt` — telemetry recording at lines 348, 352, 391, 395, 423, 427, 440-458
- `optoapp/.../viewmodel/SyncDiagnosticsViewModel.kt` — diagnostics VM reads remote telemetry
- `optoapp/.../ui/components/config/ConfigSyncDiagnosticsCard.kt` — diagnostic UI that shows remote telemetry

### Supabase (PostgreSQL)
- `sync_telemetry_optica` table — currently UPSERT-only (no history)
- New migration for `sync_telemetry_log` table (append-only history)

## Approaches

### 1. Minimal — Fix Telemetry + Finanzas Only

**Description**: Fix the two highest-impact gaps with minimal changes:
- Make remote telemetry append-only by adding a `sync_telemetry_log` table
- Fix `safeUpload`/`safeDownload` to throw on non-transient errors (let errors propagate to orchestrator)
- Add per-module error recording to `SyncDiagnosticsViewModel` via the orchestrator's existing callback path

**Files changed**:
- `SyncFinanzasUseCase.kt` — modify `safeUpload`/`safeDownload` to throw instead of returning 0 on non-transient errors
- New Supabase migration for `sync_telemetry_log` table
- `SyncViewModel.kt` — update `recordRemoteSyncTelemetry` to also insert into `sync_telemetry_log`
- `SyncTelemetryRemoteRow.kt` — add log entry DTO
- `SyncDiagnosticsViewModel.kt` — add ability to fetch sync history
- `ConfigSyncDiagnosticsCard.kt` — show sync history in diagnostics card

**Pros**:
- Smallest change set, lowest risk
- Fixes the critical Finanzas silent-fail bug immediately
- Preserves full error history

**Cons**:
- No per-module retry (existing `NetworkRetryHelper` remains at chunk level only)
- No local telemetry history beyond DataStore
- Silent sync already does per-module telemetry writing, but full sync still only records one aggregate entry

**Effort**: Low-Medium

### 2. Full — Orchestrator-Level Retry + Module Error Aggregation

**Description**: All of Approach 1, plus:
- Add per-module retry with exponential backoff in `SyncOrchestrator.executeModules()`
- Return structured error results (not just `Boolean`) including which modules failed and why
- Record per-module results directly to `BackgroundErrorCollector` from the orchestrator
- Create a `SyncModuleResult` sealed class to track module-level outcomes
- Add a new Room table or DataStore preference for sync history (failed syncs log)

**Files changed**: All files from Approach 1, plus:
- `SyncOrchestrator.kt` — per-module retry, structured error return, BackgroundErrorCollector integration
- New Room entities for sync session history
- `SyncViewModel.kt` — consume structured results, pass to telemetry
- `BackgroundErrorCollector.kt` — may need source categorization

**Pros**:
- Comprehensive resilience improvement
- Retry handles transient failures without user-facing error
- Full visibility into sync health per module
- Structured result enables better diagnostics UI

**Cons**:
- Larger change (more risk, more testing)
- May need to rework `executeSilentModules` and `executeModules` contract
- Room entity migration needed for local sync history
- Higher coupling between orchestrator and telemetry

**Effort**: High

### 3. Surgical — Fix Finanzas + Enable Telemetry History

**Description**: Fixes the two proven bugs (Finanzas swallowing + UPSORT telemetry overwrite) plus enables per-module error routing through existing infrastructure without adding orchestrator retry. Retry is deferred to a follow-up change.

**Key decisions**:
- `safeUpload` in SyncFinanzasUseCase: throw `IOException` instead of returning 0 for network errors (keep 401/403/409 propagation as-is). Download errors already propagate correctly in other use cases.
- New `sync_telemetry_log` table: append-only, indexed by `optica_id + created_at`. The existing `sync_telemetry_optica` table stays as "latest state" quick-lookup.
- Per-module errors: route through the existing `executeSilentModules` callback path. For full sync (`executeModules`), add a mutable error list and pass it to `BackgroundErrorCollector` inside the orchestrator.
- Local telemetry (`SyncTelemetry` DataStore): replace single-error storage with a small bounded list of recent sync results.

**Files changed**:
- `SyncFinanzasUseCase.kt` — fix `safeUpload` to throw on non-transient failures
- Supabase migration: `CREATE TABLE sync_telemetry_log`
- `SyncTelemetryRemoteRow.kt` — add `SyncTelemetryLogEntry` DTO
- `SyncViewModel.kt` — `recordRemoteSyncTelemetry` inserts into both tables
- `SyncOrchestrator.kt` — add `BackgroundErrorCollector` injection, record per-module errors
- `SyncTelemetry.kt` — replace single error with bounded list
- `SyncDiagnosticsViewModel.kt` — fetch and expose sync history
- `ConfigSyncDiagnosticsCard.kt` — show history section
- Tests for all new behavior

**Pros**:
- Fixes the two critical bugs that directly cause the "sincronización parcial" error
- Keeps orchestrator changes minimal (no retry logic complexity)
- Leverages existing infrastructure (`BackgroundErrorCollector`, `SyncStateTracker`)
- Backward compatible — `sync_telemetry_optica` table unchanged
- Per-module errors become visible in diagnostics immediately

**Cons**:
- No per-module retry (transient errors still cause partial sync)
- `SyncStateTracker` entity errors will accumulate dirty state without a retry mechanism
- Silent sync already records per-module telemetry; full sync still shows "finalizado" stage

**Effort**: Medium

## Recommendation

**Approach 3 (Surgical)** is recommended for the following reasons:

1. **Root cause addressed**: The "sincronización parcial" complaint is directly caused by `SyncFinanzasUseCase` swallowing errors AND the telemetry overwrite. Both are fixable surgically.
2. **History preserved**: `sync_telemetry_log` gives us append-only sync history — the single most requested operational insight.
3. **Orchestrator change is minimal**: Adding `BackgroundErrorCollector` to the orchestrator is a single-DI change with no contract breakage.
4. **Per-module retry is a separable concern**: Retry logic is complex (transient vs permanent errors, idempotency guarantees, timeout management). Adding it in this change would bloat scope and risk. It should be its own follow-up.
5. **Existing patterns are reused**: `BackgroundErrorCollector` already has max-50 cap and thread-safe recording. `SyncStateTracker` already tracks entity-level errors. No new infrastructure needed.

### What this change WILL do:
- Fix `safeUpload` to propagate errors (not silently return 0)
- Create `sync_telemetry_log` table for append-only sync history
- Record per-module errors from orchestrator to `BackgroundErrorCollector`
- Expose sync history in `SyncDiagnosticsViewModel`
- Show sync history in the diagnostics card
- Write tests for all changes (strict TDD)

### What this change will NOT do:
- Add per-module retry with exponential backoff (deferred to follow-up)
- Replace the existing `sync_telemetry_optica` table (it stays as "latest state")
- Add Room migration for local sync history
- Rewrite the orchestrator's return type

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Fixing `safeUpload` breaks existing partial-upload flow | Low | `UploadPartialException` is preserved — still caught and returns partial count. Only full failures (IOException without partial progress) now throw. |
| New `sync_telemetry_log` table adds storage cost | Low | Each sync cycle adds ~1 row per optica. At ~2KB/row, 1000 opticas × 10 syncs/day × 365 days = ~7GB/year. Add a TTL or cleanup policy. |
| `BackgroundErrorCollector` already at 50-cap may lose errors | Low | Per-module errors are batched; orchestrator records at most 8 per sync cycle. 50 cap is sufficient. |
| Existing tests fail after `safeUpload` behavior change | Medium | Must fix tests in `SyncFinanzasUseCaseTest` before implementation (TDD). Tests that assert zero-count on failure need updating. |
| Silent sync and full sync write conflicting telemetry | Low | Both write to same `sync_telemetry_optica` UPSERT — resolved by the log table being append-only. Both writes go to the log. |

## Overlap with `sync-conflict-egress-fix-v2`

**None detected.** `sync-conflict-egress-fix-v2` addresses:
- ID-scoped conflict checking (RC-1)
- Removal of pacientes cascade in `PostSaveSyncScheduler` (RC-2)
- Auto-clear of stale conflict records (RC-3)

All three are about egress reduction and stale conflict drainage — unrelated to error propagation, retry, or telemetry history.

## Ready for Proposal

**Yes.** The exploration identified the critical gaps, verified them against real code, and proposed a surgical fix that addresses the user's "sincronización parcial" errors without over-engineering.

**Key findings for the proposer**:
1. `SyncFinanzasUseCase.safeUpload` always returns 0 on failure — this IS the root cause of masked errors.
2. `sync_telemetry_optica` is a single-row UPSERT — no sync history exists.
3. The orchestrator's `executeModules` logs per-module errors via `Log.w()` but never records them to any observable sink.
4. Per-module retry is desirable but separable — recommend deferring.
5. The existing `executeSilentModules` callback is a good model for how to route per-module results.
