# Servicio Extra Specification

## Purpose

The `ServicioExtra` entity models non-dispensación services sold by an optica (repairs, accessories, etc.). This spec defines the entity shape, the optional delivery date mirroring `DispensacionOptica.fechaEntrega`, and the migrations that add `fecha_entrega` to existing local (Room) and remote (Supabase) schemas.

## Requirements

### Requirement: ServicioExtra Entity Shape

The `ServicioExtra` data class MUST expose `id`, `descripcion`, `montoTotal`, `aCuenta`, `estado`, `fecha`, `pacienteId`, `opticaId`, and an optional `fechaEntrega: LocalDate? = null` mirroring `DispensacionOptica.fechaEntrega`. `aCuenta` represents the down-payment collected at sale time (analogue of `DispensacionOptica.montoPagado`).

#### Scenario: Default fechaEntrega is null

- GIVEN a `ServicioExtra` is constructed without supplying `fechaEntrega`
- THEN its `fechaEntrega` field MUST be `null`

#### Scenario: fechaEntrega can be set

- GIVEN a `ServicioExtra` constructed with `fechaEntrega = LocalDate.of(2026, 7, 1)`
- THEN `fechaEntrega` MUST equal `LocalDate.of(2026, 7, 1)`

### Requirement: Room Migration 28→29

The system SHALL ship a `MIGRATION_28_29` that runs `ALTER TABLE servicios_extra ADD COLUMN fecha_entrega TEXT` (nullable, no default). `OptoDatabase` MUST bump its `version` to 29 and register `MIGRATION_28_29` in `addMigrations(...)`.

#### Scenario: Migration adds the column and preserves existing rows

- GIVEN a v28 database with N rows in `servicios_extra`
- WHEN `MIGRATION_28_29.migrate` runs
- THEN the `servicios_extra` table MUST have a `fecha_entrega` column AND all N rows MUST survive with `fecha_entrega IS NULL`

#### Scenario: Fresh insert post-migration can set fecha_entrega

- GIVEN a database migrated to v29
- WHEN a row is inserted with `fecha_entrega = '2026-07-01'`
- THEN querying `fecha_entrega` for that row MUST return `'2026-07-01'`

#### Scenario: Migration is re-exported and chain stays sequential

- GIVEN migrations 6→28 exist
- THEN `MIGRATION_28_29.startVersion` MUST be 28 AND `endVersion` MUST be 29 AND `OptoDatabase.MIGRATION_28_29` MUST equal the public `MIGRATION_28_29`

### Requirement: Supabase Migration for fecha_entrega

The system SHALL ship a Supabase migration that runs `ALTER TABLE public.servicios_extra ADD COLUMN fecha_entrega DATE` (nullable). The column MUST be nullable so existing rows back-fill to `NULL` with no data loss.

#### Scenario: Column added as nullable DATE

- GIVEN the Supabase migration is applied to a project with existing `servicios_extra` rows
- THEN `public.servicios_extra.fecha_entrega` MUST be of type `DATE` AND nullable AND every pre-existing row MUST have `NULL` in that column

### Requirement: Servicio Extra Inventory Picker

Servicios extra MUST list active inventory items (armazón and ACCESORIO) with stock > 0 in the product search picker.

#### Scenario: accesorio activo con stock aparece

- GIVEN a montura row with `categoria=ACCESORIO`, `activo=true`, `stockActual=5`
- WHEN the user opens nuevo servicio extra picker
- THEN the item MUST appear in search results

### Requirement: ServicioExtra monturaId

`ServicioExtra` MUST expose optional `monturaId`. Sync DTO MUST map `montura_id`.

#### Scenario: servicio vinculado guarda monturaId

- GIVEN user selects inventory item `m-liquido`
- WHEN save succeeds
- THEN `servicios_extra.monturaId = 'm-liquido'`

### Requirement: Servicio Extra Stock on Sale

WHEN save links `monturaId` and it is new or changed
THEN the system MUST register `SALIDA_VENTA` qty 1 with `referenciaId = servicio.id`.

WHEN cancel or edit removes/changes linked product
THEN the system MUST register `AJUSTE` restock with distinct `:rev:` referencia.

### Requirement: Dispensación Picker Unchanged

Dispensación picker MUST continue excluding ACCESORIO (`isArmazon` only).
