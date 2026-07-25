# Delta for Sync Conflict Resolution

## ADDED Requirements

### Requirement: Remote Row Fetching MUST Batch Large ID Sets

`ConflictHelper.selectRemoteRows()` MUST split entity IDs into chunks of at most 80 and execute queries in parallel via structured concurrency (`coroutineScope` + `async`). The method SHALL NOT send all IDs in a single URL — with 160+ UUIDs (36 chars each), the URL exceeds PostgREST/Kong's 8000-character limit and triggers HTTP 414 (URI Too Long), which currently returns an empty map and silently marks all entities as conflict-safe.

#### Scenario: Normal batch — single query

- GIVEN 50 entity IDs
- WHEN `selectRemoteRows()` queries remote `updatedAt` values
- THEN exactly 1 query is issued with all 50 IDs

#### Scenario: Large batch — multiple chunks

- GIVEN 200 entity IDs
- WHEN `selectRemoteRows()` queries remote `updatedAt` values
- THEN exactly 3 queries are issued (80 + 80 + 40)
- AND results from all chunks are merged into a single map

#### Scenario: Batch size boundary

- GIVEN exactly 80 entity IDs
- WHEN `selectRemoteRows()` runs
- THEN 1 query is issued (80 is within limit, no unnecessary chunking)

#### Scenario: Single chunk fails — surviving results used

- GIVEN 3 chunks of 80 IDs each, and 1 chunk query fails
- WHEN `selectRemoteRows()` completes
- THEN results from the 2 successful chunks are merged
- AND a warning is logged for the failed chunk
- AND conflict detection proceeds with partial data

---

### Requirement: Timestamps MUST Be Normalized Before Instant Parsing

`ConflictHelper.parseInstant()` MUST normalize timestamp strings before calling `Instant.parse()`. Normalization SHALL: (a) truncate fractional seconds to at most 3 digits (milliseconds) to handle microsecond precision from PostgreSQL `timestamptz`, and (b) replace `+00:00` and `+0000` offset suffixes with `Z` — Android core library desugaring rejects both patterns, causing a fallback to fragile `LocalDateTime.parse()` + string comparison.

#### Scenario: Microsecond precision with colon offset

- GIVEN timestamp `"2026-07-25T02:32:19.469712+00:00"`
- WHEN `isLocalNewerOrEqual(local, remote)` compares this timestamp
- THEN normalization produces `"2026-07-25T02:32:19.469Z"`
- AND `Instant.parse()` succeeds

#### Scenario: Microsecond precision without colon offset

- GIVEN timestamp `"2026-07-25T02:32:19.469712+0000"`
- WHEN normalization runs
- THEN the offset suffix `+0000` is replaced with `Z`
- AND fractional seconds are truncated to 3 digits

#### Scenario: Standard millisecond precision passes through

- GIVEN timestamp `"2026-07-25T02:32:19.469Z"`
- WHEN normalization runs
- THEN the timestamp remains unchanged

#### Scenario: No fractional seconds

- GIVEN timestamp `"2026-07-25T02:32:19Z"`
- WHEN normalization runs
- THEN the timestamp remains unchanged

#### Scenario: Unparseable timestamp falls back to string comparison

- GIVEN a timestamp that remains unparseable after normalization
- WHEN `parseInstant()` attempts parsing
- THEN the method falls back to `LocalDateTime.parse()` + string comparison (existing behavior preserved)

---

### Requirement: Conflict Detection Failure MUST NOT Be Silent

When `selectRemoteRows()` cannot retrieve remote timestamps for ANY entity (all chunk queries fail), `filterConflicts()` SHALL NOT treat all local entities as conflict-safe. The method MUST return a failure indicator so upstream callers can decide whether to abort upload or proceed with caution.

#### Scenario: All chunk queries fail

- GIVEN Supabase is unreachable for all chunk queries across 200 IDs
- WHEN `filterConflicts()` attempts conflict detection
- THEN the method returns a distinguishable failure state (empty map with logged error, or propagated exception)
- AND NO entities are silently passed as conflict-free

#### Scenario: Local-only entities with remote unavailability

- GIVEN 50 local entities exist with timestamps, and `selectRemoteRows()` returns empty due to all-chunk failure
- WHEN `filterConflicts()` runs
- THEN the caller receives a failure signal
- AND those 50 local entities are NOT uploaded as if no remote versions exist

#### Scenario: Normal fetch succeeds — no false alarm

- GIVEN `selectRemoteRows()` successfully returns timestamps for all IDs
- WHEN `filterConflicts()` runs
- THEN conflict detection proceeds normally (no failure signal raised)
