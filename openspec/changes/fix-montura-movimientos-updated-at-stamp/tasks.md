# Tasks: fix-montura-movimientos-updated-at-stamp

Strict TDD. Budget ≤ 400 lines.

- [x] SDD explore/propose/spec/design artifacts
- [x] T1 RED `MonturaInventoryCoordinatorStampTest`
- [x] T2 GREEN stamp in `insertMonturaMovimiento` + `registrarSalida` route
- [x] T3 `SyncInventarioUseCaseKtTest.monturaMovimiento_toRemoto_preservesUpdatedAt`
- [x] T4 `SyncInventarioUseCaseUploadTest.uploadMovimientos_batchIncludesNonNullUpdatedAt`
- [x] T5 RED `Migration47To48Test`
- [x] T6 GREEN `MIGRATION_47_48` + DB version 48 + schema 48.json
- [x] T7 Gate targeted sync/inventario tests — PASS
- [x] T8 Gate full `testDebugUnitTest` — PASS
- [x] T9 verify-report.md
- [ ] T10 Device journey (manual RDD receipt)
- [ ] T11 Archive post-merge
