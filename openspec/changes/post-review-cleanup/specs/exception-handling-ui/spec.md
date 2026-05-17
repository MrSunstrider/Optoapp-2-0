# Exception Handling UI Specification

## Purpose

Ensure all ViewModels and delegates catch specific exception types instead of generic `catch (e: Exception)`, with structured error logging. Applies to Android presentation layer only.

## Requirements

### Requirement: No generic catches in ViewModels

The system SHALL NOT contain `catch (e: Exception)` blocks in ViewModel or delegate classes. Each catch MUST use the most specific exception type available for the operation.

| File | Current Catches | Expected Specific Types |
|------|----------------|------------------------|
| AuthDelegate | 2 generic | IOException, SupabaseException, or domain-specific |
| BackupDelegate | 1 generic | IOException, DatabaseException |
| ServiciosViewModel | 1 generic | NetworkException, IOException |
| MonturasViewModel | 1 generic | NetworkException, IOException |
| PacienteViewModel | 1 generic | NetworkException, IOException |
| FiscalConfigViewModel | 1 generic | IOException, ValidationException |
| EvaluacionViewModel | 1 generic | IOException, SerializationException |

#### Scenario: Specific exception caught on network failure

- GIVEN a ViewModel makes a network call via repository
- WHEN a `SocketTimeoutException` is thrown
- THEN the catch block MUST handle `IOException` (or a subtype), NOT `Exception`
- AND the error is logged with context (operation name, exception message)

#### Scenario: Specific exception caught on serialization failure

- GIVEN a ViewModel parses JSON response
- WHEN a `SerializationException` is thrown
- THEN the catch block MUST handle `SerializationException`, NOT `Exception`

#### Scenario: No generic catch remains after refactoring

- GIVEN all 7 identified files have been refactored
- WHEN a code search for `catch (e: Exception)` is performed across `presentation/viewmodel/` and delegates
- THEN zero matches MUST be found

### Requirement: Structured error logging

Each catch block SHALL log the exception with at minimum: exception type, message, and the operation being performed. Logging MUST use the project's existing logging mechanism (Timber or similar).

#### Scenario: Error logged with context on exception

- GIVEN a catch block handles a specific exception
- WHEN the exception is caught
- THEN the log entry MUST include the operation name and exception message
- AND the log level MUST be ERROR (not DEBUG or INFO)

### Requirement: User-facing error state preserved

Refactoring catch blocks SHALL NOT change the error state exposed to the UI. The existing `UiState.Error` or equivalent mechanism MUST continue to receive the same error information after refactoring.

#### Scenario: Error state unchanged after catch refactoring

- GIVEN a ViewModel previously caught `Exception` and set error state
- WHEN the catch is refactored to a specific type
- THEN the same error state MUST be set for the same failure scenarios
- AND no new error states are introduced that the UI doesn't handle
