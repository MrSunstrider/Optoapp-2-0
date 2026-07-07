# Proposal: Fix Sync State Tracking

## Intent

4 sync state tracking gaps in the finanzas sync pipeline cause remote records to persist forever (deletions don't propagate), deleted records to re-download on every sync, and no visibility into whether venta uploads succeeded. These are correctness bugs in the offline-first sync contract.

## Scope

### In Scope

1. **`deleteGastoOperativo`** — add `syncStateTracker.markDeleted()` call (mirrors `deleteDispensacion` pattern)
2. **`deleteVentaById`** — add `syncStateTracker.markDeleted()` call (mirrors `deleteDispensacionItemById` pattern)
3. **`uploadVentas`** — add `markSynced`/`markError` calls matching every other upload method in `UploadSyncCoordinator` (e.g., `uploadGastosOperativos`)
4. **`DeletionSyncHelper.pushPendingDeletions`** — add `gasto_operativo`, `venta`, `dispensacion_item`, `arqueo_caja` to the entity-type-to-table mapping, with companion constants (mirrors existing 3-entry `when` block)

### Out of Scope

- `uploadArqueos` already lacking `markSynced`/`markError` — deferred (different pattern, no batch marker, no retry helper)
- RLS policy changes — no schema changes needed
- Sync spec changes — behavior only, no spec-level requirement changes

## Capabilities

### New Capabilities
None — pure bugfix, no new product capability.

### Modified Capabilities
None — no spec-level requirement changes. Existing sync behavior is corrected.

## Approach

For each gap:
- **Gaps 1-2**: Add `syncStateTracker.markDeleted(opticaId, entityType, entityId)` after local delete, following the exact pattern from lines 96-98 (`deleteDispensacion`).
- **Gap 3**: Add `markSynced("upload_ventas", "batch")` + per-entity `markSynced("venta", v.id)` on success; add `markError(...)` in IOException/Exception catch blocks; add early-return `markSynced` for empty list.
- **Gap 4**: Add `TABLE_VENTAS`, `TABLE_GASTOS_OPERATIVOS`, `TABLE_DISPENSACION_ITEMS`, `TABLE_ARQUEO_CAJA` companion constants; add corresponding `when` branches using `UploadSyncCoordinator` table names as source of truth.

TDD: write failing tests first (RED), then implement (GREEN), then refactor.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../data/OptoRepository.kt:260-263` | Modified | Add `markDeleted` in `deleteGastoOperativo` |
| `optoapp/.../data/OptoRepository.kt:140` | Modified | Add `markDeleted` in `deleteVentaById` |
| `optoapp/.../domain/UploadSyncCoordinator.kt:329-349` | Modified | Add `markSynced`/`markError` in `uploadVentas` |
| `optoapp/.../domain/DeletionSyncHelper.kt:26-58` | Modified | Add 4 entity-type-to-table mappings |
| `optoapp/.../test/.../OptoRepositoryFinanzasTest.kt` | Modified | Add tests verifying `markDeleted` calls |
| `optoapp/.../test/.../DeletionSyncHelperTest.kt` | Modified | Add tests for new entity type mappings |
| New test file for `UploadSyncCoordinator` uploadVentas | New | Verify `markSynced`/`markError` calls |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `uploadVentas` already handles server-side triggers (migration 20260704000002) — Android "upload" may conflict | Low | Verify `uploadVentas` is NOT skipped server-side; if it is, this fix is still correct (state tracking) |
| `deleteVentaById` takes `origenId` — unclear if `origenId` matches `opticaId` | Low | Both parameters exist; pattern from `deleteDispensacionItemById` uses standalone `opticaId` param |

## Rollback Plan

Revert the 4 file changes. Existing data is not schema-migrated — local state tracking entries are ephemeral and safe to roll back.

## Dependencies

None.

## Success Criteria

- [ ] `deleteGastoOperativo` calls `syncStateTracker.markDeleted`
- [ ] `deleteVentaById` calls `syncStateTracker.markDeleted`
- [ ] `uploadVentas` calls `markSynced` on success and `markError` on failure
- [ ] `DeletionSyncHelper.pushPendingDeletions` maps `gasto_operativo`, `venta`, `dispensacion_item`, `arqueo_caja` to correct Supabase tables
- [ ] All existing tests pass
- [ ] New tests verify each of the 4 fixes
