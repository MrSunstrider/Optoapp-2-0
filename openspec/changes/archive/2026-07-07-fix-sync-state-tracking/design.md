# Design: Fix Sync State Tracking

## Technical Approach

Four isolated, pattern-matching fixes to add missing `SyncStateTracker` calls in the finanzas sync pipeline. No new classes, no architecture changes — each fix mirrors an existing 1-3 line pattern already present in the same file.

| Fix | File | Pattern Mirrored |
|-----|------|------------------|
| 1 — `deleteGastoOperativo` markDeleted | `OptoRepository.kt` | `deleteDispensacion` (line 96) |
| 2 — `deleteVentaById` markDeleted | `OptoRepository.kt` | `deleteDispensacionItemById` (line 98) |
| 3 — `uploadVentas` markSynced/markError | `UploadSyncCoordinator.kt` | `uploadGastosOperativos` (line 297) |
| 4 — DeletionSyncHelper entity mappings | `DeletionSyncHelper.kt` | Existing 3-branch `when` block (line 31) |

## Architecture Decisions

### Decision: Add `opticaId` parameter to `deleteVentaById`

**Choice**: Add `opticaId: String` as third parameter, mirroring `deleteDispensacionItemById` signature.
**Alternatives**: Query the Venta entity from Room to extract opticaId before deletion.
**Rationale**: Single caller (`DispensacionViewModel`, line 457) already has `opticaId` in scope. Adding the parameter is simpler, avoids an unnecessary DB read, and matches the established pattern. The existing test characterization in `VentaDaoTest` covers the deletion logic; the new parameter only affects the one caller.

### Decision: Duplicate table-name constants in `DeletionSyncHelper` companion

**Choice**: Add `TABLE_VENTAS`, `TABLE_GASTOS_OPERATIVOS`, `TABLE_DISPENSACION_ITEMS`, `TABLE_ARQUEO_CAJA` as private constants in `DeletionSyncHelper.Companion`.
**Alternatives**: Reference `UploadSyncCoordinator` constants directly.
**Rationale**: `UploadSyncCoordinator` constants are `private` — making them accessible requires a wider change. The existing `DeletionSyncHelper` already duplicates `TABLE_DISPENSACIONES`, `TABLE_PAGOS`, and `TABLE_SERVICIOS` from `UploadSyncCoordinator`. Continuing the duplication pattern is the minimal-change approach.

## Data Flow

```
OptoRepository.deleteGastoOperativo / deleteVentaById
    ↓ Room delete (existing)
    ↓ syncStateTracker.markDeleted(opticaId, entityType, id)  ← NEW
    ↓ postSaveSyncScheduler.scheduleFinanzasSync (existing)

UploadSyncCoordinator.uploadVentas
    ↓ empty check → markSynced(batch) ← NEW
    ↓ chunked upsert via retryNetwork
    ↓ success → markSynced(batch) + per-item markSynced ← NEW
    ↓ IOException/Exception → markError(batch) + rethrow ← NEW

DeletionSyncHelper.pushPendingDeletions
    ↓ when(entityType) {
        "gasto_operativo"    → TABLE_GASTOS_OPERATIVOS    ← NEW
        "venta"              → TABLE_VENTAS               ← NEW
        "dispensacion_item"  → TABLE_DISPENSACION_ITEMS   ← NEW
        "arqueo_caja"        → TABLE_ARQUEO_CAJA          ← NEW
        (existing 3 branches unchanged)
      }
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../data/OptoRepository.kt:260-263` | Modify | Add `syncStateTracker.markDeleted(opticaId, "gasto_operativo", gasto.id)` to `deleteGastoOperativo` |
| `optoapp/.../data/OptoRepository.kt:140` | Modify | Add `opticaId: String` param and `syncStateTracker.markDeleted(opticaId, "venta", id)` to `deleteVentaById` |
| `optoapp/.../domain/UploadSyncCoordinator.kt:329-349` | Modify | Add `markSynced`/`markError` calls to `uploadVentas` matching `uploadGastosOperativos` pattern |
| `optoapp/.../domain/DeletionSyncHelper.kt:26-58` | Modify | Add 4 companion constants + 4 `when` branches |
| `optoapp/.../viewmodel/DispensacionViewModel.kt:457` | Modify | Pass `opticaId` as third arg to updated `deleteVentaById` |
| `optoapp/.../test/.../OptoRepositoryFinanzasTest.kt` | Modify | Add tests verifying `markDeleted` calls for both deletes |
| `optoapp/.../test/.../DeletionSyncHelperTest.kt` | Modify | Add tests for 4 new entity type mappings + unmapped fallthrough |
| `optoapp/.../test/.../UploadSyncCoordinatorVentasTest.kt` | Create | New file: verify `markSynced`/`markError` in `uploadVentas` |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `deleteGastoOperativo` calls `markDeleted` | Robolectric + MockK `coVerify` on `syncStateTracker` mock |
| Unit | `deleteVentaById` calls `markDeleted` | Same pattern |
| Unit | `uploadVentas` markSynced/markError | Robolectric + MockK, mock Supabase to trigger error paths |
| Unit | DeletionSyncHelper entity mapping | Structural tests mirroring existing `DeletionSyncHelperTest` pattern |
| Integration | Existing tests still pass | `./gradlew :optoapp:testDebugUnitTest --stacktrace` |

## Migration / Rollout

No migration required. State tracking entries are ephemeral — safe to add or roll back at any time.

## Open Questions

None. All four fixes have clear mirroring patterns in the codebase.
