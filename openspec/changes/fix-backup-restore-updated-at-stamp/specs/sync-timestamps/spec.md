# Spec Delta: sync-timestamps (MODIFIED)

## ADDED Requirements

### Requirement: Backup restore stamps updatedAt

When `BackupRestoreCoordinator.restoreBackup` inserts pacientes, evaluaciones, dispensaciones, pagos, or servicios_extra into Room, each entity MUST have a non-null ISO-8601 `updatedAt` before the DAO insert.

#### Scenario: Paciente restored without updatedAt in backup

- **Given** a backup paciente with `updatedAt = null`
- **When** restore runs for the current óptica
- **Then** `pacienteRepo.insertPaciente` receives an entity with non-null `updatedAt`

#### Scenario: Pago restored without updatedAt in backup

- **Given** a backup pago with `updatedAt = null`
- **When** restore runs
- **Then** `dispensacionRepo.insertPago` receives an entity with non-null `updatedAt`

#### Scenario: All five sync entity types stamped

- **Given** a backup containing one row of each type with null `updatedAt`
- **When** restore runs
- **Then** every insert call receives non-null `updatedAt`
