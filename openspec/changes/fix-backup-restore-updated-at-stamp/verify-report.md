# Verify Report: fix-backup-restore-updated-at-stamp

**Date:** 2026-08-31

## Causal receipt

Restore bypassed `OptoRepository` stamps → Room rows with null `updatedAt` → next sync upload could hit PostgreSQL 23502 on pacientes / evaluaciones / dispensaciones / pagos / servicios_extra.

## Fix

`BackupRestoreCoordinator.withDefaults()` now sets `updatedAt = Instant.now().toString()` for all five backup entity types before DAO insert.

## Tests

| Test | Result |
|------|--------|
| `BackupRestoreCoordinatorStampTest.restoreBackup_pacienteWithoutUpdatedAt_isStamped` | PASS |
| `BackupRestoreCoordinatorStampTest.restoreBackup_allSyncEntitiesWithoutUpdatedAt_areStamped` | PASS |
| Full `:optoapp:testDebugUnitTest` | PASS |

## RDD journey (manual)

1. Export backup with legacy null timestamps (or clear updatedAt in JSON)
2. Restore into óptica
3. Full sync → diagnóstico: no 23502 on pacientes/evaluaciones/dispensaciones/pagos/servicios

## Family of stamp fixes (this session)

1. montura_movimientos insert + migration 47→48
2. monturas adjustStock + migration 48→49 + proveedores insert
3. **backup restore withDefaults** (this change)
