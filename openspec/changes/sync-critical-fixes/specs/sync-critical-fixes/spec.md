# Delta for Sync Critical Fixes

## ADDED Requirements

### Requirement: EvaluacionEntity Columns SHALL Match Supabase Nullability

Room `EvaluacionEntity` columns where Supabase allows NULL MUST use nullable Kotlin types (`String?`, `Int?`, etc.). Room MUST NOT crash when Supabase returns NULL for these columns during download. Consumers SHALL handle null values via safe-call or Elvis with sensible defaults.

#### Scenario: Supabase returns NULL — no Room crash
- GIVEN Supabase evaluacion row has NULL in an optional text column
- WHEN download upserts row into Room
- THEN Room inserts successfully; Kotlin field reads as null

#### Scenario: All columns populated — no regression
- GIVEN Supabase row has all non-key columns filled
- WHEN download writes to Room
- THEN all fields read correctly with their original values

#### Scenario: UI handles null safely
- GIVEN `observaciones` is `String?` after download
- WHEN evaluation details screen renders
- THEN field displays empty string or "—" instead of crashing

---

### Requirement: Download DAO Insert Methods SHALL Be Idempotent

All `@Insert` methods in DAOs used by download coordinator paths MUST use `OnConflictStrategy.REPLACE`. Re-downloading an entity with an existing primary key SHALL succeed by replacing the stale row, never throwing `SQLiteConstraintException`.

#### Scenario: Re-download replaces existing row
- GIVEN entity with PK=X already in Room
- WHEN download inserts same PK again
- THEN row is replaced without exception

#### Scenario: First insert unaffected
- GIVEN no row with PK=X exists
- WHEN download inserts entity
- THEN insert succeeds normally (no regression)

#### Scenario: Audit covers all download-path DAOs
- GIVEN any `@Insert` method used by `DownloadSyncCoordinator`, `SyncHistorialUseCase`, or other download use cases
- WHEN code is inspected
- THEN annotation includes `onConflict = OnConflictStrategy.REPLACE`

---

### Requirement: Upload markSynced SHALL Run Inside Database Transaction

`SyncStateTracker.markSynced()` calls on upload paths MUST execute within `database.withTransaction {}` alongside entity writes. If `markSynced()` fails, entity writes SHALL roll back. This matches the download path pattern where both are already transactional.

#### Scenario: Write and markSynced commit atomically
- GIVEN entity uploads successfully to Supabase
- WHEN upload path writes entity + calls `markSynced()` inside `withTransaction`
- THEN both commit together; entity will not re-upload next cycle

#### Scenario: markSynced failure rolls back write
- GIVEN `markSynced()` throws inside `withTransaction`
- WHEN transaction block executes
- THEN entity write is rolled back; entity remains unsynced for retry

#### Scenario: Download path unchanged
- GIVEN download path already wraps `markSynced` in `withTransaction`
- WHEN this fix touches upload paths only
- THEN download behavior is unchanged

---

### Requirement: All-Chunk Query Failure SHALL Not Crash Sync

`ConflictHelper.selectRemoteRows()` MUST NOT throw `RuntimeException` when every chunk query fails. It SHALL return an empty map or recoverable `Result`. Callers in `filterConflicts()` SHALL handle empty results without crashing the sync cycle, logging the failure and allowing sibling modules to proceed.

#### Scenario: All chunks fail — empty map returned
- GIVEN Supabase unreachable for every chunk query
- WHEN `selectRemoteRows()` runs
- THEN method returns empty map (no throw); error is logged

#### Scenario: Partial failure — surviving chunks merged
- GIVEN 2 of 3 chunks succeed, 1 fails
- WHEN `selectRemoteRows()` completes
- THEN results from successful chunks are merged; failure logged as warning

#### Scenario: Caller survives empty result
- GIVEN `selectRemoteRows()` returns empty map due to total network failure
- WHEN `filterConflicts()` processes result
- THEN sync module logs error and continues to next module without crash
