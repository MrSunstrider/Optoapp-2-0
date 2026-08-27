# Tasks: Fix inventory movement PK reconcile

Budget: ≤ 400 authored lines. Strict TDD.

- [x] 1.1 RED `MovimientoUploadPartitionTest` — reconcile / upload-new / same-id
- [x] 1.2 RED `SyncInventarioUseCaseUploadTest` — skip POST on id mismatch, POST when new, skip conflicted
- [x] 1.3 GREEN `ConflictHelper.partitionMovimientosForUpload` + `MovimientoUploadPlan`
- [x] 1.4 GREEN `uploadMovimientos` reconcile + `onConflict` + transaction seam
- [x] 1.5 Gate: targeted `testDebugUnitTest` green (partition, upload, persistence)
- [x] 1.6 Full `:optoapp:testDebugUnitTest` — 2215 tests; 3 failures in `DispensacionViewModel*` (Dispatchers.Main leftover), not in this change
- [x] 1.7 GGA R1–R4 Round 1 BLOCKED → fixes applied (delete local PK, fail-closed fetch, drop composite `onConflict`)
- [x] 1.8 Judgment Day Round 1 confirmed CRITICALs fixed; Round 2 re-judge launched
