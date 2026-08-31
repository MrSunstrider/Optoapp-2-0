# Verify Report: fix-montura-movimientos-updated-at-stamp

**Date:** 2026-08-31  
**Change:** `fix-montura-movimientos-updated-at-stamp`

## Original receipt

- Diagnóstico 2026-08-29 12:06:53, óptica `25af5a92-4a2d-4e7a-957f-61bec87a07d8`
- Error: `23502 null value in column "updated_at" of relation "montura_movimientos"`
- Module: `sync:inventario` / `upload_montura_movimientos` batch

## Requirements verified

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Stamp updatedAt at Room save | PASS | `MonturaInventoryCoordinator.insertMonturaMovimiento` stamps `Instant.now()` |
| registrarSalida uses stamped path | PASS | Calls `insertMonturaMovimiento` instead of direct DAO |
| Legacy backfill migration 47→48 | PASS | `MIGRATION_47_48` + `Migration47To48Test` |
| toRemoto preserves timestamp | PASS | `SyncInventarioUseCaseKtTest.monturaMovimiento_toRemoto_preservesUpdatedAt` |
| Upload batch includes non-null updatedAt | PASS | `SyncInventarioUseCaseUploadTest.uploadMovimientos_batchIncludesNonNullUpdatedAt` |
| No Supabase changes | PASS | Android-only fix |

## Tests

```
./gradlew :optoapp:testDebugUnitTest --tests "*MonturaInventoryCoordinatorStampTest*"
./gradlew :optoapp:testDebugUnitTest --tests "*Migration47To48Test*"
./gradlew :optoapp:testDebugUnitTest --tests "*SyncInventarioUseCase*"
./gradlew :optoapp:testDebugUnitTest --stacktrace
```

All GREEN (full suite).

## Device journey (RDD)

**Manual — pending on physical device:**

1. Registrar entrada manual de inventario
2. Ejecutar sync completa
3. Configuración → Diagnóstico: verificar 0 errores `upload_montura_movimientos`

Expected: POST body carries non-null `updated_at`; no 23502.

## Rollback

Revert `MonturaInventoryCoordinator.kt`, `MIGRATION_47_48`, DB version 48→47. Backfilled timestamps are harmless.

## Regression notes

- Existing PK reconcile tests in `SyncInventarioUseCaseUploadTest` remain PASS
- Sync download path (`SyncSnapshotCoordinator.upsertMonturaMovimiento`) unchanged — preserves server timestamps
