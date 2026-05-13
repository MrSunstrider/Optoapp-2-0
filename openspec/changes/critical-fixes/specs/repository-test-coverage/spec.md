# Repository Test Coverage Specification

## Purpose

Define requirements for adding unit tests to four core repositories currently at 0% test coverage: MembershipRepository, OptoRepository, PacienteRepository, and DispensacionRepository.

## Requirements

### Requirement: MembershipRepository MUST have ≥5 unit tests

Tests MUST cover membership CRUD operations, optica membership sync, and error handling. Tests MUST use MockK for dependency mocking and follow existing DAO test patterns.

#### Scenario: Create membership successfully

- GIVEN a valid membership object
- WHEN `createMembership()` is called
- THEN the membership MUST be persisted
- AND the returned result MUST contain the created membership

#### Scenario: Read membership by ID

- GIVEN a membership exists with ID "mem-123"
- WHEN `getMembership("mem-123")` is called
- THEN the result MUST contain the membership data

#### Scenario: Update membership

- GIVEN a membership exists with ID "mem-123"
- WHEN `updateMembership()` is called with modified fields
- THEN the membership MUST be updated
- AND subsequent reads MUST return the updated data

#### Scenario: Delete membership

- GIVEN a membership exists with ID "mem-123"
- WHEN `deleteMembership("mem-123")` is called
- THEN the membership MUST be removed
- AND subsequent reads MUST return null

#### Scenario: Network error during sync

- GIVEN the network is unavailable
- WHEN `syncOpticaMembership()` is called
- THEN the result MUST be a failure
- AND the exception MUST indicate the network error

### Requirement: OptoRepository MUST have ≥5 unit tests

Tests MUST cover database operations and sync state management. Tests MUST verify correct behavior for both success and failure paths.

#### Scenario: Insert optician data

- GIVEN valid optician data
- WHEN `insertOpto()` is called
- THEN the data MUST be persisted to the database

#### Scenario: Query optician by ID

- GIVEN an optician record exists
- WHEN `getOptoById()` is called
- THEN the result MUST contain the correct optician data

#### Scenario: Sync state tracks last sync time

- GIVEN a successful sync operation
- WHEN `updateSyncState()` is called
- THEN the sync timestamp MUST be updated
- AND subsequent `getLastSyncTime()` calls MUST return the new timestamp

#### Scenario: Sync handles server error

- GIVEN the server returns a 500 error
- WHEN sync is triggered
- THEN the sync state MUST NOT be updated
- AND the error MUST be propagated via Result.failure

#### Scenario: Clear all optician data

- GIVEN multiple optician records exist
- WHEN `clearAll()` is called
- THEN all records MUST be removed

### Requirement: PacienteRepository MUST have ≥5 unit tests

Tests MUST cover CRUD operations and observability (Flow-based queries). Tests MUST verify both happy paths and edge cases.

#### Scenario: Create patient

- GIVEN a valid patient object
- WHEN `createPaciente()` is called
- THEN the patient MUST be persisted
- AND the returned result MUST contain the created patient with assigned ID

#### Scenario: Read patient by ID

- GIVEN a patient exists with ID "pac-456"
- WHEN `getPacienteById("pac-456")` is called
- THEN the result MUST contain the patient data

#### Scenario: Update patient

- GIVEN a patient exists with ID "pac-456"
- WHEN `updatePaciente()` is called with modified fields
- THEN the patient MUST be updated
- AND subsequent reads MUST return the updated data

#### Scenario: Delete patient

- GIVEN a patient exists with ID "pac-456"
- WHEN `deletePaciente("pac-456")` is called
- THEN the patient MUST be removed

#### Scenario: Observe patients via Flow

- GIVEN patients exist in the database
- WHEN `observePacientes()` is called
- THEN a Flow MUST be returned that emits the current list
- AND subsequent inserts MUST trigger new emissions

### Requirement: DispensacionRepository MUST have ≥5 unit tests

Tests MUST cover create/update workflows and stock operations. Tests MUST verify transactional integrity of multi-step operations.

#### Scenario: Create dispensacion

- GIVEN a valid dispensacion object with items
- WHEN `createDispensacion()` is called
- THEN the dispensacion MUST be persisted with all items
- AND stock MUST be decremented for each item

#### Scenario: Update dispensacion status

- GIVEN a dispensacion exists with status "pending"
- WHEN `updateStatus("completed")` is called
- THEN the status MUST be updated
- AND the update timestamp MUST be recorded

#### Scenario: Stock decrement on dispensacion

- GIVEN item "lente-001" has stock of 10
- WHEN a dispensacion with quantity 3 is created
- THEN item "lente-001" MUST have stock of 7

#### Scenario: Insufficient stock rejection

- GIVEN item "lente-001" has stock of 2
- WHEN a dispensacion with quantity 5 is attempted
- THEN the result MUST be a failure
- AND the stock MUST NOT be modified

#### Scenario: List dispensaciones by date range

- GIVEN dispensaciones exist for dates 2026-01-01 through 2026-01-31
- WHEN `getByDateRange("2026-01-15", "2026-01-20")` is called
- THEN only dispensaciones within that range MUST be returned

## Non-Goals

- Integration or instrumented tests (these are unit tests only)
- Testing ViewModels or UI layers
- Testing DAO layer (already covered by existing DAO tests)
- Achieving specific coverage percentages beyond the ≥5 tests per repository
- Testing network layer (mocked via MockK)

## Test Infrastructure

- Framework: JUnit 4 with MockK for mocking
- Coroutine testing: `kotlinx-coroutines-test` with `runTest`
- Naming: descriptive camelCase method names
- Location: `app/src/test/java/com/example/optoapp/data/` mirroring source structure
- Pattern: one assertion per test, setup via `@Before` or `@BeforeEach`
