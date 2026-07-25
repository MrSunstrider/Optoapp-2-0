# Proposal: Sync Resilience Improvements

**Change**: `sync-resilience-improvements`
**Date**: 2026-07-23
**TDD mode**: STRICT

## Intent

Sync failures are invisible. `SyncFinanzasUseCase.safeUpload` swallows ALL exceptions returning 0 — orchestrator never sees finanzas failures, masking "sincronización parcial". Remote telemetry is a single-row UPSERT — error history lost on next success. Per-module errors logged to `Log.w` only, never reach diagnostics.

## Scope

### In Scope
- Fix `safeUpload` — propagate `IOException`. Keep `UploadPartialException` catch. Preserve 401/403/409.
- Create `sync_telemetry_log` table (append-only). `sync_telemetry_optica` unchanged.
- Supabase migration (DDL + RLS) + Android DTO.
- Connect `BackgroundErrorCollector` to orchestrator — per-module errors in `executeModules()`.
- Sync history in diagnostics VM + card.
- Tests for all (TDD).

### Out of Scope
- Per-module retry (follow-up), replace `sync_telemetry_optica`, local sync history, orchestrator return type rewrite, UI redesign.

## Capabilities

### New Capabilities
- `sync-resilience`: Sync error propagation and telemetry history. Finanzas MUST propagate errors. Telemetry MUST be append-only. Per-module errors MUST reach `BackgroundErrorCollector`.

### Modified Capabilities
None.

## Approach

Three independent reversible fixes:

1. **safeUpload** — change `catch (e: Exception)` to only catch `UploadPartialException`. Non-fatal exceptions propagate → `Resource.Error`.
2. **Migration** — `CREATE TABLE sync_telemetry_log (id bigint PK, optica_id FK, status text, stage text, module text, error_message text, created_at timestamptz)`. RLS: member select, service_role insert.
3. **Orchestrator** — inject `BackgroundErrorCollector`; after each module returns `Resource.Error` → `collector.record("Sync", "Error en ${moduleName}: ...")`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `SyncFinanzasUseCase.kt` | Modified | `safeUpload` throws instead of 0 |
| `SyncOrchestrator.kt` | Modified | Inject BEC, record per-module errors |
| `SyncTelemetryRemoteRow.kt` | Modified | Add `SyncTelemetryLogEntry` DTO |
| `SyncDiagnosticsViewModel.kt` | Modified | Fetch sync log |
| `ConfigSyncDiagnosticsCard.kt` | Modified | Sync history section |
| `supabase/migrations/` | New | Sync telemetry log migration |
| Test files | New/Modified | TDD — RED first |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| safeUpload breaks partial-upload | Low | `UploadPartialException` preserved |
| new table storage cost | Low | ~1 row per sync per optica. TTL if needed. |
| BEC 50-cap overflow | Low | ≤8 modules/cycle. 50 entries sufficient. |
| existing tests expect zero-count on failure | Medium | TDD: update tests first |

## Rollback Plan

Each fix independently reversible: revert `SyncFinanzasUseCase.kt` (catch-all), drop `sync_telemetry_log` + revert DTO/UI, revert orchestrator DI. No cross-cascade.

## Dependencies

- Supabase access for migration
- Hilt DI (BEC singleton)

## Success Criteria

- [ ] `SyncFinanzasUseCase.invoke()` returns `Resource.Error` on network failure
- [ ] `sync_telemetry_log` stores append-only rows per sync cycle
- [ ] Orchestrator records per-module errors to `BackgroundErrorCollector`
- [ ] Diagnostics card shows sync history from log
- [ ] All existing tests pass
