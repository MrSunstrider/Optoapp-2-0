# Design: Fix Room Migration — gastos_operativos Column Migration

## Technical Approach

Add table recreation for `gastos_operativos` inside MIGRATION_39_40, following the existing `conflict_records` pattern: CREATE new table → INSERT with COALESCE → DROP old → RENAME new → CREATE INDEX. The Room entity `GastoOperativoEntity` already declares `isRecurring: Boolean` and `frecuencia: String` — the fix is purely in the migration layer. No entity, DAO, or TypeConverter changes needed. DB version stays at 40.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|---------|---------|--------|-----------|
| Schema change method | `ALTER TABLE RENAME COLUMN` | Table recreation (CREATE-INSERT-DROP-RENAME) | **Table recreation** | `RENAME COLUMN` requires SQLite 3.25.0 (Android API 30+). Min SDK is 24 (SQLite 3.9). Table recreation works on all API levels. |
| Migration placement | New MIGRATION_40_41 | Fix existing MIGRATION_39_40 | **Fix MIGRATION_39_40** | All changes are uncommitted — no version history to preserve. 39→40 already groups v39→v40 schema changes. Adding one more is consistent. |
| Destruction fallback | `fallbackToDestructiveMigration()` | Manual migration | **Manual migration** | Offline-first app — unsynced local data would be lost. Violates the project principle of not breaking existing data. |
| NULL handling | Require column exists | `COALESCE(esRecurrente, 0)` | **COALESCE** | Handles both scenarios: column exists (COALESCE returns value) and column missing (returns 0). Safe upgrade path from any intermediate state. |

## SQL Migration Strategy

Following the pattern established by `conflict_records` in MIGRATION_39_40 (lines 1097-1119):

1. `CREATE TABLE gastos_operativos_new` — full schema matching `GastoOperativoEntity` (adds `isRecurring INTEGER NOT NULL DEFAULT 0`, `frecuencia TEXT NOT NULL DEFAULT 'mensual'`)
2. `INSERT INTO gastos_operativos_new SELECT id, opticaId, categoria, descripcion, monto, fecha, fechaProgramada, nota, COALESCE(esRecurrente, 0), 'mensual', createdAt FROM gastos_operativos`
3. `DROP TABLE gastos_operativos`
4. `ALTER TABLE gastos_operativos_new RENAME TO gastos_operativos`
5. `CREATE INDEX IF NOT EXISTS index_gastos_operativos_opticaId ON gastos_operativos(opticaId)`

Columns use SQLite affinity: `INTEGER` for Boolean (Room stores 0/1), `REAL` for `BigDecimal`, `TEXT` for `LocalDate`/`String`. Index name must match `@Entity(indices = [Index(value = ["opticaId"])])` — Room derives index name from table+column.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt` | Modify | Add `gastos_operativos` table recreation block inside MIGRATION_39_40, after the existing `conflict_records` block (line 1119) |
| `optoapp/src/test/java/com/example/optoapp/data/OptoDatabaseMigrationTest.kt` | Modify | Add `gastos_operativos` table with `esRecurrente` INTEGER column to `createV30Tables()`, insert test row, add assertions in `migrate 30 to current preserves all data` |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (TDD) | MIGRATION_39_40 alone: v39 with `gastos_operativos` (has `esRecurrente`) → v40 with `isRecurring` | Failing test first (per `strict_tdd: true`), then fix |
| Integration | Full chain v30→v40: create table at v30 with `esRecurrente=1`, run all 10 migrations, verify `isRecurring=1`, `frecuencia='mensual'`, `monto` preserved | Extend existing `migrate 30 to current preserves all data` test |
| Regression | All existing migration tests pass | `./gradlew :optoapp:testDebugUnitTest --stacktrace` (run by CI) |

**TDD flow**: Add `gastos_operativos` with `esRecurrente` to `createV30Tables()` and insert/assert data → test FAILS (MIGRATION_39_40 missing the table recreation) → add SQL to MIGRATION_39_40 → test PASSES → run full test suite.

Per `config.yaml` rule: `strict_tdd: true`. The apply phase MUST write the failing test first, prove it fails, then implement the fix.

## Open Questions

None — scope is fully defined by the proposal and spec. Two files, well-understood pattern, no external dependencies.
