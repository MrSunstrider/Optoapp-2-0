# Delta for sync

## ADDED Requirements

### Requirement: configuracion_financiera Upload

`UploadSyncCoordinator` SHALL implement `uploadConfiguracionFinanciera(opticaId)` that upserts the single local row (if present) to PostgREST. `SyncFinanzasUseCase` SHALL call it in the upload phase before download of the same table. `FinanzasSyncResult` SHALL include `uploadedConfiguracionesFinancieras` (or equivalent). Failures MUST be safeUpload-logged without aborting unrelated uploads.

#### Scenario: Upload then download order

- GIVEN local config row dirty
- WHEN SyncFinanzas runs
- THEN uploadConfiguracionFinanciera runs before downloadConfiguracionFinanciera

#### Scenario: Missing row uploads zero

- GIVEN no local config for optica
- WHEN upload runs
- THEN counter is 0
