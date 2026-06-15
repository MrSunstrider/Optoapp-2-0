# Proposal: Sync Conflict Root-Cause Fix

## Intent

Sync conflicts in OptoApp regenerate after every sync cycle. The Supabase trigger fix (migration 20260615) closed one of six independent root causes; the other five remain. This change eliminates phantom conflicts and makes manual resolutions actually stick, so a resolved conflict stays resolved.

## Business Impact

Optometry practices lose trust in the app when conflicts they "resolved" reappear on the next sync. Recurring phantom conflicts force staff to re-resolve the same records, create doubt about whether patient/financial data is correct, and increase support burden. Reliable, durable conflict resolution is core to the SaaS value proposition.

## Scope

### In Scope (P0–P5)
- **RC-3 (P0)** `resolveKeepMine` must upload the local version after clearing the conflict record — `SyncViewModel.kt:103-115`
- **RC-4 (P3)** `acceptAllCloud` must clear `sync_entity_state` conflicted rows, not just `conflict_records` — `SyncViewModel.kt:154-163`, `SyncStateTracker.kt:55-66`, new `deleteConflictedForOptica` in `SyncEntityStateDao`
- **RC-1 (P1)** Set `updatedAt` on every Room entity save to kill the `null → Instant.now()` drift — `SyncPacientesUseCase.kt:254`, `SyncHistorialDto.kt:275`, `SyncFinanzasDto.kt:198/211/238`, `SyncInventarioUseCase.kt:268`
- **RC-2 (P2)** Silent sync must write server timestamps back to Room after upload — `SyncViewModel.kt:310-334`
- **RC-5 (P4)** Make `cancelPending()` suspend/awaitable to close the race with `performFullDownload` — `PostSaveSyncScheduler.kt:65-70`
- **RC-6 (P5)** Add `filterConflicts` to `uploadPagos` — `UploadSyncCoordinator.kt:259-289`

### Out of Scope
- **RC-6 (P6, backlog)** Schema change adding `updated_at` to `dispensacion_items` and `montura_movimientos` — `SyncFinanzasDto.kt:126-160`. Deferred: requires Room migration + Supabase column change.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `sync-conflict-resolution`: resolution and silent-sync requirements change — resolving a conflict must upload/download as appropriate, `acceptAllCloud` must clear all conflict state, and silent sync must preserve server timestamps. (If no existing spec name matches, sdd-spec creates `sync-conflict-resolution`.)

## Approach

| RC | Fix |
|----|-----|
| RC-3 | After deleting the conflict record, trigger an upload of the local entity so local wins durably. |
| RC-4 | Add `deleteConflictedForOptica` to `SyncEntityStateDao`; `acceptAllCloud` calls it alongside `clearConflicts()`. |
| RC-1 | Stamp `updatedAt = Instant.now()` at save time in Room, not at `toRemoto()` time — single source of truth. |
| RC-2 | In `performSilentSync`, write the server-confirmed `updatedAt` back into Room after a successful upload. |
| RC-5 | Convert `cancelPending()` to a suspend function that awaits cancellation before `performFullDownload` proceeds. |
| RC-6 | Call `filterConflicts` inside `uploadPagos` before pushing pagos. |

## Delivery

Multi-fix change delivered as chained PRs (`auto-chain`, `stacked-to-main`):

- **PR-A** — RC-3 + RC-4: user-facing fixes (resolutions stick, acceptAllCloud fully clears state).
- **PR-B** — RC-1 + RC-2: timestamp infrastructure (eliminate drift, persist server timestamps).
- **PR-C** — RC-5 + RC-6: hardening (cancelPending race, pagos conflict check).

Each PR is autonomous, independently verifiable, and rollback-safe.

## TDD Contract (tests written BEFORE the fix)

- **RC-3**: `resolveKeepMine` triggers an upload of the local version after clearing the conflict record.
- **RC-4**: `acceptAllCloud` clears BOTH `conflict_records` AND `sync_entity_state` conflicted rows.
- **RC-1**: null `updatedAt` does not regenerate across multiple sync cycles (stable timestamp).
- **RC-2**: silent sync persists the server timestamp; no stale timestamp across cycles.
- **RC-5**: `cancelPending` completes before `performFullDownload` runs (no race re-creating conflicts).
- **RC-6**: pagos upload without a conflict check is caught — `uploadPagos` calls `filterConflicts` and does not silently overwrite remote.
- **Roundtrip**: full upload→download roundtrip preserves timestamps.
- **resolveAcceptTheirs**: triggers a download after resolution.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `SyncViewModel.kt` | Modified | resolveKeepMine upload, acceptAllCloud clears state, silent sync timestamps |
| `SyncStateTracker.kt` / `SyncEntityStateDao` | Modified | new `deleteConflictedForOptica` |
| `SyncPacientesUseCase.kt`, `SyncHistorialDto.kt`, `SyncFinanzasDto.kt`, `SyncInventarioUseCase.kt` | Modified | stamp `updatedAt` at save time |
| `PostSaveSyncScheduler.kt` | Modified | `cancelPending()` becomes suspend/awaitable |
| `UploadSyncCoordinator.kt` | Modified | `filterConflicts` in `uploadPagos` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Timestamp comparison edge cases (timezone, precision) | Med | Compare in UTC `Instant`; assert precision in roundtrip tests |
| Room migration if entities gain NOT NULL columns (RC-1) | Low | Stamp in code, not schema; avoid NOT NULL column adds in P1 |
| RC-5 suspend change alters call sites / blocks UI | Med | Audit `cancelPending` callers; keep cancellation off the main dispatcher |
| RC-3 upload after resolution loops if remote still newer | Low | Upload sets local as authoritative; covered by roundtrip + resolution tests |

## Rollback Plan

Each RC ships in its own chained PR with isolated commits. Revert per PR (PR-C → PR-B → PR-A) to roll back independently. No data migration in scope (P6 deferred), so no destructive schema rollback is needed.

## Dependencies

- Supabase trigger migration 20260615 (already applied) — prerequisite for RC-level fixes.

## Success Criteria

- [ ] Resolving a conflict via "keep mine" does NOT recreate the conflict on the next sync.
- [ ] `acceptAllCloud` leaves zero `conflicted` rows in `sync_entity_state`.
- [ ] An entity with null `updatedAt` keeps a stable timestamp across ≥3 silent sync cycles.
- [ ] Silent sync stores the server-confirmed timestamp in Room after upload.
- [ ] No conflict is re-created by a debounced post-save job racing a full download.
- [ ] `uploadPagos` runs through `filterConflicts` and never silently overwrites remote pagos.
- [ ] All TDD-contract tests pass; each written and failing before its fix.
