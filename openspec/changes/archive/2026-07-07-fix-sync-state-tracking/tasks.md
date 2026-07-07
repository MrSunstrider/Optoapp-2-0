# Tasks: Fix Sync State Tracking

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 80–120 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Tests

- [x] 1.1 `OptoRepositoryFinanzasTest` — add test `deleteGastoOperativo_calls_markDeleted`: insert via DAO, call `deleteGastoOperativo`, `coVerify` `syncStateTracker.markDeleted(opticaId, "gasto_operativo", id)`
- [x] 1.2 `OptoRepositoryFinanzasTest` — add test `deleteVentaById_calls_markDeleted`: insert via DAO, call `deleteVentaById(id, origenId, opticaId)`, `coVerify` `syncStateTracker.markDeleted(opticaId, "venta", id)`
- [x] 1.3 Create `UploadSyncCoordinatorVentasTest.kt` — test `uploadVentas_markSynced_on_success` (verify batch + per-item `markSynced`), `uploadVentas_markError_on_ioexception`, `uploadVentas_markError_on_exception`, `uploadVentas_markSynced_on_empty`
- [x] 1.4 `DeletionSyncHelperTest` — add tests for 4 new entity type mappings (`gasto_operativo -> gastos_operativos`, `venta -> ventas`, `dispensacion_item -> dispensacion_items`, `arqueo_caja -> arqueo_caja`) and `unmapped_entityType_clearsDeletionState`

## Phase 2: GREEN — Implement Fixes

- [x] 2.1 `OptoRepository.kt` — add `syncStateTracker.markDeleted(gasto.opticaId, "gasto_operativo", gasto.id)` to `deleteGastoOperativo` between the DAO delete and the `scheduleFinanzasSync` call
- [x] 2.2 `OptoRepository.kt` — add `opticaId: String` param to `deleteVentaById`; add `syncStateTracker.markDeleted(opticaId, "venta", id)` after both DAO deletes
- [x] 2.3 `DispensacionViewModel.kt` — pass `opticaId` as third arg to `deleteVentaById` call
- [x] 2.4 `UploadSyncCoordinator.kt` — add markSynced/markError to `uploadVentas` matching `uploadGastosOperativos` pattern: empty-early-return markSynced, IOException catch→markError→rethrow, Exception catch→markError→rethrow, post-success batch+per-item markSynced
- [x] 2.5 `DeletionSyncHelper.kt` — add `TABLE_GASTOS_OPERATIVOS`, `TABLE_VENTAS`, `TABLE_DISPENSACION_ITEMS`, `TABLE_ARQUEO_CAJA` companion constants; add 4 corresponding `when` branches for entity type mapping

## Phase 3: REFACTOR — Verify

- [x] 3.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all 8 new tests pass, all existing tests pass
- [x] 3.2 Remove any debug logging or commented-out code; verify code style matches project conventions
