# Design: backup restore updatedAt stamp

## Seam

Private `withDefaults()` helpers already normalize restore entities. Extend each to set `updatedAt = Instant.now().toString()` so every restore insert is stamped in one place.

## Flow

```
backup JSON → withDefaults() stamps updatedAt → repo.insert* → schedule sync
```

## Why not route through OptoRepository

Coordinator already owns clear + insert + schedule. Duplicating OptoRepository would nest schedulers. Stamp at withDefaults keeps restore self-contained and matches REQ-B1 (stamp at Room save time).

## Rollback

Revert BackupRestoreCoordinator + test file.
