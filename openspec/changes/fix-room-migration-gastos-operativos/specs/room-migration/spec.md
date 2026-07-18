# Room Migration Specification

## Purpose

Define requirements for Room database migrations in the Android app, ensuring schema validity after version upgrades and data preservation through table recreation operations.

## Requirements

### Requirement: Gastos Operativos Column Migration in MIGRATION_39_40

MIGRATION_39_40 MUST recreate the `gastos_operativos` table to rename column `esRecurrente` to `isRecurring` and add column `frecuencia` with default `'mensual'`. The recreation SHALL follow the same CREATE-INSERT-DROP-RENAME pattern used for `conflict_records` in the same migration. DB version SHALL remain at 40.

#### Scenario: Table has old column `esRecurrente`

- GIVEN `gastos_operativos` has column `esRecurrente` (INTEGER) but not `isRecurring`
- WHEN MIGRATION_39_40 runs
- THEN the table is recreated with `isRecurring INTEGER NOT NULL DEFAULT 0` and `frecuencia TEXT NOT NULL DEFAULT 'mensual'`
- AND `esRecurrente` values are migrated via `COALESCE(esRecurrente, 0)` into `isRecurring`

#### Scenario: Table has neither old nor new column (fresh from v31→32)

- GIVEN `gastos_operativos` was created by MIGRATION_31_32 without `esRecurrente` or `isRecurring`
- WHEN MIGRATION_39_40 runs
- THEN the table is recreated with `isRecurring INTEGER NOT NULL DEFAULT 0`
- AND existing rows receive the default value `0` for `isRecurring` via COALESCE fallback

#### Scenario: Existing data is preserved through table recreation

- GIVEN `gastos_operativos` contains rows with `esRecurrente = 1` and other column values
- WHEN MIGRATION_39_40 recreates the table
- THEN all rows are migrated from old table into new table
- AND `isRecurring` reflects the original `esRecurrente` value
- AND all other column values (`id`, `opticaId`, `categoria`, `monto`, `fecha`, `fechaProgramada`, `nota`, `descripcion`, `createdAt`) are preserved unchanged
- AND the `index_gastos_operativos_opticaId` index is recreated with `CREATE INDEX IF NOT EXISTS`

### Requirement: Full Migration Chain Data Preservation

The Room migration chain from version 30 to version 40 MUST preserve `gastos_operativos` data. Test coverage SHALL verify preservation both in isolation (39→40) and through the full chain (30→40).

#### Scenario: Full chain v30→v40 preserves gastos_operativos data

- GIVEN a v30 database with `gastos_operativos` containing a row where `esRecurrente = 1` and `monto = 500`
- WHEN all migrations 30→31 through 39→40 run sequentially
- THEN the row survives with `isRecurring = 1`, `frecuencia = 'mensual'`, and `monto = 500`
- AND all other column values match the original insert

#### Scenario: Crash before fix, resolved after fix (TDD)

- GIVEN a v39 database where `gastos_operativos` has column `esRecurrente` but the Room entity at v40 expects `isRecurring`
- WHEN Room validates the v40 schema without the migration fix
- THEN `IllegalStateException` with message `Migration didn't properly handle: gastos_operativos` is thrown
- AND after applying the table recreation fix to MIGRATION_39_40, the same validation succeeds with no crash
