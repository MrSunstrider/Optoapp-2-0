# Delta for sync

## ADDED Requirements

### Requirement: costos_productos Sync
System SHALL include `costos_productos` in download AND upload sync. Remote DTO SHALL map matrix columns via `@SerialName`. Sync order: ← ventas → **costos_productos** → pagos.

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN costos_productos downloads after ventas
- AND local updates upload

### Requirement: costos_biselado Sync (Read-Only)
System SHALL include `costos_biselado` in download only. Upload SHALL NOT be supported.

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN costos_biselado downloads
- AND no upload occurs
