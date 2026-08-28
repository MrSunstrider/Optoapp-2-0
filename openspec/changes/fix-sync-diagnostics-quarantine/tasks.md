# Tasks: fix-sync-diagnostics-quarantine

- [x] Inventory PK reconcile (prior change) — tests green
- [x] 1.1 RED `FinanzasUploadValidatorTest` — safeParentBalanceForUpload
- [x] 1.2 RED `UploadSyncCoordinatorTest` — negative net dispensación uploads with monto_pagado=0
- [x] 1.3 GREEN validator + uploadDispensaciones + uploadServicios
- [x] 1.4 Gate targeted unit tests
- [x] 1.5 Full `:optoapp:testDebugUnitTest` — 2223 run; 3 pre-existing failures in `DispensacionViewModelCreateSaveTest` / `DeleteTest` (Dispatchers.Main)
- [x] 1.6 verify-report.md
