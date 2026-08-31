# Proposal: Stamp updatedAt on backup restore

## Intent

After restore, every synced entity MUST have non-null `updatedAt` so the next sync upload does not fail with 23502.

## In scope

- Stamp `updatedAt = Instant.now()` in `BackupRestoreCoordinator` for pacientes, evaluaciones, dispensaciones, pagos, servicios_extra
- Unit tests capturing insert args

## Out of scope

- Changing PacienteRepository / DispensacionRepository insert APIs
- Monturas / movimientos restore (backup payload does not include them today)
- Supabase migrations
