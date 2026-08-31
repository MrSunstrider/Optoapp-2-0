# Exploration: Backup restore missing updatedAt stamps

## Causal invariant

Restore must leave Room entities with non-null `updatedAt` before the next sync upload. Supabase NOT NULL on pacientes / evaluaciones / dispensaciones / pagos / servicios_extra rejects explicit null (23502).

## Gap

`BackupRestoreCoordinator.restoreBackup` calls `pacienteRepo` / `dispensacionRepo` insert APIs **directly**, bypassing `OptoRepository` which stamps `Instant.now()`.

Backup JSON often has `updatedAt: null` (legacy exports or entities never stamped).

## Related

- fix-montura-movimientos-updated-at-stamp
- fix-sync-updated-at-stamp-gaps
- sync-conflict-rootcause REQ-B1
