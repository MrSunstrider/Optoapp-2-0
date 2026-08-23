# configuracion-financiera Specification

## Purpose

In-app edit and bidirectional sync of per-optica `configuracion_financiera` thresholds used by recommendations and BI alerts.

## Requirements

### Requirement: Threshold Editors

The system SHALL expose editors for existing config fields (margen neto objetivo, ticket, caídas, deuda, stock, min ventas, recalc freq). Only `admin`/`gerente` MAY save. Fail-closed when rol is null. Validation MUST reject nonsensical values (e.g. negative percentages where domain requires ≥0).

#### Scenario: Admin saves margen objetivo

- GIVEN rol admin and margenNetoObjetivo set to 18
- WHEN user saves
- THEN Room upserts the row for current `opticaId`
- AND finanzas sync upload is scheduled

#### Scenario: Non-writer denied

- GIVEN rol especialista (or null)
- WHEN config screen renders
- THEN save controls MUST be disabled or inaccessible

### Requirement: Upload After Local Upsert

`UploadSyncCoordinator` SHALL upsert the local config row to Supabase (`configuracion_financiera`). `ConfiguracionFinancieraRemoto` SHALL map snake_case columns and provide entity↔remoto mappers. `FinanzasSyncResult` SHALL expose an uploaded counter. Download path MUST remain; upload MUST run before download in the same sync cycle so local edits are not clobbered without push.

#### Scenario: Local edit survives sync cycle

- GIVEN local config changed and pending upload
- WHEN SyncFinanzas runs upload then download
- THEN remote receives the local values
- AND subsequent download keeps those values

#### Scenario: Empty local → upload zero

- GIVEN no local config row
- WHEN upload runs
- THEN uploaded counter MUST be 0 and no error crash
