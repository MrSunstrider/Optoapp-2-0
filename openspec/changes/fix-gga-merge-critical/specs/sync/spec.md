# Delta for Sync

## ADDED Requirements

### Requirement: Upload SHALL validate opticaId

`UploadSyncCoordinator` MUST throw `IllegalArgumentException` when `opticaId` is blank. Silent fallback to a default tenant MUST NOT occur.

#### Scenario: Blank opticaId
- GIVEN blank or null `opticaId`
- WHEN upload dispatch runs
- THEN `IllegalArgumentException` thrown before any HTTP call

#### Scenario: Valid opticaId
- GIVEN non-blank `opticaId`
- WHEN upload dispatch runs
- THEN upload proceeds normally

### Requirement: Local merge SHALL run after remote upsert

`mergeLocalDispensacionConflict` MUST execute only after the remote upsert succeeds. Network failure SHALL prevent local state mutation.

#### Scenario: Network fails during upsert
- GIVEN a dispensacion pending upload with merge conflict
- WHEN remote upsert throws `IOException`
- THEN `mergeLocalDispensacionConflict` is NOT called
- AND local state remains unchanged

#### Scenario: Upsert succeeds
- GIVEN a dispensacion pending upload with merge conflict
- WHEN remote upsert returns success
- THEN `mergeLocalDispensacionConflict` executes after upsert

### Requirement: Servicio dedup SHALL compare timestamps as Instant

Timestamp deduplication for servicios MUST parse ISO-8601 strings to `Instant`. Raw string comparison MUST NOT be used — it produces incorrect results when precision varies.

#### Scenario: Different precision — temporal winner
- GIVEN servicio A with `"2025-01-01T10:00:00Z"`, B with `"2025-01-01T10:00:00.500Z"`
- WHEN deduplication runs
- THEN the record with the later `Instant` is selected

#### Scenario: Unparseable timestamp — keep existing
- GIVEN an existing servicio and a new one with a malformed timestamp
- WHEN deduplication runs
- THEN the existing record is kept, the malformed one discarded

### Requirement: Entity markSynced SHALL precede batch markSynced

Individual per-entity `markSynced` calls MUST complete successfully before `markSynced(entityType, "batch")` is invoked. If any individual mark fails, the batch SHALL NOT be marked synced.

#### Scenario: Individual mark fails — batch skipped
- GIVEN entity 2 of 3 fails `markSynced`
- WHEN the upload loop runs
- THEN `markSynced(opticaId, entityType, "batch")` is NOT called

#### Scenario: All marks succeed — batch marked
- GIVEN all entities synced without error
- WHEN the upload loop completes
- THEN `markSynced(opticaId, entityType, "batch")` is called

### Requirement: Dispensacion reassign SHALL filter by opticaId

`reassignItemsDispensacion` and `reassignRegalosDispensacion` MUST include `opticaId = :opticaId` in their WHERE clauses. Cross-tenant data reassignment MUST NOT occur.

#### Scenario: Cross-tenant — not reassigned
- GIVEN data belongs to `opticaId` "A", reassign targets `opticaId` "B"
- WHEN either reassign method runs
- THEN 0 rows are affected

#### Scenario: Same-tenant — reassigned
- GIVEN data and target share the same `opticaId`
- WHEN either reassign method runs
- THEN matching rows are reassigned correctly

### Requirement: syncGate mutex SHALL have timeout

`SyncOrchestrator.syncGate` mutex acquisition MUST use `withTimeout`. Indefinite blocking SHALL NOT occur — if one module hangs, the lock MUST release for the next attempt.

#### Scenario: Module hangs — timeout fires
- GIVEN `syncGate` is held by a hanging sync module
- WHEN another attempt waits for the lock
- THEN the wait times out
- AND the lock is released for subsequent attempts

### Requirement: Telemetry retry SHALL use exponential backoff

`SyncDiagnosticsViewModel` remote telemetry retry MUST use exponential backoff (×2 multiplier) with random jitter. Base delay SHALL be ≥1 second, retries SHALL NOT exceed 5 attempts.

#### Scenario: Transient failure — exponential delays
- GIVEN remote telemetry returns a transient error
- WHEN retry logic fires
- THEN delays follow ≈1s, ≈2s, ≈4s, ≈8s, ≈16s (± jitter)
- AND retries stop after 5 attempts

#### Scenario: Non-transient error — no retry
- GIVEN remote telemetry returns a 4xx client error
- WHEN retry logic evaluates the response
- THEN no retry is attempted
