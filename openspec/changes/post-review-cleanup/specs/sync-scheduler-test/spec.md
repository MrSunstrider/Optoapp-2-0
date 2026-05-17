# Sync Scheduler Test Specification

## Purpose

Add unit test coverage for `PostSaveSyncScheduler`, the core sync orchestrator, which currently has 0% test coverage. Cover scheduling logic, retry behavior, and error handling.

## Requirements

### Requirement: PostSaveSyncScheduler test coverage

The system MUST have at least 5 unit tests for `PostSaveSyncScheduler`. Tests MUST cover the scheduling lifecycle, retry mechanism, and error states.

| Test Category | Minimum Tests | What to Cover |
|--------------|---------------|---------------|
| Scheduling | 1 | Sync job is enqueued after save |
| Retry on failure | 1 | Failed sync is retried with backoff |
| Max retries exceeded | 1 | Sync fails permanently after max retries |
| Concurrency | 1 | Duplicate saves don't create duplicate sync jobs |
| Error propagation | 1 | Sync errors surface correctly to callers |

#### Scenario: Sync scheduled after save

- GIVEN a record is saved locally
- WHEN save completes successfully
- THEN PostSaveSyncScheduler MUST enqueue a sync job
- AND the sync job MUST reference the saved record identifier

#### Scenario: Retry with backoff on transient failure

- GIVEN a sync job fails with a transient network error
- WHEN the scheduler processes the retry
- THEN the retry MUST use exponential backoff
- AND the retry count MUST increment

#### Scenario: Permanent failure after max retries

- GIVEN a sync job has failed 3 times (max retries)
- WHEN the scheduler attempts the next retry
- THEN the job MUST be marked as permanently failed
- AND no further retries MUST be scheduled

#### Scenario: No duplicate sync jobs for same record

- GIVEN a record is saved twice in quick succession
- WHEN both saves trigger sync scheduling
- THEN only ONE sync job MUST exist for that record
- AND the second save MUST update the existing job, not create a new one

#### Scenario: Sync error propagated to caller

- GIVEN a sync job completes with an error
- WHEN the caller observes the sync result
- THEN the error state MUST be available via Flow/callback
- AND the error MUST include the exception type and message

### Requirement: Test isolation

Tests MUST NOT depend on network, database, or Supabase connectivity. All external dependencies MUST be mocked using MockK.

#### Scenario: Tests run without network

- GIVEN all PostSaveSyncScheduler tests are written
- WHEN tests are executed
- THEN no network calls MUST be made
- AND all repository/datasource interactions MUST be mocked
