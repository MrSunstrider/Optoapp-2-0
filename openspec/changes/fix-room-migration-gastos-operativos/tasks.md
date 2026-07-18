# Tasks: Fix Room Migration — gastos_operativos Column Rename

## Review Workload Forecast

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Migration Test

- [x] 1.1 Skipped — user explicitly forbade modifying OptoDatabaseMigrationTest.kt. No test changes requested for this apply batch.
- [x] 1.2 Skipped — user explicitly forbade modifying OptoDatabaseMigrationTest.kt.
- [x] 1.3 Skipped — user explicitly forbade modifying OptoDatabaseMigrationTest.kt.
- [x] 1.4 Skipped — user explicitly forbade modifying OptoDatabaseMigrationTest.kt.

## Phase 2: GREEN — Add gastos_operativos Recreation to MIGRATION_39_40

- [x] 2.1 Added `gastos_operativos` table recreation block inside `MIGRATION_39_40.migrate()` after the `resumen_diario` block:
  - `CREATE TABLE gastos_operativos_new` with `isRecurring INTEGER NOT NULL DEFAULT 0` and `frecuencia TEXT NOT NULL DEFAULT 'mensual'`
  - `INSERT INTO ... SELECT COALESCE(esRecurrente, 0), COALESCE(frecuencia, 'mensual'), ... FROM gastos_operativos`
  - `DROP TABLE gastos_operativos`
  - `ALTER TABLE gastos_operativos_new RENAME TO gastos_operativos`
  - `CREATE INDEX IF NOT EXISTS index_gastos_operativos_opticaId`

## Phase 3: VERIFY — Test Suite Passes

- [x] 3.1 Compilation verified: `./gradlew :optoapp:assembleDebug` — BUILD SUCCESSFUL (Room schema validation passes via KSP)
- [x] 3.2 No regression: existing `conflict_records` and `resumen_diario` blocks untouched
