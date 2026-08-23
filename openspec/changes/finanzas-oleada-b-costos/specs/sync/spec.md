# Delta for sync

## ADDED Requirements

### Requirement: costos_lc Sync

System SHALL include `costos_lc` in SyncFinanzas download AND upload. Remote DTO SHALL map matrix columns via `@SerialName`. Upload and download SHALL run immediately after `costos_biselado`. Download SHALL use upsert with `skipDeletions = true`. Result counters SHALL include uploaded/downloaded LC counts.

#### Scenario: Order after biselado

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN `costos_lc` uploads after `costos_biselado`
- AND `costos_lc` downloads after `costos_biselado`

#### Scenario: Empty local upload

- GIVEN no local `costos_lc` rows for optica
- WHEN upload runs
- THEN upload returns 0 without failing the sync cycle

## MODIFIED Requirements

### Requirement: costos_biselado Sync (Read-Only)

System SHALL include `costos_biselado` in download AND upload sync (mirror `costos_productos` pattern). Remote DTO SHALL map columns via `@SerialName`. Sync order: … → **costos_productos** → **costos_biselado** → **costos_lc** → pagos ….

(Previously: Spec claimed download-only; production already uploads+downloads — delta aligns docs with live behavior and places LC after biselado.)

#### Scenario: Upload and download

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN costos_biselado uploads and downloads
- AND no deletion push for soft-deleted matrix rows beyond upsert semantics
