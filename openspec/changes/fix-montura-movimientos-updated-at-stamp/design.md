# Design: montura_movimientos updatedAt stamp

## Seam

```
call sites → insertMonturaMovimiento(mov)
           → copy(updatedAt = Instant.now().toString())
           → monturaMovimientoDao.insertMovimiento(stamped)
           → scheduleInventarioSync
```

`registrarSalida()` calls `insertMonturaMovimiento(movimiento)` instead of direct DAO insert.

Pattern matches `insertMontura()` / `updateMontura()` in same class.

## Migration 47→48

```sql
UPDATE montura_movimientos
SET updatedAt = COALESCE(updatedAt, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
WHERE updatedAt IS NULL;
```

## Sync download path unchanged

`SyncSnapshotCoordinator.upsertMonturaMovimiento` preserves server timestamps (no stamp).

## Rollback

Revert two production files + migration registration.
