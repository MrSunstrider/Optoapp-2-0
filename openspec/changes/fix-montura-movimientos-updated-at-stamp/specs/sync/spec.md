# Spec Delta: `sync` (MODIFIED)

## ADDED Requirements

### Requirement: Montura movimiento updatedAt stamped at Room save

`MonturaInventoryCoordinator.insertMonturaMovimiento` MUST set `updatedAt` to a non-null ISO-8601 UTC string before DAO insert. `toRemoto()` MUST pass through that value unchanged.

#### Scenario: New manual stock movement uploads successfully

- **Given** a new ENTRADA movement created via inventory UI
- **When** inventario sync uploads movimientos
- **Then** the POST body includes non-null `updated_at`
- **And** Supabase does not return 23502

#### Scenario: Legacy null rows backfilled on migration

- **Given** Room has `montura_movimientos` rows with `updatedAt IS NULL` before migration 47→48
- **When** migration runs
- **Then** every row has non-null `updatedAt`

#### Scenario: toRemoto does not fabricate timestamp

- **Given** a Room entity with `updatedAt = "2026-08-29T12:00:00Z"`
- **When** `toRemoto()` is called
- **Then** remote DTO `updatedAt` equals `"2026-08-29T12:00:00Z"`
