# Exception Handling Specification

## Purpose

Define requirements for replacing 57+ generic `catch (e: Exception)` blocks across data/domain layers with specific exception types, structured logging, and Result-based error propagation.

## Requirements

### Requirement: Generic catches MUST be replaced with specific exception types

Each `catch (e: Exception)` block in data and domain layers MUST be refactored to catch the most specific exception type that the enclosed code can throw. Where multiple exceptions are possible, separate catch blocks MUST be used.

#### Scenario: IOException in network call

- GIVEN a repository method makes an HTTP request
- WHEN an IOException occurs during the request
- THEN the catch block MUST catch `IOException` specifically (not `Exception`)
- AND the error MUST be logged with Timber at ERROR level including operation context

#### Scenario: TimeoutException in network call

- GIVEN a repository method makes an HTTP request with a timeout
- WHEN a `java.util.concurrent.TimeoutException` occurs
- THEN the catch block MUST catch `TimeoutException` specifically
- AND the error MUST be logged with context identifying the timed-out operation

#### Scenario: Multiple exception types possible

- GIVEN a method can throw both `IOException` and `JsonParseException`
- WHEN the method body executes
- THEN there MUST be separate catch blocks for each exception type
- AND each catch block MUST log with appropriate context

### Requirement: All catches MUST log with context

Every catch block in data and domain layers MUST log the exception using Timber at ERROR level. The log message MUST include enough context to identify the failing operation without reading the stack trace.

#### Scenario: Log includes operation context

- GIVEN a repository method `syncFinanzas()` catches an exception
- WHEN the exception is logged
- THEN the log message MUST contain the operation name (e.g., "syncFinanzas") and relevant parameters

#### Scenario: Exception chain preserved

- GIVEN a caught exception wraps a root cause
- WHEN logged, the exception MUST be passed as the second argument to Timber (for stack trace)
- AND the message MUST not discard the original exception

### Requirement: Errors MUST propagate via Result<T>

Repository and use-case methods in the data and domain layers MUST wrap their return values in `Result<T>`. Catch blocks MUST return `Result.failure(exception)` instead of silently returning null or re-throwing.

#### Scenario: Failure propagates to caller

- GIVEN a use case calls a repository method that fails with `IOException`
- WHEN the use case catches the `IOException`
- THEN it MUST return `Result.failure(IOException(...))`
- AND the caller MUST be able to inspect `result.isFailure` to handle the error

#### Scenario: Success propagates correctly

- GIVEN a repository method succeeds
- WHEN the result is wrapped in `Result.success(data)`
- THEN the caller MUST receive the data via `result.getOrNull()`

### Requirement: Scope — data and domain layers only

Refactoring MUST target catch blocks in `data/` and `domain/` layers. ViewModel and UI layer catch blocks are explicitly out of scope.

#### Scenario: ViewModel catch blocks unchanged

- GIVEN a ViewModel contains `catch (e: Exception)`
- WHEN the refactoring is applied
- THEN those catch blocks MUST NOT be modified

#### Scenario: SyncFinanzasUseCase fully covered

- GIVEN `SyncFinanzasUseCase.kt` contains 10 generic catch blocks
- WHEN the refactoring is complete
- THEN all 10 blocks MUST have specific exception types and logging

## Non-Goals

- Refactoring catch blocks in ViewModels or UI layer
- Introducing a global exception handler
- Changing the exception propagation to ViewModels (that is deferred to a separate cleanup)
