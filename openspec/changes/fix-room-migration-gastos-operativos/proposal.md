# Proposal: Fix incomplete Room migration MIGRATION_39_40 — gastos_operativos column rename

## Intent

Room migration MIGRATION_39_40 (v39→v40) handles `conflict_records` (composite PK) and `resumen_diario` (unique index), but is missing the column rename in `gastos_operativos`: `esRecurrente` → `isRecurring`. The app crashes at startup with `IllegalStateException: Migration didn't properly handle: gastos_operativos`.

## Scope

### In Scope
1. Add `gastos_operativos` table recreation (CREATE-INSERT-DROP-RENAME + index) to MIGRATION_39_40
2. Update v30→v40 data-preservation test to create `gastos_operativos` with old column name and verify it survives migration

### Out of Scope
- Room database version bump (stays at 40)
- Supabase schema, RLS, or Edge Functions (Room/SQLite only)
- TypeConverter changes (none needed)
- Entity class changes (`GastoOperativoEntity.kt` already has `isRecurring`)

## Capabilities

### New Capabilities
None — bug fix, no new capability.

### Modified Capabilities
None — no spec-level behavior changes. `migration-conventions` and `migration-tests` specs cover Supabase migrations, not Room.

## Approach

Add table recreation for `gastos_operativos` to MIGRATION_39_40 using the same pattern already used for `conflict_records`:

1. `CREATE TABLE gastos_operativos_new (... isRecurring INTEGER NOT NULL DEFAULT 0 ...)`
2. `INSERT INTO ... SELECT COALESCE(esRecurrente, 0), ... FROM gastos_operativos`
3. `DROP TABLE gastos_operativos`
4. `ALTER TABLE gastos_operativos_new RENAME TO gastos_operativos`
5. `CREATE INDEX IF NOT EXISTS index_gastos_operativos_opticaId ...`

Per `strict_tdd: true`: apply phase MUST write failing test first, then migration fix, then verify.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/OptoDatabaseMigrations.kt` | Modified | Add gastos_operativos recreation inside MIGRATION_39_40 |
| `data/OptoDatabaseMigrationTest.kt` | Modified | Add gastos_operativos to `createV30Tables`, insert row, assert data after migration |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Incorrect column count/order in INSERT mapping | Low | Review against GastoOperativoEntity fields; COALESCE handles nulls; test verifies |
| Index already exists with different definition | Low | `CREATE INDEX IF NOT EXISTS` |

## Rollback Plan

`git checkout` on `OptoDatabaseMigrations.kt` and `OptoDatabaseMigrationTest.kt`. No DB version to revert (unchanged).

## Dependencies

None.

## Success Criteria

- [ ] v30→v40 test inserts `gastos_operativos` row with `esRecurrente`, runs all 10 migrations, reads back `isRecurring` with preserved value
- [ ] All existing migration tests pass
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes
